package it.lucaneg.callgraph.dump.graphml;

import it.lucaneg.callgraph.CallGraphExplorer;
import it.lucaneg.callgraph.dump.BaseDumper;
import it.lucaneg.callgraph.dump.Graph;

public class GraphMLDumper extends BaseDumper {

	public GraphMLDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
		super(explorer, excludeUnresolved, chop);
	}

	@Override
	protected Graph getGraph() {
		return new GraphMLGraph();
	}

	@Override
	protected String getGraphExtension() {
		return "graphml";
	}
}
