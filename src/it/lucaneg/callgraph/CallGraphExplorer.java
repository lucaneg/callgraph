package it.lucaneg.callgraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
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

	public Collection<MethodMetadata> getEntryPointMethods() {
		return methods.values()
						.stream()
						.filter(m -> m.getCallers().isEmpty() && !m.isUnresolved())
						.collect(Collectors.toList());
	}
	
	public void computeCallingChains() {
		int dynamics = 0;
		
		for (ClassGen clazz : classes.values())
			for (Method m : clazz.getMethods()) {
				ConstantPoolGen cp = new ConstantPoolGen(m.getConstantPool());
				MethodGen method = new MethodGen(m, clazz.getClassName(), cp);
				MethodMetadata metadata = methods.computeIfAbsent(signature(method), s -> new MethodMetadata(s));
				
				if (method.isAbstract() || method.isNative() || method.isInterface())
					continue;
				
				for (InstructionHandle handle : method.getInstructionList())
					if (handle.getInstruction() instanceof InvokeInstruction) {
						InvokeInstruction invoke = (InvokeInstruction) handle.getInstruction();
						
						String name = invoke.getName(cp);
						Type[] pars = invoke.getArgumentTypes(cp);

						// we determine the return type of the method
						Type returnType = invoke.getReturnType(cp);

						if (invoke instanceof INVOKEDYNAMIC) {
							// TODO
							dynamics++;
						} else {
							// we determine the class of the receiver
							ReferenceType recType = invoke.getReferenceType(cp);
							if (!(recType instanceof ObjectType))
								recType = ObjectType.OBJECT;
							
							// TODO dynamic target resolution
							
							boolean unresolved = false;
							if (!classes.containsKey(Utility.signatureToString(recType.getSignature(), false)))
								unresolved = true;

							String target = signature(recType, name, pars, returnType);
							if (unresolved) {
								MethodMetadata targetMetadata = methods.computeIfAbsent(target, s -> new MethodMetadata(s, true));
								metadata.getUnresolvedCallees().add(targetMetadata);
								targetMetadata.getCallers().add(metadata);
							} else {
								MethodMetadata targetMetadata = methods.computeIfAbsent(target, s -> new MethodMetadata(s));
								metadata.getCallees().add(targetMetadata);
								targetMetadata.getCallers().add(metadata);
							}
						}
					}
			}
		
		System.out.println("Skipped " + dynamics + " invokedynamic statements");
	}
	
	private static String signature(ReferenceType receiver, String name, Type[] pars, Type ret) {
		return Utility.signatureToString(receiver.getSignature(), false) + "." + name + Type.getMethodSignature(ret, pars);
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
