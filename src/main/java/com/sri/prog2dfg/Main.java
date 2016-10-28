/**
 * 
 */
package com.sri.prog2dfg;

import java.io.File;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.sri.prog2dfg.soot.DfgBuilder;

/**
 * @author schaef
 * 
 */
public class Main {

	/**
	 * 
	 */
	public Main() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options options = Options.v();
		CmdLineParser parser = new CmdLineParser(options);
		
		
		
		try {
			// parse command-line arguments
			parser.parseArgument(args);
			SootToGraph s2g = new SootToGraph();
			Map<String, DfgBuilder> graphs = s2g.run(Options.v().getJarFile(), Options.v().getClasspath());
			
			System.out.println("Number of graphs generated: "+graphs.size());
			File outdir = new File(Options.v().getOutputDirectory());
			if (Options.v().getOutputDirectory()!=null) {
				if (!outdir.exists() || !outdir.isDirectory()) {
					if (!outdir.mkdir()) {
						System.err.println("say sth meaningful");
					}
				}

				Util.generateDotFiles(outdir, graphs, Options.v().getGeneratePdf());
				System.out.println("Written output to "+ outdir.getAbsolutePath());
			}
			// get top 3 common nodes (if any)

		} catch (CmdLineException e) {
			System.out.println(e.toString());
			System.out.println("java -jar LALA.jar [options...] arguments...");
			parser.printUsage(System.err);
		}

	}

}
