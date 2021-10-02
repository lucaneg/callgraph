package it.lucaneg.callgraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.commons.lang3.StringUtils;

import it.lucaneg.callgraph.model.ClassMetadata;
import it.lucaneg.callgraph.model.MethodMetadata;
import it.lucaneg.callgraph.types.ClassType;
import it.lucaneg.callgraph.types.Type;

public class CallGraphExplorer {

	private final Map<String, ClassGen> classGens = new HashMap<>();

	private final Map<String, ClassMetadata> classes = new HashMap<>();

	private Map<String, MethodMetadata> methods = new HashMap<>();

	public Collection<MethodMetadata> getMatching(Predicate<MethodMetadata> test) {
		return methods.values()
				.stream()
				.filter(test)
				.collect(Collectors.toList());
	}

	public void computeCallingChains() {
		for (ClassGen classGen : classGens.values())
			parseClassSignature(classGen);

		int dynamics = 0;
		for (ClassGen classGen : classGens.values())
			dynamics += process(classGen);
		for (ClassMetadata clazz : classes.values())
			clazz.closeHierarchy();
		
		for (MethodMetadata method : methods.values())
			closure(method);

		if (dynamics != 0)
			System.out.println("Skipped " + dynamics + " invokedynamic statements");
	}

	private void parseClassSignature(ClassGen classGen) {
		String className = classGen.getClassName();
		if (classes.containsKey(className))
			return;

		String superclass = classGen.getSuperclassName();
		if (!classes.containsKey(superclass) && classGens.containsKey(superclass))
			parseClassSignature(classGens.get(superclass));
		for (String iface : classGen.getInterfaceNames())
			if (!classes.containsKey(iface) && classGens.containsKey(iface))
				parseClassSignature(classGens.get(iface));

		ClassMetadata clazz = new ClassMetadata(className);
		clazz = put(classes, className, clazz);
		if (classGens.containsKey(superclass))
			clazz.setSuperclass(classes.get(superclass));
		for (String iface : classGen.getInterfaceNames())
			if (classGens.containsKey(iface))
				clazz.addInterface(classes.get(iface));

		// store it as a type
		ClassType.lookup(className, clazz);
	}

	private int process(ClassGen classGen) {
		String className = classGen.getClassName();
		ClassMetadata clazz = classes.get(className);
		AtomicInteger skippedInfos = new AtomicInteger(0);
		List<LambdaInfo> infos = computeLambdas(classGen, skippedInfos);

		for (Method m : classGen.getMethods()) {
			ConstantPoolGen cp = new ConstantPoolGen(m.getConstantPool());
			MethodGen method = new MethodGen(m, className, cp);

			MethodMetadata metadata = buildMetadataFor(className, m.getName(),
					m.getReturnType(), m.getArgumentTypes(),
					canBeOverridden(null, method), false);
			metadata = put(methods, metadata.getSignature(), metadata);
			clazz.addMethod(metadata);

			if (method.isAbstract() || method.isNative() || method.isInterface())
				// no code, we can skip the parsing
				continue;

			for (InstructionHandle handle : method.getInstructionList())
				if (handle.getInstruction() instanceof InvokeInstruction) {
					InvokeInstruction invoke = (InvokeInstruction) handle.getInstruction();

					AtomicBoolean unresolved = new AtomicBoolean(false);
					MethodMetadata targetMetadata;
					if (invoke instanceof INVOKEDYNAMIC)
						targetMetadata = processInvokeDynamic(infos, cp, (INVOKEDYNAMIC) invoke);
					else
						targetMetadata = processGenericInvoke(invoke, cp, method, unresolved);

					targetMetadata = put(methods, targetMetadata.getSignature(), targetMetadata);
					targetMetadata.getCallers().add(metadata);
					if (unresolved.get())
						metadata.getUnresolvedCallees().add(targetMetadata);
					else
						metadata.getCallees().add(targetMetadata);
				}
		}

		return skippedInfos.get();
	}

