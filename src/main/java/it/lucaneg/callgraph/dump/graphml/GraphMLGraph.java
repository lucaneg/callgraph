package it.lucaneg.callgraph.dump.graphml;

import java.io.IOException;
import java.io.Writer;
import java.util.IdentityHashMap;
import java.util.Map;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkGraphML;

import it.lucaneg.callgraph.MethodMetadata;

public class GraphMLGraph implements it.lucaneg.callgraph.dump.Graph {
	
	private static final String LABEL = "label";
	private static final String ENTRY = "entry";
	private static final String EXIT = "exit";

	private final Graph graph;

	private final Map<MethodMetadata, Long> codes = new IdentityHashMap<>();

	private long nextCode = 0;

	public GraphMLGraph() {
		this.graph = new MultiGraph("callgraph");
	}

	@Override
	public void addNode(MethodMetadata method, boolean entry, boolean exit, boolean chop) {
		Node n = graph.addNode(nodeName(codes.computeIfAbsent(method, nn -> nextCode++)));

		n.setAttribute(ENTRY, entry);
		n.setAttribute(EXIT, exit);

		String label = (method.isUnresolved() ? "[open] " : "")
				+ method.getReadableSignatureWithNoClassName(chop)
				+ "\nclass: " + method.getClassName(false);
		n.setAttribute(LABEL, label);
	}

	private String nodeName(long id) {
		return "node" + id;
	}

	@Override
	public void addEdge(MethodMetadata method, MethodMetadata follow) {
		long id = codes.computeIfAbsent(method, n -> nextCode++);
		long id1 = codes.computeIfAbsent(follow, n -> nextCode++);

		graph.addEdge("edge-" + id + "-" + id1, nodeName(id), nodeName(id1), true);
	}

	@Override
	public void dump(Writer file) throws IOException {
		FileSinkGraphML sink = new FileSinkGraphML();
		sink.writeAll(graph, file);
	}
}
