package com.sri.prog2dfg;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class SingleFileMain {

	public SingleFileMain() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		Options options = Options.v();
		CmdLineParser parser = new CmdLineParser(options);

		File classDir = null;
		SingleFileMain m = new SingleFileMain();
		try {
			// parse command-line arguments
			parser.parseArgument(args);
			
			String sourceFileName = Options.v().getJarFile();
			if (sourceFileName == null || !sourceFileName.endsWith(".java")) {
				System.err.println("Expect single .java file as input!");
				return;				
			}
			
			File sourceFile = new File(Options.v().getJarFile());
			classDir = m.compileJavaFile(sourceFile);
			Main.main(new String[]{"-j", classDir.getAbsolutePath()});

		} catch (CmdLineException e) {
			System.out.println(e.toString());
			parser.printUsage(System.err);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			m.delete(classDir);
		}
	}
	
	
	/**
	 * Takes a single .java file, compiles it, and returns a temp fold
	 * containing the class files.
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public File compileJavaFile(File sourceFile) throws IOException {
		final File tempDir = getTempDir();
		final String javac_command = String.format("javac -g %s -d %s",
				sourceFile.getAbsolutePath(), tempDir.getAbsolutePath());

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

	protected File getTempDir() throws IOException {
		final File tempDir = File.createTempFile("bixie_test_temp",
				Long.toString(System.nanoTime()));
		if (!(tempDir.delete())) {
			throw new IOException("Could not delete temp file: "
					+ tempDir.getAbsolutePath());
		}
		if (!(tempDir.mkdir())) {
			throw new IOException("Could not create temp directory: "
					+ tempDir.getAbsolutePath());
		}
		return tempDir;
	}

	public void delete(File f) {
		if (f==null) return;
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files==null) return;
			for (File c : files)
				delete(c);
		}
		if (!f.delete()) {
			System.err.println("Failed to delete file: " + f);
		}
	}	

}
