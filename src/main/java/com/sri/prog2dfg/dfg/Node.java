package com.sri.prog2dfg.dfg;

public abstract class Node {
	
	protected final String label;
	protected final String debugLabel;
	
	private static long counter = 0L;
	
	public Node(String label, String debugLabel) {
		this.label = label + "_"+(counter++);
		this.debugLabel = debugLabel.replace("\"", "*");
	}
	
	public abstract Node duplicate();
	
	
	/**
	 * @return the debugLabel
	 */
	public String getDebugLabel() {
		return debugLabel;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}	
	
	@Override
	public String toString() {
		return label;
	}
	
}
