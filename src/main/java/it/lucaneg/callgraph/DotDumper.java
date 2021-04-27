package it.lucaneg.callgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class DotDumper {

	private Writer file;

	private final Set<MethodMetadata> done = new HashSet<>();

	private final Stack<MethodMetadata> workingSet = new Stack<>();

	private final Map<String, Integer> ids = new HashMap<>();

	private final boolean excludeUnresolved;
	
	private final boolean chop;

	public DotDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
		this.excludeUnresolved = excludeUnresolved;
		this.chop = chop;
		Collection<MethodMetadata> start = explorer.getEntryPointMethods(excludeUnresolved); 
		if (!start.isEmpty())
			workingSet.addAll(start);
	}

	public void dump(String path) throws IOException {
		try (Writer file = new FileWriter(path)) {
			this.file = file;

			beginDotFile();

			while (!workingSet.isEmpty()) {
				MethodMetadata method = workingSet.pop();

				if (done.add(method)) {
					dumpMethod(method);
					dumpArcsLeaving(method);
				}
			}

			endDotFile();
		}
	}

	private void beginDotFile() throws IOException {
		file.write("digraph \"callgraph\" {\n");
		file.write("rankdir=TD;\n");
	}

	private void endDotFile() throws IOException {
		file.write("}");
		file.flush();
	}

	private void dumpMethod(MethodMetadata method) throws IOException {
		file.write(nameInDotFile(method) + " [" + getLabelFor(method) + "];\n");
	}

	private void dumpArcsLeaving(MethodMetadata method) throws IOException {
		for (MethodMetadata follow : method.getAllCallees())
			if (!excludeUnresolved || !follow.isUnresolved())
				dumpArc(method, follow, "color = blue");
	}

	private void dumpArc(MethodMetadata from, MethodMetadata to, String label) throws IOException {
		if (!done.contains(to))
			workingSet.push(to);

		file.write(nameInDotFile(from) + " -> " + nameInDotFile(to) + "[" + label + "]\n");
	}

	private String getLabelFor(MethodMetadata method) {
		return "shape = box, label = <" + (method.isUnresolved() ? "[open] " : "")
				+ method.getReadableSignatureWithNoClassName(chop) + "<BR/>class: " + method.getClassName(false) + ">";
	}

	private final String nameInDotFile(MethodMetadata method) {
		Integer id = ids.get(method.getSignature());
		if (id == null)
			ids.put(method.getSignature(), id = ids.size());

		return "method_" + id;
	}
}
