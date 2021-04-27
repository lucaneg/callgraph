package it.lucaneg.callgraph;

import java.io.IOException;

public class Runner {
	public static void main(String[] args) throws IOException {
		CallGraphExplorer explorer = new CallGraphExplorer();
		try {
			explorer.addJarEntries(args[0]);
		} catch (IOException e) {
			System.err.println("Unable to open " + args[0]);
			System.err.println(e);
		}

		explorer.computeCallingChains();

		new DotDumper(explorer).dump(args[1], true);
	}
}
