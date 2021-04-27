package it.lucaneg.callgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class DotDumper {

	private final Set<MethodMetadata> isolated = new TreeSet<>();

	private final Stack<MethodMetadata> workingSet = new Stack<>();

	private final boolean excludeUnresolved;

	private final boolean chop;

	public DotDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
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

	public void dump(String path) throws IOException {
		DotGraph g = new DotGraph();
		Set<MethodMetadata> added = new HashSet<>();
		Set<MethodMetadata> done = new HashSet<>();

		if (!isolated.isEmpty()) {
			System.out.println("The following methods will not be dumped to the graph "
					+ "since they are isolated (no explicit callers nor callees):");
			for (MethodMetadata method : isolated)
				System.out.println("- " + method.getReadableSignature(chop));
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
		try (Writer file = new FileWriter(path)) {
			g.dumpDot(file);
			System.out.println("Callgraph dumped to " + path);
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
