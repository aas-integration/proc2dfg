/**
 * 
 */
package com.sri.prog2dfg.dfg;

import soot.SootMethod;

/**
 * @author schaef
 *
 */
public class MethodNode extends Node {

	/**
	 * @param label
	 * @param debugLabel
	 */
	public MethodNode(String label, String debugLabel) {
		super(label, debugLabel);
		// TODO Auto-generated constructor stub
	}

	public MethodNode duplicate() {
		MethodNode clone = new MethodNode(label, debugLabel);
		clone.sootMethod = this.sootMethod;
		return clone;
	}
	
	public SootMethod sootMethod = null;
}
