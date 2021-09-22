package it.lucaneg.callgraph.dump;

import java.io.IOException;
import java.io.Writer;

import it.lucaneg.callgraph.MethodMetadata;

public interface Graph {

	void addNode(MethodMetadata method, boolean entry, boolean exit, boolean chop);

	void addEdge(MethodMetadata method, MethodMetadata follow);

	void dump(Writer file) throws IOException;
}