	private MethodMetadata processGenericInvoke(InvokeInstruction invoke, ConstantPoolGen cp, MethodGen method,
			AtomicBoolean unresolved) {
		String name = invoke.getName(cp);
		
		ReferenceType recType = invoke.getReferenceType(cp);
		if (!(recType instanceof ObjectType))
			recType = ObjectType.OBJECT;

		String receiver = Utility.signatureToString(recType.getSignature(), false);
		String sig = invoke.getSignature(cp);
		String retStr = Utility.methodSignatureReturnType(sig, false);
		String[] paramsStr = Utility.methodSignatureArgumentTypes(sig, false);

		MethodGen target = null;
		if (!classGens.containsKey(receiver)) {
			put(classes, receiver, new ClassMetadata(receiver));
			unresolved.set(true);
			target = null;
		} else {
			for (Method mtd : classGens.get(receiver).getMethods())
				if (mtd.getName().equals(name) && mtd.getSignature().equals(sig)) {
					target = new MethodGen(mtd, receiver, cp);
					break;
				}
		}
		
		return buildMetadataFor(receiver, name, retStr, paramsStr, canBeOverridden(invoke, target), unresolved.get());
	}

	private MethodMetadata processInvokeDynamic(List<LambdaInfo> infos, ConstantPoolGen cp, INVOKEDYNAMIC call) {
		ConstantInvokeDynamic id = (ConstantInvokeDynamic) cp.getConstant(call.getIndex());
		int index = id.getBootstrapMethodAttrIndex();
		LambdaInfo info = infos.get(index);
		String retStr = Utility.methodSignatureReturnType(info.getTargetSignature(), false);
		String[] paramsStr = Utility.methodSignatureArgumentTypes(info.getTargetSignature(), false);
		return buildMetadataFor(info.getTargetClass(), info.getTargetName(), retStr, paramsStr, false, false);
	}

	private MethodMetadata buildMetadataFor(String clazz, String name, org.apache.bcel.generic.Type returnType,
			org.apache.bcel.generic.Type[] paramsTypes, boolean canBeOverriden, boolean unresolved) {
		String ret = Utility.signatureToString(returnType.getSignature(), false);
		String[] params = new String[paramsTypes.length];
		for (int i = 0; i < params.length; i++)
			params[i] = Utility.signatureToString(paramsTypes[i].getSignature(), false);
		return buildMetadataFor(clazz, name, ret, params, canBeOverriden, unresolved);
	}

	private MethodMetadata buildMetadataFor(String clazz, String name, String returnType, String[] paramsTypes,
			boolean canBeOverriden, boolean unresolved) {
		ClassMetadata targetClass = classes.get(clazz);
		Type ret = Type.convert(org.apache.bcel.generic.Type.getType(Utility.getSignature(returnType)), classes);
		Type[] params = new Type[paramsTypes.length];
		for (int i = 0; i < params.length; i++)
			params[i] = Type.convert(org.apache.bcel.generic.Type.getType(Utility.getSignature(paramsTypes[i])), classes);
		return new MethodMetadata(targetClass,
				signature(clazz, name, paramsTypes, returnType),
				name, ret, params, canBeOverriden, unresolved);
	}

	private boolean canBeOverridden(InvokeInstruction invoke, MethodGen method) {
		return (invoke == null || invoke instanceof INVOKEVIRTUAL || invoke instanceof INVOKEINTERFACE)
				&& (method == null || !(method.isStatic()
						|| method.isFinal()
						|| method.isPrivate()
						|| method.getName().equals(Const.CONSTRUCTOR_NAME)
						|| method.getName().equals(Const.STATIC_INITIALIZER_NAME)));
	}

	private static <T> T put(Map<String, T> map, String key, T target) {
		T ret = map.putIfAbsent(key, target);
		if (ret == null)
			ret = target;
		return ret;
	}

	private void closure(MethodMetadata method) {
		if (method.getCallers().isEmpty() || !method.canBeOverridden())
			return;

		for (MethodMetadata overriding : method.getOverriders())
			for (MethodMetadata caller : method.getCallers())
				overriding.getCallers().add(caller);
	}

	private final static String LAMBDA_FACTORY_CLASS = "java/lang/invoke/LambdaMetafactory";
	private final static String CONCAT_CLASS = "java/lang/invoke/StringConcatFactory";
	private final static String CONCAT_METHOD = "makeConcatWithConstants";
	private final static String CONCAT_SIGNATURE = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
			+ "Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)"
			+ "Ljava/lang/invoke/CallSite;";

