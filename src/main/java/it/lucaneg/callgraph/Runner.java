package it.lucaneg.callgraph;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.lucaneg.callgraph.dump.dot.DotDumper;

public class Runner {

	private static final Option INPUT;
	private static final Option OUTPUT;
	private static final Option UNRESOLVED;
	private static final Option CHOP;

	static {
		INPUT = Option.builder("i").longOpt("input").hasArg()
				.desc("add a jar file to be analyzed").build();
		OUTPUT = Option.builder("o").longOpt("output").hasArg().desc("name of the output dot graph").required().build();
		UNRESOLVED = Option.builder("u").longOpt("exclude-unresolved")
				.desc("do not dump unresolved (i.e. library) methods").build();
		CHOP = Option.builder("c").longOpt("chop-types")
				.desc("use simple class names for return types and parameters instead of fully qualified ones").build();
	}

	private static CommandLine cmdLine;

	public static void main(String[] args) {
		Options options = buildOptions();
		try {
			cmdLine = new DefaultParser().parse(options, args);

			if (!cmdLine.hasOption(INPUT.getOpt())) {
				System.out.println("No input jar provided with -" + INPUT.getOpt() + ", skipping execution");
				return;
			}

			CallGraphExplorer explorer = new CallGraphExplorer();
			for (String in : cmdLine.getOptionValues(INPUT.getOpt()))
				try {
					explorer.addJarEntries(in);
				} catch (IOException e) {
					System.err.println("Unable to open " + in);
					System.err.println(e);
				}

			explorer.computeCallingChains();

			new DotDumper(explorer, cmdLine.hasOption(UNRESOLVED.getOpt()), cmdLine.hasOption(CHOP.getOpt()))
					.dump(cmdLine.getOptionValue(OUTPUT.getOpt()));
		} catch (ParseException e) {
			printUsage(options);
			System.err.println(e.getMessage());
		} catch (Exception e) {
			System.err.println("Exception during execution of the callgraph extractor:\n" + e.getMessage() + "\n");
			e.printStackTrace(System.err);
		}
	}

	private static void printUsage(Options options) {
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp(Runner.class.getSimpleName(), options);
	}

	private static Options buildOptions() {
		Options result = new Options();
		result.addOption(INPUT);
		result.addOption(OUTPUT);
		result.addOption(UNRESOLVED);
		result.addOption(CHOP);
		return result;
	}
}
