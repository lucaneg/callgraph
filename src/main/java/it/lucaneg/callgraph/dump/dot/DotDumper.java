package it.lucaneg.callgraph.dump.dot;

import it.lucaneg.callgraph.CallGraphExplorer;
import it.lucaneg.callgraph.dump.BaseDumper;
import it.lucaneg.callgraph.dump.Graph;

public class DotDumper extends BaseDumper {

	public DotDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
		super(explorer, excludeUnresolved, chop);
	}

	@Override
	protected Graph getGraph() {
		return new DotGraph();
	}
	
	@Override
	protected String getGraphExtension() {
		return "dot";
	}
}
