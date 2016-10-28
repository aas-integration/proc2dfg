/*
 * jimple2boogie - Translates Jimple (or Java) Programs to Boogie
 * Copyright (C) 2013 Martin Schaeaeaeaeaeaeaeaeaef and Stephan Arlt
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sri.prog2dfg;

import java.io.File;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;

/**
 * Options
 * 
 * @author arlt, schaef
 */
public class Options {



	@Option(name = "-debug", usage = "Debug mode. E.g., prints jimple output to ./dump", required = false)
	private boolean debug=false;
	public boolean isDebug() {
		return debug;
	}
	public void setDebug(boolean val) {
		debug=val;;
	}
	
	@Option(name = "-verbose", usage = "Print debug output", required = false)
	public boolean verbose = false;

	/**
	 * JAR file
	 */
	@Option(name = "-j", usage = "JAR file", required = false)
	private String jarFile;

	/**
	 * JAR file
	 */
	@Option(name = "-pdf", usage = "generate pdf files", required = false)
	private boolean generatePdf = false;

	public boolean getGeneratePdf() {
		return this.generatePdf;
	}

	
	@Option(name = "-all", usage = "Treat all methods as entry points.", required = false)
	public boolean allEntry = false;

	@Option(name = "-minVerts", usage = "Ignore all graphs with less than minVerts nodes.", required = false)
	public int minVerts = 7;

	
	/**
	 * Output directory
	 */
	@Option(name = "-o", usage = "output directory", required = false)
	private String outDir = Paths.get("./dot").toAbsolutePath().toString();
	public String getOutputDirectory() {
		return outDir;
	}
	
	public void setOutputDirectory(String dirName) {
		File f = new File(dirName);
		if (!f.exists() || !f.isDirectory()) {		
			throw new RuntimeException(this.outDir + " is not a directory!");
		}
		outDir = f.getAbsolutePath();	
	}
	
	
	/**
	 * Classpath
	 */
	@Option(name = "-cp", usage = "Classpath")
	private String classpath;

	
	/**
	 * Classpath
	 */
	@Option(name = "-source", usage = "Optional file containing the set of source files used to build the classes. Seperated by path linebreak.")
	public String sourceFile= null;

	
	/**
	 * Scope
	 */
	@Option(name = "--scope", usage = "Scope")
	private String scope;

	/**
	 * Determines, whether Joogie has a scope
	 * 
	 * @return Scope of Joogie
	 */
	public boolean hasScope() {
		return (null != scope);
	}

	/**
	 * Returns the scope of Joogie
	 * 
	 * @return Scope of Joogie
	 */
	public String getScope() {
		return scope;
	}


	/**
	 * Returns the JAR file
	 * 
	 * @return JAR file
	 */
	public String getJarFile() {
		return jarFile;
	}

	/**
	 * Determines, whether Joogie has an additional classpath
	 * 
	 * @return true = Joogie has an additional classpath
	 */
	public boolean hasClasspath() {
		return (null != classpath);
	}

	/**
	 * Returns the additional classpath
	 * 
	 * @return Additional classpath
	 */
	public String getClasspath() {
		return classpath;
	}

	/**
	 * Assigns the additional classpath
	 * 
	 * @param classpath
	 *            Additional classpath
	 */
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
		
	/**
	 * Option object
	 */
	private static Options options;

	public static void resetInstance() {
		options = null;	
	}
	
	@Option(name = "-threadhack", usage = "try to build a call graph with threads. HACK", required = false)
	private boolean threadHack = false;
	public boolean useThreadHack() {
		return this.threadHack;
	}

	
	/**
	 * Singleton method
	 * 
	 * @return Options
	 */
	public static Options v() {
		if (null == options) {
			options = new Options();
		}
		return options;
	}

	/**
	 * C-tor
	 */
	private Options() {
		// do nothing
	}

}
