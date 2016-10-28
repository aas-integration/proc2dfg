package com.sri.prog2dfg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sri.prog2dfg.soot.DfgBuilder;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLineNumberTag;
import soot.tagkit.Tag;

public final class Util {

	public static void generateDotFiles(File outputDir, Map<String,DfgBuilder> graphs, boolean generatePdf) {
		final String lineNumberMappingFilename = "sourcelines.txt";
		File lineNumberMappingFile = new File(outputDir.getAbsolutePath() + File.separator + lineNumberMappingFilename);
		writeSignature2SourceLocationMapping(graphs.keySet(), lineNumberMappingFile);
		
		final String methodToDotMappingFilename = "methods.txt";
		File mappingFile = new File(outputDir.getAbsolutePath() + File.separator + methodToDotMappingFilename);
		try (FileWriter fileWritter = new FileWriter(mappingFile.getAbsolutePath(), false);
				BufferedWriter bufferWritter = new BufferedWriter(fileWritter);) {
			long counter = 0L;
			for (Entry<String, DfgBuilder> entry : graphs.entrySet()) {
				// create the dot file.
				String dotFilename = String.format("%09d.dot", counter);
				File dotFile = new File(outputDir.getAbsolutePath() + File.separator + dotFilename);
				
				generateDot(entry.getValue(), dotFile.getAbsolutePath(), generatePdf);
				bufferWritter.write(entry.getKey());
				bufferWritter.write("\t");
				bufferWritter.write(dotFilename);
				bufferWritter.write("\n");
				counter++;
				
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("Storing failed: " + e.toString());
		}

	}

	private static void generateDot(DfgBuilder graph, String fileName, boolean generatePdf) {
		graph.toDot(fileName);
		
		if (generatePdf) {
			try {
				String pdffileName = fileName.replace(".dot", ".pdf");
				Process process = Runtime.getRuntime()
						.exec("/usr/local/bin/dot -Tpdf " + fileName + " -o " + pdffileName);
				Worker worker = new Worker(process);
				worker.start();
				try {
					worker.join(1000);
				} catch (InterruptedException ex) {
					worker.interrupt();
					Thread.currentThread().interrupt();
					System.err.println("Failed to generate PDF from dot");
				} finally {
					process.destroy();
				}

			} catch (Throwable e) {
				System.err.println(e.toString());
			}
		}
	}
	
	
	private static void writeSignature2SourceLocationMapping(Set<String> methods, File outFile) {
		if (Options.v().sourceFile==null) return;
		File sourceListFile = new File(Options.v().sourceFile);
		if (!sourceListFile.isFile()) {
			System.err.println("Couldn't write the source code mapping because I dont have the source list.");
			return;
		}
		Set<String> sourceFileNames = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(sourceListFile))) {
			String line;
			while ((line = br.readLine()) != null) {
//				System.out.println(line);
				sourceFileNames.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (FileWriter fileWritter = new FileWriter(outFile.getAbsolutePath(), false);
				BufferedWriter bufferWritter = new BufferedWriter(fileWritter);) {

			for (String methodSignature : methods) {
				SootMethod m = Scene.v().grabMethod(methodSignature);
				SootClass sc = m.getDeclaringClass();
				while (sc.hasOuterClass()) {
					sc = sc.getOuterClass();
				}
				String javaName = sc.getName();
				if (javaName.indexOf('$') != -1) {
					javaName = javaName.substring(0, javaName.indexOf('$'));
				}
				javaName = javaName.replace(".", File.separator);
				javaName = javaName + ".java";

				String fullFileName = null;
				for (String s : sourceFileNames) {
					if (s.endsWith(javaName)) {
						fullFileName = javaName;
					}
				}
				if (fullFileName != null) {
					int startline = Integer.MAX_VALUE;
					int endline = -1;

					for (Unit u : m.retrieveActiveBody().getUnits()) {
						int ln = getLineNumber(u.getTags());
						startline = (ln > 0) ? Math.min(startline, ln) : startline;
						endline = Math.max(endline, ln);
					}
					
					bufferWritter.write(m.getSignature());
					bufferWritter.write("\t");
					bufferWritter.write(fullFileName);
					bufferWritter.write("\t");
					bufferWritter.write(Integer.toString(startline));
					bufferWritter.write("\t");
					bufferWritter.write(Integer.toString(endline));
					bufferWritter.write("\n");
					
				}
			}

		} catch (Throwable e) {
			throw new RuntimeException("Storing failed: " + e.toString());
		}

	}

	private static int getLineNumber(List<Tag> list) {
		for (Tag t : list) {
			if (t instanceof LineNumberTag) {
				return ((LineNumberTag) t).getLineNumber();
			} else if (t instanceof SourceLineNumberTag) {
				return ((SourceLineNumberTag) t).getLineNumber();
			}
		}
		return -1;
	}

	
	static class Worker extends Thread {
		public final Process process;
		public Integer exit;

		Worker(Process process) {
			this.process = process;
		}

		public void run() {
			try {
				exit = process.waitFor();
			} catch (InterruptedException ignore) {
				return;
			}
		}
	}
	
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "DM_DEFAULT_ENCODING")
	public static String fileToString(File f) {
		StringBuffer sb = new StringBuffer();
		try (FileReader fileRead = new FileReader(f); BufferedReader reader = new BufferedReader(fileRead);) {
			String line;
			while (true) {
				line = reader.readLine();
				if (line == null)
					break;
				sb.append(line);
				sb.append("\n");
			}
		} catch (Throwable e) {

		}
		return sb.toString();
	}

	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "DM_DEFAULT_ENCODING")
	public static boolean compareFiles(File out, File gold) {
		try (FileReader fR1 = new FileReader(out);
				FileReader fR2 = new FileReader(gold);
				BufferedReader reader1 = new BufferedReader(fR1);
				BufferedReader reader2 = new BufferedReader(fR2);) {
			String line1, line2;
			while (true) // Continue while there are equal lines
			{
				line1 = reader1.readLine();
				line2 = reader2.readLine();

				// End of file 1
				if (line1 == null) {
					// Equal only if file 2 also ended
					return (line2 == null ? true : false);
				}

				// Different lines, or end of file 2
				if (!line1.equalsIgnoreCase(line2)) {
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Compiles a sourceFile into a temp folder and returns this folder or null
	 * if compilation fails.
	 * 
	 * @param sourceFile
	 * @return the folder that contains the class file(s) or null if compilation
	 *         fails.
	 * @throws IOException
	 */
	public static File compileJavaFile(File sourceFile) throws IOException {
		final File tempDir = getTempDir();
		final String javac_command = String.format("javac -g %s -d %s", sourceFile.getAbsolutePath(),
				tempDir.getAbsolutePath());

		ProcessBuilder pb = new ProcessBuilder(javac_command.split(" "));
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		Process p = pb.start();

		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		return tempDir;
	}

	/**
	 * Compiles a set of sourceFiles into a temp folder and returns this folder
	 * or null if compilation fails.
	 * 
	 * @param sourceFile
	 * @return the folder that contains the class file(s) or null if compilation
	 *         fails.
	 * @throws IOException
	 */
	public static File compileJavaFiles(File[] sourceFiles) throws IOException {
		final File tempDir = getTempDir();
		StringBuilder sb = new StringBuilder();
		for (File f : sourceFiles) {
			sb.append(f.getAbsolutePath());
			sb.append(" ");
		}
		final String javac_command = String.format("javac -g -d %s %s", tempDir.getAbsolutePath(), sb.toString());

//		System.out.println(javac_command);

		ProcessBuilder pb = new ProcessBuilder(javac_command.split(" "));
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		Process p = pb.start();

		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		return tempDir;
	}

	public static File getTempDir() throws IOException {
		final File tempDir = File.createTempFile("bixie_test_temp", Long.toString(System.nanoTime()));
		if (!(tempDir.delete())) {
			throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
		}
		if (!(tempDir.mkdir())) {
			throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
		}
		return tempDir;
	}

	public static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}
	}
}
