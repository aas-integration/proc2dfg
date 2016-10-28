/**
 * 
 */
package com.sri.prog2dfg.dfg;

/**
 * @author schaef
 *
 */
public class ArgumentEdge extends Edge {

	public final Integer argPos;
	
	/**
	 * @param source
	 * @param label
	 * @param sink
	 */
	public ArgumentEdge(Node source, String label, Node sink, int argPos) {
		super(source, label, sink);
		this.argPos = argPos;
	}

}