	private List<LambdaInfo> computeLambdas(ClassGen clazz, AtomicInteger skipped) {
		List<LambdaInfo> infos = new ArrayList<>();
		for (Attribute attribute : clazz.getAttributes())
			if (attribute instanceof BootstrapMethods) {
				ConstantPoolGen cpg = clazz.getConstantPool();
				ConstantPool cp = cpg.getConstantPool();

				for (BootstrapMethod bootstrap : ((BootstrapMethods) attribute).getBootstrapMethods()) {
					int[] args = bootstrap.getBootstrapArguments();
					ConstantMethodHandle methodRef = (ConstantMethodHandle) cpg
							.getConstant(bootstrap.getBootstrapMethodRef());
					int index = methodRef.getReferenceIndex();
					ConstantCP target = (ConstantCP) cpg.getConstant(index);
					String definingClass = ((ConstantClass) cpg.getConstant(target.getClassIndex())).getBytes(cp);

					if (args.length >= 3 && definingClass.equals(LAMBDA_FACTORY_CLASS)) {
						Constant cmh = cpg.getConstant(args[1]);
						if (cmh instanceof ConstantMethodHandle) {
							int ri = ((ConstantMethodHandle) cmh).getReferenceIndex();

							ConstantCP cpentry = (ConstantCP) cpg.getConstant(ri);
							String cpclass = ((ConstantClass) cpg.getConstant(cpentry.getClassIndex())).getBytes(cp);
							ConstantNameAndType nameAndType = (ConstantNameAndType) cpg
									.getConstant(cpentry.getNameAndTypeIndex());

							infos.add(new LambdaInfo(nameAndType.getName(cp),
									Utility.signatureToString("L" + cpclass + ";", false),
									nameAndType.getSignature(cp)));
						} else {
							skipped.incrementAndGet();
							continue;
						}
					} else if (args.length >= 1 && definingClass.equals(CONCAT_CLASS)) {
						ConstantNameAndType nameAndType = (ConstantNameAndType) cpg
								.getConstant(target.getNameAndTypeIndex());
						String name = nameAndType.getName(cp);
						String signature = nameAndType.getSignature(cp);

						if (CONCAT_METHOD.equals(name) && CONCAT_SIGNATURE.equals(signature))
							infos.add(new LambdaInfo(CONCAT_METHOD, CONCAT_CLASS, CONCAT_SIGNATURE));
						else {
							skipped.incrementAndGet();
							continue;
						}
					}
				}
			}

		return infos;
	}

	public static class LambdaInfo {

		private final String targetName;

		private final String targetClass;

		private final String targetSignature;

		private LambdaInfo(String targetName, String targetClass, String targetSignature) {
			this.targetName = targetName;
			this.targetClass = targetClass;
			this.targetSignature = targetSignature;
		}

		public String getTargetName() {
			return targetName;
		}

		public String getTargetClass() {
			return targetClass;
		}

		public String getTargetSignature() {
			return targetSignature;
		}

		public String getSignature() {
			return targetClass + "." + targetName + targetSignature;
		}

		@Override
		public String toString() {
			return targetName + ", " + targetClass + ", " + targetSignature;
		}
	}

	private static String signature(String clazz, String name, String[] pars, String ret) {
		return clazz + '.' + name + '(' + StringUtils.join(pars) + ')' + ret;
	}

	public void addJarEntries(String path) throws IOException {
		JarFile jf = new JarFile(path);

		jf.stream().forEachOrdered(e -> {
			String fileName = e.getName();

			if (!fileName.endsWith(".class"))
				return;

			try (InputStream is = new ByteArrayInputStream(getBytes(jf.getInputStream(e)))) {
				JavaClass clazz = new ClassParser(is, fileName).parse();
				classGens.put(clazz.getClassName(), new ClassGen(clazz));
				Repository.addClass(clazz);
			} catch (IOException | ClassFormatException ex) {
				System.err.println("Cannot parse " + fileName + " in " + path);
				System.err.println(ex);
			}
		});

		jf.close();
	}

	private static byte[] getBytes(InputStream is) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = is.read(buffer)) != -1)
				os.write(buffer, 0, len);

			os.flush();
			return os.toByteArray();
		}
	}
}
