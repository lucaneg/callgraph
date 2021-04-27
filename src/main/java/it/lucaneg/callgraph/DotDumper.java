package it.lucaneg.callgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DotDumper {

	private final Set<MethodMetadata> done = new HashSet<>();

	private final Stack<MethodMetadata> workingSet = new Stack<>();

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
		DotGraph g = new DotGraph();
		Set<MethodMetadata> added = new HashSet<>();

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
		}
	}

	private boolean isExit(MethodMetadata method) {
		return method.isUnresolved() || method.getAllCallees().isEmpty();
	}

	private boolean isEntry(MethodMetadata method) {
		return method.getCallers().isEmpty();
	}
}
