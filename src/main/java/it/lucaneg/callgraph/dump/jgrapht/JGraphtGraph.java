package it.lucaneg.callgraph.dump.jgrapht;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.jgrapht.nio.graphml.GraphMLExporter.AttributeCategory;

import it.lucaneg.callgraph.MethodMetadata;
import it.lucaneg.callgraph.dump.Graph;

public class JGraphtGraph implements Graph {

	private final DefaultDirectedGraph<MethodMetadata, DefaultEdge> graph;
	private final Map<MethodMetadata, Map<String, Attribute>> nodeAttributes;

	public JGraphtGraph() {
		this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		this.nodeAttributes = new HashMap<>();
	}

	@Override
	public void addNode(MethodMetadata method, boolean entry, boolean exit, boolean chop) {
		graph.addVertex(method);
		String label = (method.isUnresolved() ? "[open] " : "") 
				+ method.getReadableSignatureWithNoClassName(chop) 
				+ "\nclass: " + method.getClassName(chop);
		nodeAttributes.put(method, Map.of(
				"entry", new DefaultAttribute<>(entry, AttributeType.BOOLEAN),
				"exit", new DefaultAttribute<>(exit, AttributeType.BOOLEAN),
				"label", new DefaultAttribute<>(label, AttributeType.STRING)
				));
	}

	@Override
	public void addEdge(MethodMetadata method, MethodMetadata follow) {
		graph.addEdge(method, follow);
	}

	@Override
	public void dump(Writer file) throws IOException {
		GraphMLExporter<MethodMetadata, DefaultEdge> exporter = new GraphMLExporter<>();
		exporter.setVertexAttributeProvider(meta -> nodeAttributes.get(meta));
		exporter.registerAttribute("entry", AttributeCategory.NODE, AttributeType.BOOLEAN);
		exporter.registerAttribute("exit", AttributeCategory.NODE, AttributeType.BOOLEAN);
		exporter.registerAttribute("label", AttributeCategory.NODE, AttributeType.STRING);
		exporter.exportGraph(graph, file);
	}
}
