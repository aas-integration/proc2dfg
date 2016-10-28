/**
 * 
 */
package com.sri.prog2dfg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sri.prog2dfg.soot.DfgBuilder;
import com.sri.prog2dfg.soot.SootRunner;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

/**
 * @author schaef
 *
 */
public class SootToGraph {

	Map<String, DfgBuilder> methodGraphs;

	/**
	 * Run Soot and translate classes into Boogie/Horn
	 * 
	 * @param input
	 *            class folder, jar file, or apk file
	 * @param classPath
	 *            class path, or platform jar folder for apk. see
	 *            https://github.com/Sable/android-platforms
	 * @param cfg
	 */
	public Map<String, DfgBuilder> run(String input, String classPath) {
		methodGraphs = new LinkedHashMap<String, DfgBuilder>();
		// run soot to load all classes.
		SootRunner runner = new SootRunner();
		runner.run(input, classPath);

		List<SootClass> classes = new LinkedList<SootClass>(Scene.v().getClasses());
		for (SootClass sc : classes) {
			processSootClass(sc);
		}

		// //TODO: only for benchmarks
		// for (SootMethod sm : Scene.v().getEntryPoints()) {
		// if (sm.isMain()) {
		// printPetabloxMain(sm);
		// break;
		// }
		// }

		return methodGraphs;
	}

	protected void printPetabloxMain(SootMethod m) {
		File propFile = new File("petablox.properties");
		try (FileWriter fileWritter = new FileWriter(propFile.getAbsolutePath(), false);
				BufferedWriter writer = new BufferedWriter(fileWritter);) {
			writer.write("petablox.main.class=");
			writer.write(m.getDeclaringClass().getName());
			writer.newLine();
			writer.write("petablox.class.path=classes/");
			writer.newLine();
			writer.write("petablox.src.path=src/");
			writer.newLine();
			System.out.println(writer.toString());
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("Storing failed: " + e.toString());
		}
	}

	/**
	 * Analyze a single SootClass and transform all its Methods
	 * 
	 * @param sc
	 */
	private void processSootClass(SootClass sc) {
		if (sc.resolvingLevel() < SootClass.SIGNATURES) {
			return;
		}

		if (sc.isApplicationClass()) {
			Map<SootMethod, DfgBuilder> dfgs = new LinkedHashMap<SootMethod, DfgBuilder>();
			for (SootMethod sm : sc.getMethods()) {
				DfgBuilder gb = processSootMethod(sm);
				if (gb != null) {
					// TODO: Hack
					dfgs.put(sm, gb);
				}
			}

			Map<SootMethod, Set<SootMethod>> isCalledByMap = new HashMap<SootMethod, Set<SootMethod>>();
			for (Entry<SootMethod, DfgBuilder> entry : dfgs.entrySet()) {
				for (SootMethod m : entry.getValue().callsTo) {
					if (!isCalledByMap.containsKey(m)) {
						isCalledByMap.put(m, new HashSet<SootMethod>());
					}
					isCalledByMap.get(m).add(entry.getKey());
				}
			}
			Set<SootMethod> calledMethods = new HashSet<SootMethod>();
			for (Entry<SootMethod, Set<SootMethod>> entry : isCalledByMap.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					calledMethods.add(entry.getKey());
				}
			}

			int ignored = 0;

			for (Entry<SootMethod, DfgBuilder> entry : dfgs.entrySet()) {
				// debug code
				// dfgToDot("non-inlined_"+entry.getKey().getName(),
				// entry.getValue());
				if (Options.v().allEntry && entry.getValue().getNodes().size() < Options.v().minVerts) {
					ignored++;
					continue;
				}

				DfgBuilder inlinedGraph = entry.getValue().duplicate();
				Set<SootMethod> inlinedAlready = new HashSet<SootMethod>();
				inlinedAlready.add(entry.getKey());
				inlinedGraph.inlineLocalCalls(dfgs, inlinedAlready);

				if (Options.v().allEntry) {
					methodGraphs.put(entry.getKey().getSignature(), inlinedGraph);
				} else {
					if (!calledMethods.contains(entry.getKey())) {
						if (entry.getKey().isConstructor()) {
							if (Options.v().verbose) {
								System.out.println("Ignoring constructor: " + entry.getKey().getSignature());
							}
						} else {
							if (Options.v().verbose) {
								System.out.println("Writing method that is not called from anywhere "
										+ entry.getKey().getSignature());
							}
							methodGraphs.put(entry.getKey().getSignature(), inlinedGraph);
						}
					} else {
						if (Options.v().verbose) {
							System.out.println("Ignoring " + entry.getKey().getSignature()
									+ " because it is being called by other methods.");
						}
					}
				}

				// dfgToDot(entry.getKey().getName()+"_inlined", inlinedGraph);
			}
			if (Options.v().allEntry) {

				if (Options.v().verbose) {
					System.out.println(
							"Ignored " + ignored + " procedures with less than " + Options.v().minVerts + " nodes");
				}
			}
		}

	}

	private DfgBuilder processSootMethod(SootMethod sm) {
		if (Options.v().hasScope()) {
			if (!sm.getSignature().contains(Options.v().getScope())) {
				return null; // ignore current body
			}
		}

		try {
		if (sm.isConcrete() && !sm.isStaticInitializer()) {			
			Body body = sm.retrieveActiveBody();
			return processMethodBody(body);
		}
		} catch (Exception e) {
			System.err.println("No code for "+sm.getSignature());
		}
		return null;
	}

	private DfgBuilder processMethodBody(Body body) {
		// return new DfgBuilder(body, new BriefUnitGraph(body));
		return new DfgBuilder(body);
	}

}
