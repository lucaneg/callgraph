package it.lucaneg.callgraph.dump;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import it.lucaneg.callgraph.CallGraphExplorer;
import it.lucaneg.callgraph.model.MethodMetadata;

public abstract class BaseDumper {

	private final Set<MethodMetadata> isolated = new TreeSet<>();

	private final Stack<MethodMetadata> workingSet = new Stack<>();

	private final boolean excludeUnresolved;

	private final boolean chop;

	protected BaseDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
		this.excludeUnresolved = excludeUnresolved;
		this.chop = chop;
		Collection<MethodMetadata> start = explorer.getMatching(m -> isEntry(m));
		if (!start.isEmpty())
			for (MethodMetadata entry : start)
				if (isExit(entry))
					isolated.add(entry);
				else
					workingSet.push(entry);
	}

	protected abstract Graph getGraph();

	protected abstract String getGraphExtension();

	public final void dump(String path) throws IOException {
		Graph g = getGraph();
		Set<MethodMetadata> added = new HashSet<>();
		Set<MethodMetadata> done = new HashSet<>();

		if (!isolated.isEmpty()) {
			Set<String> unreachable = new HashSet<>();
			Set<String> classInit = new HashSet<>();
			Set<String> standard = new HashSet<>();

			for (MethodMetadata method : isolated) {
				String sig = method.getReadableSignature(chop);
				if (method.isStandard())
					standard.add(sig);
				else if (method.isClassConstructor())
					classInit.add(sig);
				else
					unreachable.add(sig);
			}

			System.out.println("The following will not be dumped since they have no explicit callers nor callees:");
			if (!unreachable.isEmpty()) {
				System.out.println("Methods of the application:");
				System.out.println("- " + StringUtils.join(unreachable, "\n- "));
			}

			if (!standard.isEmpty()) {
				System.out.println("Standard methods (equals, hashCode, toString, compareTo):");
				System.out.println("- " + StringUtils.join(standard, "\n- "));
			}

			if (!classInit.isEmpty()) {
				System.out.println("Class initializers:");
				System.out.println("- " + StringUtils.join(classInit, "\n- "));
			}
		}

		while (!workingSet.isEmpty()) {
			MethodMetadata method = workingSet.pop();

			if (done.add(method)) {
				if (added.add(method))
					g.addNode(method, isEntry(method), isExit(method), chop);

				for (MethodMetadata follow : method.getAllCallees())
					if (!excludeUnresolved || !follow.isUnresolved()) {
						if (!done.contains(follow)) {
							workingSet.push(follow);

							if (added.add(follow))
								g.addNode(follow, false, isExit(follow), chop);
						}

						g.addEdge(method, follow);
					}
			}
		}
		String fileName = path + "." + getGraphExtension();
		try (Writer file = new FileWriter(fileName)) {
			g.dump(file);
			System.out.println("Callgraph dumped to " + fileName);
		}
	}

	private boolean isExit(MethodMetadata method) {
		return method.isUnresolved()
				|| (method.getCallees().isEmpty() && (excludeUnresolved || method.getUnresolvedCallees().isEmpty()));
	}

	private boolean isEntry(MethodMetadata method) {
		return method.getCallers().isEmpty() && (!excludeUnresolved || !method.isUnresolved());
	}
}
