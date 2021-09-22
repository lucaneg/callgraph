package it.lucaneg.callgraph.dump.dot;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDOT;

import it.lucaneg.callgraph.MethodMetadata;

public class DotGraph implements it.lucaneg.callgraph.dump.Graph {
	private static final String COLOR_BLACK = "black";
	private static final String COLOR_GRAY = "gray";
	private static final String COLOR_BLUE = "blue";
	private static final String COLOR = "color";
	private static final String SHAPE = "shape";
	private static final String LABEL = "label";
	private static final String NODE_SHAPE = "rect";
	private static final String EXIT_NODE_EXTRA_ATTR = "peripheries";
	private static final String EXIT_NODE_EXTRA_VALUE = "2";

	private final Graph graph;

	private final Map<MethodMetadata, Long> codes = new IdentityHashMap<>();

	private long nextCode = 0;

	public DotGraph() {
		this.graph = new MultiGraph("callgraph");
	}

	@Override
	public void addNode(MethodMetadata node, boolean entry, boolean exit, boolean chop) {
		Node n = graph.addNode(nodeName(codes.computeIfAbsent(node, nn -> nextCode++)));

		n.setAttribute(SHAPE, NODE_SHAPE);
		if (entry || exit)
			n.setAttribute(COLOR, COLOR_BLACK);
		else
			n.setAttribute(COLOR, COLOR_GRAY);

		if (exit)
			n.setAttribute(EXIT_NODE_EXTRA_ATTR, EXIT_NODE_EXTRA_VALUE);

		String label = (node.isUnresolved() ? "[open] " : "")
				+ node.getReadableSignatureWithNoClassName(chop)
				+ "<BR/>class: " + node.getClassName(false);
		n.setAttribute(LABEL, "<" + label + ">");
	}

	private String nodeName(long id) {
		return "node" + id;
	}

	@Override
	public void addEdge(MethodMetadata source, MethodMetadata dest) {
		long id = codes.computeIfAbsent(source, n -> nextCode++);
		long id1 = codes.computeIfAbsent(dest, n -> nextCode++);

		Edge e = graph.addEdge("edge-" + id + "-" + id1, nodeName(id), nodeName(id1), true);

		e.setAttribute(COLOR, COLOR_BLUE);
	}

	@Override
	public void dump(Writer writer) throws IOException {
		FileSinkDOT sink = new CustomDotSink();
		sink.setDirected(true);
		sink.writeAll(graph, writer);
	}

	private static class CustomDotSink extends FileSinkDOT {

		@Override
		protected String outputAttribute(String key, Object value, boolean first) {
			boolean quote = true;

			if (value instanceof Number || key.equals(LABEL))
				// labels that we output are always in html format
				// so no need to quote them
				quote = false;

			return String.format("%s%s=%s%s%s", first ? "" : ",", key, quote ? "\"" : "", value, quote ? "\"" : "");
		}

		@Override
		protected String outputAttributes(Element e) {
			if (e.getAttributeCount() == 0)
				return "";

			Map<String, String> attrs = new HashMap<>();
			e.attributeKeys().forEach(key -> attrs.put(key, outputAttribute(key, e.getAttribute(key), true)));

			StringBuilder buffer = new StringBuilder("[");
			for (Entry<String, String> entry : attrs.entrySet())
				if (!entry.getKey().equals(LABEL))
					buffer.append(entry.getValue()).append(",");

			if (attrs.containsKey(LABEL))
				buffer.append(attrs.get(LABEL));

			String result = buffer.toString();
			if (result.endsWith(","))
				result = result.substring(0, result.length() - 1);

			return result + "]";
		}
	}
}
