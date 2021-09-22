package it.lucaneg.callgraph.dump.jgrapht;

import it.lucaneg.callgraph.CallGraphExplorer;
import it.lucaneg.callgraph.dump.BaseDumper;
import it.lucaneg.callgraph.dump.Graph;

public class JGraphtDumper extends BaseDumper {

	public JGraphtDumper(CallGraphExplorer explorer, boolean excludeUnresolved, boolean chop) {
		super(explorer, excludeUnresolved, chop);
	}

	@Override
	protected Graph getGraph() {
		return new JGraphtGraph();
	}

	@Override
	protected String getGraphExtension() {
		return "graphml";
	}
}
