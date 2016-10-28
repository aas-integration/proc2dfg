/**
 * 
 */
package com.sri.prog2dfg.dfg;

/**
 * @author schaef
 *
 */
public class VariableNode extends Node {

	/**
	 * @param label
	 * @param debugLabel
	 */
	public VariableNode(String label, String debugLabel) {
		super(label, debugLabel);
		// TODO Auto-generated constructor stub
	}

	public VariableNode duplicate() {
		VariableNode clone = new VariableNode(label, debugLabel);		
		return clone;
	}

}
