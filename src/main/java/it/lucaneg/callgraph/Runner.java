package it.lucaneg.callgraph;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.lucaneg.callgraph.dump.dot.DotDumper;
import it.lucaneg.callgraph.dump.jgrapht.JGraphtDumper;

public class Runner {

	private static final Option INPUT;
	private static final Option OUTPUT;
	private static final Option UNRESOLVED;
	private static final Option CHOP;
	private static final Option FORMAT;

	enum Format {
		GRAPHML, DOT
	}
	
	static {
		INPUT = Option.builder("i")
						.longOpt("input")
						.hasArg()
						.argName("path")
						.desc("add a jar file to be analyzed")
						.required()
						.build();
		OUTPUT = Option.builder("o")
						.longOpt("output")
						.hasArg()
						.argName("path")
						.desc("name of the output dot graph")
						.required()
						.build();
		UNRESOLVED = Option.builder("u")
						.longOpt("exclude-unresolved")
						.desc("do not dump unresolved (i.e. library) methods")
						.build();
		CHOP = Option.builder("c")
						.longOpt("chop-types")
						.desc("use simple class names for return types and parameters instead of fully qualified ones")
						.build();
		FORMAT = Option.builder("f")
						.longOpt("format")
						.hasArg()
						.argName("format")
						.desc("the type of output graph. Defaults to " + Format.GRAPHML.name().toLowerCase() + ". Possible values: " 
								+ String.join(", ", Arrays.stream(Format.values()).map(Format::name).map(String::toLowerCase).toArray(String[]::new)))
						.build();
	}

	private static CommandLine cmdLine;

	public static void main(String[] args) {
		Options options = buildOptions();
		try {
			cmdLine = new DefaultParser().parse(options, args);

			CallGraphExplorer explorer = new CallGraphExplorer();
			for (String in : cmdLine.getOptionValues(INPUT.getOpt()))
				try {
					explorer.addJarEntries(in);
				} catch (IOException e) {
					System.err.println("Unable to open " + in);
					System.err.println(e);
				}

			explorer.computeCallingChains();

			boolean unresolved = cmdLine.hasOption(UNRESOLVED.getOpt());
			boolean chop = cmdLine.hasOption(CHOP.getOpt());
			String output = cmdLine.getOptionValue(OUTPUT.getOpt());
			Format format = Format.valueOf(cmdLine.getOptionValue(FORMAT.getOpt()));
			
			switch (format) {
			case DOT:
				new DotDumper(explorer, unresolved, chop).dump(output);
				break;
			case GRAPHML:
				new JGraphtDumper(explorer, unresolved, chop).dump(output);				
				break;
			default:
				throw new IllegalArgumentException("Unknown output format: " + format.name().toLowerCase());
			}
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
		hf.printHelp(Runner.class.getSimpleName(), options, true);
	}

	private static Options buildOptions() {
		Options result = new Options();
		result.addOption(INPUT);
		result.addOption(OUTPUT);
		result.addOption(UNRESOLVED);
		result.addOption(CHOP);
		result.addOption(FORMAT);
		return result;
	}
}
