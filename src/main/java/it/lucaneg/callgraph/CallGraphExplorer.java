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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

public class CallGraphExplorer {

	private Map<String, ClassGen> classes = new HashMap<>();

	private Map<String, MethodMetadata> methods = new HashMap<>();

	public Collection<MethodMetadata> getEntryPointMethods(boolean excludeUnresolved) {
		return methods.values()
				.stream()
				.filter(m -> isEntry(excludeUnresolved, m))
				.collect(Collectors.toList());
	}

	private boolean isEntry(boolean excludeUnresolved, MethodMetadata m) {
		return m.getCallers().isEmpty() && (!excludeUnresolved && !m.isUnresolved());
	}

	public void computeCallingChains() {
		int dynamics = 0;

		for (ClassGen clazz : classes.values()) {
			AtomicInteger skippedInfos = new AtomicInteger(0);
			List<LambdaInfo> infos = computeLambdas(clazz, skippedInfos);
			dynamics += skippedInfos.get();

			for (Method m : clazz.getMethods()) {
				ConstantPoolGen cp = new ConstantPoolGen(m.getConstantPool());
				MethodGen method = new MethodGen(m, clazz.getClassName(), cp);
				MethodMetadata metadata = methods.computeIfAbsent(signature(method), s -> new MethodMetadata(s));

				if (method.isAbstract() || method.isNative() || method.isInterface())
					continue;

				for (InstructionHandle handle : method.getInstructionList())
					if (handle.getInstruction() instanceof InvokeInstruction) {
						InvokeInstruction invoke = (InvokeInstruction) handle.getInstruction();

						boolean unresolved = false;
						String target;
						if (invoke instanceof INVOKEDYNAMIC) {
							INVOKEDYNAMIC call = (INVOKEDYNAMIC) invoke;
							ConstantInvokeDynamic id = (ConstantInvokeDynamic) cp.getConstant(call.getIndex());

							int index = id.getBootstrapMethodAttrIndex();
							target = infos.get(index).getSignature();
						} else {
							String name = invoke.getName(cp);
							Type[] pars = invoke.getArgumentTypes(cp);
							Type returnType = invoke.getReturnType(cp);

							ReferenceType recType = invoke.getReferenceType(cp);
							if (!(recType instanceof ObjectType))
								recType = ObjectType.OBJECT;

							// TODO dynamic target resolution

							if (!classes.containsKey(Utility.signatureToString(recType.getSignature(), false)))
								unresolved = true;

							target = signature(recType, name, pars, returnType);
						}

						boolean unres = unresolved;
						MethodMetadata targetMetadata = methods.computeIfAbsent(target, s -> new MethodMetadata(s, unres));
						targetMetadata.getCallers().add(metadata);
						if (unresolved)
							metadata.getUnresolvedCallees().add(targetMetadata);
						else
							metadata.getCallees().add(targetMetadata);
					}
			}
		}

		if (dynamics != 0)
			System.out.println("Skipped " + dynamics + " invokedynamic statements");
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

							infos.add(new LambdaInfo(nameAndType.getName(cp), Utility.signatureToString("L" + cpclass + ";", false), nameAndType.getSignature(cp)));
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

		public String getSignature() {
			return targetClass + "." + targetName + targetSignature;
		}

		@Override
		public String toString() {
			return targetName + ", " + targetClass + ", " + targetSignature;
		}
	}

	private static String signature(ReferenceType receiver, String name, Type[] pars, Type ret) {
		return Utility.signatureToString(receiver.getSignature(), false) + "." + name
				+ Type.getMethodSignature(ret, pars);
	}

	private static String signature(MethodGen method) {
		return method.getClassName() + "." + method.getName() + method.getSignature();
	}

	public void addJarEntries(String path) throws IOException {
		JarFile jf = new JarFile(path);

		jf.stream().forEachOrdered(e -> {
			String fileName = e.getName();

			if (!fileName.endsWith(".class"))
				return;

			try (InputStream is = new ByteArrayInputStream(getBytes(jf.getInputStream(e)))) {
				JavaClass clazz = new ClassParser(is, fileName).parse();
				classes.put(clazz.getClassName(), new ClassGen(clazz));
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
