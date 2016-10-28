package com.sri.prog2dfg.dfg;

public class Edge {
	public Node source;
	public final Node sink;
	public final String label;

	public Edge(Node source, String label, Node sink) {
		this.source = source;
		this.label = label;
		this.sink = sink;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(source);
		sb.append(",");
		sb.append(label);
		sb.append(",");
		sb.append(sink);
		sb.append(")");
		return sb.toString();
	}
}