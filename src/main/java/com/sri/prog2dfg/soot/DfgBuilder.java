package com.sri.prog2dfg.soot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.sri.prog2dfg.Options;
import com.sri.prog2dfg.dfg.ArgumentEdge;
import com.sri.prog2dfg.dfg.Edge;
import com.sri.prog2dfg.dfg.MethodNode;
import com.sri.prog2dfg.dfg.Node;
import com.sri.prog2dfg.dfg.VariableNode;
import com.sun.javafx.binding.StringConstant;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UnopExpr;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class DfgBuilder extends ForwardFlowAnalysis<Unit, Map<Value, Node>> {

	private static final String baseLabel = "base";
	private static final String argLabel = "$ARG";

	private final Body body;
	private final List<Node> parameterNodes;
	private Node returnNode, thisNode;

	private final Set<Node> allNodes = new HashSet<Node>();
	private final Set<Edge> allEdges = new HashSet<Edge>();
	private final Map<Node, Set<Edge>> incommingEdges = new HashMap<Node, Set<Edge>>();
	private final Map<Node, Set<Edge>> outgoingEdges = new HashMap<Node, Set<Edge>>();

	private final Map<SootField, Node> fieldMap = new HashMap<SootField, Node>();
	// private final Map<SootField, Node> staticFieldMap = new
	// HashMap<SootField, Node>();

	private final SootMethod method;

	/**
	 * Copy constructor
	 * 
	 * @param allNodes
	 * @param allEdges
	 * @param parameterNodes
	 * @param returnNode
	 * @param thisNode
	 * @param sm
	 */
	private DfgBuilder(Set<Node> allNodes, Set<Edge> allEdges, List<Node> parameterNodes, Node returnNode,
			Node thisNode, SootMethod sm, Map<SootField, Node> fieldMap) {
		super(new CompleteUnitGraph(sm.getActiveBody()));
		this.method = sm;
		this.body = sm.getActiveBody();
		Map<Node, Node> cloneMap = new HashMap<Node, Node>();
		for (Node n : allNodes) {
			this.allNodes.add(duplicateNode(cloneMap, n));
		}
		this.returnNode = cloneMap.get(returnNode);
		this.thisNode = cloneMap.get(thisNode);
		this.parameterNodes = new LinkedList<Node>();
		for (Node p : parameterNodes) {
			this.parameterNodes.add(cloneMap.get(p));
		}

		for (Entry<SootField, Node> entry : fieldMap.entrySet()) {
			this.fieldMap.put(entry.getKey(), cloneMap.get(entry.getValue()));
		}

		// now restore the edges.
		for (Edge e : allEdges) {
			if (e instanceof ArgumentEdge) {
				createEdge(cloneMap.get(e.source), new String(e.label), cloneMap.get(e.sink),
						((ArgumentEdge) e).argPos);
			} else {
				createEdge(cloneMap.get(e.source), new String(e.label), cloneMap.get(e.sink));
			}
		}
	}

	public void onlyKeepLargestConnectedSet() {
		DirectedGraph<Node, DefaultEdge> g = new DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge.class);
		for (Node n : allNodes) {
			g.addVertex(n);
		}
		for (Edge e : allEdges) {
			if (!g.containsVertex(e.source))
				System.err.println("Forgot to add " + e.source);
			if (!g.containsVertex(e.sink))
				System.err.println("Forgot to add " + e.sink);
			g.addEdge(e.source, e.sink);
		}
		ConnectivityInspector<Node, DefaultEdge> ci = new ConnectivityInspector<Node, DefaultEdge>(g);
		List<Set<Node>> connectedSets = ci.connectedSets();
		if (connectedSets.size() <= 1)
			return;
		int max = 0;
		for (Set<Node> cs : connectedSets) {
			if (cs.size() > max) {
				max = cs.size();
			}
		}
		Set<Node> toRemove = new HashSet<Node>();
		Set<Node> toKeep = new HashSet<Node>();
		toKeep.addAll(fieldMap.values());
		toKeep.add(thisNode);
		toKeep.add(returnNode);
		toKeep.addAll(parameterNodes);
		for (Set<Node> cs : connectedSets) {
			if (cs.size() < max) {
				boolean cantRemove = false;
				for (Node n : cs) {
					if (toKeep.contains(n)) {
						cantRemove = true;
						break;
					}
				}
				if (cantRemove)
					continue;
				toRemove.addAll(cs);
			}
		}
		if (toRemove.isEmpty())
			return;
		System.err.println("Removing " + toRemove.size() + " nodes of " + allNodes.size());
		allNodes.removeAll(toRemove);
		Set<Edge> edgesToRemove = new HashSet<Edge>();
		for (Edge e : allEdges) {
			if (toRemove.contains(e.source) || toRemove.contains(e.sink)) {
				edgesToRemove.add(e);
			}
		}
		allEdges.removeAll(edgesToRemove);
		for (Node n : toRemove) {
			incommingEdges.remove(n);
			outgoingEdges.remove(n);
		}
		for (Entry<Node, Set<Edge>> entry : incommingEdges.entrySet()) {
			entry.getValue().removeAll(edgesToRemove);
		}
		for (Entry<Node, Set<Edge>> entry : outgoingEdges.entrySet()) {
			entry.getValue().removeAll(edgesToRemove);
		}
		foldRenamings();
	}

	public void foldRenamings() {

		while (true) {
			Node first = null, second = null;
			for (Entry<Node, Set<Edge>> entry : outgoingEdges.entrySet()) {
				if (entry.getValue().size() == 1) {
					Node succ = entry.getValue().iterator().next().sink;
					if (incommingEdges.containsKey(succ) && incommingEdges.get(succ).size() == 1) {
						if (succ.getDebugLabel().equals(entry.getKey().getDebugLabel()) && entry.getKey() != succ) {
							first = entry.getKey();
							second = succ;
							break;
						}
					}
				}
			}
			if (first == null || second == null)
				return;
			allEdges.removeAll(outgoingEdges.get(first));
			allEdges.removeAll(incommingEdges.get(second));
			outgoingEdges.remove(first);
			incommingEdges.remove(second);
			allNodes.remove(second);

			Set<Edge> outgoing = new HashSet<Edge>();
			for (Edge e : allEdges) {
				if (e.source == second) {
					e.source = first;
					outgoing.add(e);
				}
			}
			if (!outgoing.isEmpty()) {
				outgoingEdges.put(first, outgoing);
			}
			outgoingEdges.remove(second);

		}
	}

	private Node duplicateNode(Map<Node, Node> cloneMap, Node orig) {
		cloneMap.put(orig, orig.duplicate());
		return cloneMap.get(orig);
	}

	public DfgBuilder duplicate() {
		return new DfgBuilder(this.allNodes, this.allEdges, this.parameterNodes, this.returnNode, this.thisNode,
				this.method, this.fieldMap);
	}

	public DfgBuilder(Body body, UnitGraph ug) {
		super(ug);
		this.body = body;
		method = body.getMethod();
		parameterNodes = new ArrayList<Node>(method.getParameterCount());
		for (int i = 0; i < method.getParameterCount(); i++) {
			Node node = createVariableNode(method.getParameterType(i), shortLabelForType(method.getParameterType(i)));
			parameterNodes.add(i, node);
			allNodes.add(node);
		}
		if (method.getReturnType() != VoidType.v()) {
			returnNode = createVariableNode(method.getReturnType(), shortLabelForType(method.getReturnType()));
			allNodes.add(returnNode);
		} else {
			returnNode = null;
		}

		this.doAnalysis();
	}

	public DfgBuilder(Body body) {
		this(body, new CompleteUnitGraph(body));
	}

	public void inlineLocalCalls(Map<SootMethod, DfgBuilder> dfgs, Set<SootMethod> alreadyInlined) {
		List<MethodNode> worklist = new LinkedList<MethodNode>();
		// if (this.method.isStatic()) {
		// System.err.println("adhfjksdf");
		// }
		for (Node n : allNodes) {
			if (n instanceof MethodNode) {
				MethodNode mn = (MethodNode) n;

				if (mn.sootMethod != null
						&& mn.sootMethod.getDeclaringClass().equals(this.method.getDeclaringClass())) {
					if (canBeInlined(mn)) {
						if (alreadyInlined.contains(mn.sootMethod)) {
							if (Options.v().verbose) {
								System.err.println("inlining of recursive stuff is not implemented");
							}
						} else {
							worklist.add(mn);
						}
					}
				}
			}
		}
		for (MethodNode mn : worklist) {
			inlineMethod(dfgs, mn, alreadyInlined);
		}
	}

	private void inlineMethod(Map<SootMethod, DfgBuilder> dfgs, MethodNode mn, Set<SootMethod> alreadyInlined) {
		// create a copy of the graph that should be inlined.
		if (!dfgs.containsKey(mn.sootMethod)) {
			System.err.println("Cannot inline " + mn.sootMethod.getSignature());
			return;
		}
		DfgBuilder toInline = dfgs.get(mn.sootMethod).duplicate();
		Set<SootMethod> inl = new HashSet<SootMethod>(alreadyInlined);
		inl.add(mn.sootMethod);
		// first recur to inline the calls in toInline.
		toInline.inlineLocalCalls(dfgs, inl);

		// System.err.println("Maybe we should inline " +
		// mn.sootMethod.getSignature());
		/*
		 * Now do the actual inlining: - rewire the args to the params - rewire
		 * the return value if there is one - rewire the fields (if that makes
		 * sense).
		 */
		Set<Edge> edgesToRemove = new HashSet<Edge>();
		if (incommingEdges.containsKey(mn)) {
			for (Edge e : incommingEdges.get(mn)) {
				if (e instanceof ArgumentEdge) {
					toInline.replaceNode(toInline.getParameterNode(((ArgumentEdge) e).argPos), e.source);
					edgesToRemove.add(e);
				} else {
					if (e.label.equals(baseLabel)) {
						toInline.replaceNode(toInline.getThisNode(), e.source);
						edgesToRemove.add(e);
					}
				}
			}
			// now remove the edges of the node that has been substituted.
			for (Edge e : edgesToRemove) {
				allEdges.remove(e);
				if (incommingEdges.containsKey(mn) && incommingEdges.get(mn).contains(e)) {
					incommingEdges.get(mn).remove(e);
				}
			}
		}
		edgesToRemove.clear();
		if (outgoingEdges.containsKey(mn) && !outgoingEdges.get(mn).isEmpty()) {
			if (outgoingEdges.get(mn).size() > 1) {
				System.err.println("more than one successor as return value... thats odd");
			}
			Edge e = outgoingEdges.get(mn).iterator().next();
			toInline.replaceNode(toInline.getReturnNode(), e.sink);
			outgoingEdges.get(mn).remove(e);
			allEdges.remove(e);
		}

		// replace the nodes from the field map.
		for (Entry<SootField, Node> entry : new HashMap<SootField, Node>(toInline.getFieldMap()).entrySet()) {
			if (this.fieldMap.containsKey(entry.getKey())) {
				toInline.replaceNode(entry.getValue(), this.fieldMap.get(entry.getKey()));
			}
		}

		// System.out.println(toInline.getNodes().size());
		// System.out.println(toInline.getEdges().size());

		this.allNodes.addAll(toInline.getNodes());
		this.allEdges.addAll(toInline.allEdges);
		this.incommingEdges.putAll(toInline.getIncomingEdges());
		this.outgoingEdges.putAll(toInline.getOutgoingEdges());
		// finally remove the method node.
		allNodes.remove(mn);
		incommingEdges.remove(mn);
		outgoingEdges.remove(mn);
	}

	public Map<SootField, Node> getFieldMap() {
		return this.fieldMap;
	}

	public Map<Node, Set<Edge>> getIncomingEdges() {
		return this.incommingEdges;
	}

	public Map<Node, Set<Edge>> getOutgoingEdges() {
		return this.outgoingEdges;
	}

	public Set<Edge> getEdges() {
		return this.allEdges;
	}

	public Set<Node> getNodes() {
		return this.allNodes;
	}

	public Node getParameterNode(int i) {
		return this.parameterNodes.get(i);
	}

	public Node getThisNode() {
		return this.thisNode;
	}

	public Node getReturnNode() {
		return this.returnNode;
	}

	public void replaceNode(Node origNode, Node newNode) {
		// System.err.println("Replacing " + origNode.getDebugLabel() + " by " +
		// newNode.getDebugLabel());
		if (!allNodes.contains(origNode)) {
			System.err.println("Cant replace a node that doesnt exist");
			return;
		}
		allNodes.remove(origNode);
		allNodes.add(newNode);
		if (incommingEdges.containsKey(origNode)) {
			Set<Edge> edges = new HashSet<Edge>(incommingEdges.get(origNode));
			allEdges.removeAll(edges);
			incommingEdges.put(newNode, new HashSet<Edge>());
			for (Edge e : edges) {
				Edge newEdge = null;
				if (e instanceof ArgumentEdge) {
					// TODO: this is not an Argument edge anymore after
					// replacing the node.
					newEdge = new Edge(e.source, e.label, newNode);
					// newEdge = new ArgumentEdge(e.source, e.label, newNode,
					// ((ArgumentEdge) e).argPos);
				} else {
					newEdge = new Edge(e.source, e.label, newNode);
				}
				incommingEdges.get(newNode).add(newEdge);
				allEdges.add(newEdge);
			}
		}
		if (outgoingEdges.containsKey(origNode)) {
			Set<Edge> edges = new HashSet<Edge>(outgoingEdges.get(origNode));
			allEdges.removeAll(edges);
			outgoingEdges.put(newNode, new HashSet<Edge>());
			for (Edge e : edges) {
				Edge newEdge = null;
				if (e instanceof ArgumentEdge) {
					// TODO: this is not an Argument edge anymore after
					// replacing the node.
					newEdge = new Edge(newNode, e.label, e.sink);
					// newEdge = new ArgumentEdge(e.source, e.label, newNode,
					// ((ArgumentEdge) e).argPos);
				} else {
					newEdge = new Edge(newNode, e.label, e.sink);
				}
				outgoingEdges.get(newNode).add(newEdge);
				allEdges.add(newEdge);
			}
		}
		if (origNode == this.thisNode) {
			this.thisNode = newNode;
		}

		if (origNode == this.returnNode) {
			this.returnNode = newNode;
		}

		for (Entry<SootField, Node> entry : new HashMap<SootField, Node>(this.fieldMap).entrySet()) {
			if (entry.getValue() == origNode) {
				this.fieldMap.put(entry.getKey(), newNode);
				break;
			}
		}

	}

	private boolean canBeInlined(MethodNode mn) {
		if (this.method.isStatic() && mn.sootMethod.getSignature().contains("run") && Options.v().useThreadHack()) {
			if (Options.v().verbose) {
				System.err.println("hacked inlining");
			}
			return true;
		}
		if (incommingEdges.containsKey(mn)) {
			for (Edge e : incommingEdges.get(mn)) {
				if (e.label.equals(baseLabel)) {
					return thisRefIsBackwardReachable(new HashSet<Node>(), e.source);
				}
			}
		}
		return false;
	}

	private boolean thisRefIsBackwardReachable(Set<Node> visited, Node n) {
		if (visited.contains(n)) {
			return false;
		}
		visited.add(n);
		// we can descend first because we know that $this has no predecessors.
		if (incommingEdges.containsKey(n)) {
			for (Edge e : incommingEdges.get(n)) {
				if (thisRefIsBackwardReachable(visited, e.source)) {
					return true;
				}
			}
		}
		return n == thisNode;
	}

	public Set<SootMethod> callsTo = new HashSet<SootMethod>();

	@Override
	protected void flowThrough(Map<Value, Node> in, Unit u, Map<Value, Node> out) {
		out.putAll(in);
		if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
			// get an approximation of the method that this method calls out to.
			callsTo.add(((Stmt) u).getInvokeExpr().getMethod());
		}

		if (u instanceof DefinitionStmt) {
			DefinitionStmt ds = (DefinitionStmt) u;
			Node left = getLhsNode(out, ds.getLeftOp());
			Set<Node> right = getRhsNodes(out, ds.getRightOp());
			for (Node n : right) {
				createEdge(n, "==", left);
			}
		} else if (u instanceof InvokeStmt) {
			getRhsNodes(out, ((InvokeStmt) u).getInvokeExpr());
		} else if (u instanceof ReturnStmt) {
			for (Node n : getRhsNodes(out, ((ReturnStmt) u).getOp())) {
				createEdge(n, "return", returnNode);
			}
		} else if (u instanceof ThrowStmt) {
			// TODO
		}
	}

	private String makeCleanUniqueLabel(String dirtyLabel) {
		return dirtyLabel.replace("\"", "*");
	}

	private Node createMethodNode(SootMethod sm, String label) {
		MethodNode node = new MethodNode(makeCleanUniqueLabel(sm.getSignature()), label);
		node.sootMethod = sm;
		allNodes.add(node);
		// TODO
		return node;
	}

	private Node createVariableNode(Type t, String label) {
		//String.format("%07d", number)+
		Node node = new VariableNode(t.toString() + makeCleanUniqueLabel(label), label);
		allNodes.add(node);
		// TODO
		return node;
	}

	private Edge createEdge(Node from, String label, Node to) {
		return createEdge(from, label, to, -1);
	}

	private Edge createEdge(Node from, String label, Node to, int argPos) {
		for (Edge e : allEdges) {
			//if (e.sink == to && e.source == from && e.label.equals(label)) {
			if (e.sink == to && e.source == from ) {
				// Don't create edges twice...
				// TODO could be done faster.
				return e;
			}
		}

		Edge e;
		if (argPos < 0) {
			e = new Edge(from, label, to);
		} else {
			e = new ArgumentEdge(from, label, to, argPos);
		}

		if (!incommingEdges.containsKey(to)) {
			incommingEdges.put(to, new HashSet<Edge>());
		}
		incommingEdges.get(to).add(e);
		if (!outgoingEdges.containsKey(from)) {
			outgoingEdges.put(from, new HashSet<Edge>());
		}
		outgoingEdges.get(from).add(e);

		allEdges.add(e);
		return e;
	}

	private Node getLhsNode(Map<Value, Node> nodeMap, Value v) {
		if (nodeMap.containsKey(v)) {
			return nodeMap.get(v);
		}
		if (v instanceof Local) {
			return lookupOrCreateNode(nodeMap, v); // TODO
		} else if (v instanceof StaticFieldRef) {
			StaticFieldRef sfr = (StaticFieldRef) v;
			if (!this.fieldMap.containsKey(sfr.getField())) {
				Node node = lookupOrCreateNode(nodeMap, v); // TODO
				this.fieldMap.put(sfr.getField(), node);
			}
			Node node = this.fieldMap.get(sfr.getField());
			nodeMap.put(v, node);
			return node;
		} else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) v;
			Node base = lookupOrCreateNode(nodeMap, ifr.getBase());
			Node field = lookupOrCreateNode(nodeMap, v);
			createEdge(base, baseLabel, field);
			createEdge(field, baseLabel, base);

			return field; // TODO
		} else if (v instanceof ArrayRef) {
			ArrayRef ar = (ArrayRef) v;
			Node baseNode = getLhsNode(nodeMap, ar.getBase());
			Set<Node> idxNodes = getRhsNodes(nodeMap, ar.getIndex());
			for (Node idx : idxNodes) {
				createEdge(idx, "idx", baseNode);
			}
			nodeMap.put(v, baseNode);
		} else {
			throw new RuntimeException("Unexpected LHS type " + v.getClass().toString());
		}
		return nodeMap.get(v);
	}

	private Set<Node> getRhsNodes(Map<Value, Node> nodeMap, Value v) {
		Set<Node> ret = new HashSet<Node>();
		if (nodeMap.containsKey(v)) {
			ret.add(nodeMap.get(v));
			return ret;
		}

		if (v instanceof BinopExpr) {
			ret.addAll(getRhsNodes(nodeMap, ((BinopExpr) v).getOp1()));
			ret.addAll(getRhsNodes(nodeMap, ((BinopExpr) v).getOp2()));
		} else if (v instanceof UnopExpr) {
			if (v instanceof LengthExpr) {
				Node args = lookupOrCreateNode(nodeMap, ((LengthExpr) v).getOp());
				Node lenCall = new MethodNode(makeCleanUniqueLabel("lengthOf"), v.toString()); // TODO
				allNodes.add(lenCall); // TODO
				createEdge(args, "$length", lenCall);
				nodeMap.put(v, lenCall);
				ret.add(lenCall);
			} else {
				ret.addAll(getRhsNodes(nodeMap, ((UnopExpr) v).getOp()));
			}

		} else if (v instanceof CastExpr) {
			ret.addAll(getRhsNodes(nodeMap, ((CastExpr) v).getOp()));
		} else if (v instanceof ArrayRef) {
			ArrayRef ar = (ArrayRef) v;
			getRhsNodes(nodeMap, ar.getBase());
			getRhsNodes(nodeMap, ar.getIndex());
		} else if (v instanceof StaticFieldRef) {
			StaticFieldRef sfr = (StaticFieldRef) v;
			if (!this.fieldMap.containsKey(sfr.getField())) {
				Node node = lookupOrCreateNode(nodeMap, v); // TODO
				this.fieldMap.put(sfr.getField(), node);
			}
			Node node = this.fieldMap.get(sfr.getField());
			nodeMap.put(v, node);
			ret.add(node);
		} else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) v;
			Node base = lookupOrCreateNode(nodeMap, ifr.getBase());
			Node field = lookupOrCreateNode(nodeMap, v);
			createEdge(base, baseLabel, field);
			ret.add(field);
		} else if (v instanceof ParameterRef) {
			ParameterRef pr = (ParameterRef) v;
			ret.add(parameterNodes.get(pr.getIndex()));
		} else if (v instanceof ThisRef) {
			if (!nodeMap.containsKey(v)) {
				thisNode = lookupOrCreateNode(nodeMap, v);
			}
			ret.add(thisNode);
		} else if (v instanceof AnyNewExpr) {
			ret.add(lookupOrCreateNode(nodeMap, v));
		} else if (v instanceof CaughtExceptionRef) {
			ret.add(lookupOrCreateNode(nodeMap, v));
		} else if (v instanceof InstanceOfExpr) {
			// Ignore
		} else if (v instanceof Local) {
			ret.add(lookupOrCreateNode(nodeMap, v));
		} else if (v instanceof Constant) {
			ret.add(lookupOrCreateNode(nodeMap, v));
		} else if (v instanceof InvokeExpr) {
			InvokeExpr ie = (InvokeExpr) v;
			ret.add(getInvokationNode(nodeMap, ie));
		} else {
			System.err.println("ignored " + v.getClass().toString());
		}
		return ret;
	}

	private Map<Value, SootClass> hackyThreadMap = new HashMap<Value, SootClass>();

	private Node getInvokationNode(Map<Value, Node> nodes, InvokeExpr ie) {

		if (nodes.containsKey(ie)) {
			return nodes.get(ie);
		}

		if (Options.v().useThreadHack()) {
			if ("<java.lang.Thread: void <init>(java.lang.Runnable)>".equals(ie.getMethod().getSignature())) {
				hackyThreadMap.put(((InstanceInvokeExpr) ie).getBase(),
						((RefType) ie.getArg(0).getType()).getSootClass());
			}
		}

		List<Set<Node>> argNodes = new ArrayList<Set<Node>>();
		for (Value arg : ie.getArgs()) {
			argNodes.add(getRhsNodes(nodes, arg));
		}

		SootMethod sootMethod = ie.getMethod();
		if (Options.v().useThreadHack()) {
			if ("<java.lang.Thread: void start()>".equals(sootMethod.getSignature())) {
				if (!hackyThreadMap.containsKey(((InstanceInvokeExpr) ie).getBase())) {
					System.err.println("Thread hack failed");
				} else {
					System.err.println("Thread hack applied");
					sootMethod = hackyThreadMap.get(((InstanceInvokeExpr) ie).getBase()).getMethodByName("run");
				}
			}
		} else {
			// do nothing
		}

		Node methodNode = createMethodNode(sootMethod, shortLabelForType(sootMethod.getReturnType()));
		nodes.put(ie, methodNode);
		// Set<Node> baseNodes = new HashSet<Node>();
		if (ie instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
			for (Node n : getRhsNodes(nodes, iie.getBase())) {
				String label = baseLabel;
				createEdge(n, label, methodNode);
			}

		}
		// else if (ie instanceof StaticInvokeExpr) {
		// baseNodes.add(staticNode);
		// } else if (ie instanceof DynamicInvokeExpr) {
		// System.err.println("DynamicInvoke not implemented. Ignoring " +
		// ie.toString());
		// }

		int argNum = 0;
		for (Set<Node> args : argNodes) {
			for (Node arg : args) {
				String label = ie.hashCode() + "_" + sootMethod.getName() + argLabel + argNum + ")";
				createEdge(arg, label, methodNode, argNum);
				argNum++;
			}
		}
		return methodNode;
	}

	private Node lookupOrCreateNode(Map<Value, Node> nodeMap, Value v) {
		// TODO special treatment for fields.
		if (v instanceof FieldRef) {
			FieldRef ifr = (FieldRef) v;
			if (!fieldMap.containsKey(ifr.getField())) {
				Node node = createVariableNode(ifr.getField().getType(), shortLabelForType(ifr.getField().getType()));
				fieldMap.put(ifr.getField(), node);
			}
			return fieldMap.get(ifr.getField());
		} else  {
			if (!nodeMap.containsKey(v)) {	
				Node node = createVariableNode(v.getType(), shortLabelForValue(v));
				nodeMap.put(v, node);
			}		
		}
		
//		if (shortLabelForValue(v).equals("TestClass") && body.getMethod().getName().contains("fill")) {
////			System.err.println(this.body);
//			System.err.println(v + "  " + v.getClass() + "  "+nodeMap.get(v).getLabel());
//		}

		return nodeMap.get(v);
	}

	
	Map<StringConstant, Integer> constCounter = new HashMap<StringConstant,Integer>();
	private String stringToNumber(StringConstant sc) {
		if (!constCounter.containsKey(sc)) {
			constCounter.put(sc, constCounter.size());
		}
		return constCounter.get(sc).toString();
	}
	
	private String shortLabelForValue(Value v) {
		if (v instanceof StringConstant) {
			return stringToNumber((StringConstant)v);
		} else if (v instanceof ThisRef) {
			return "@this";
		} else if (v instanceof Constant) {
			return v.toString();
		}
		// if (v instanceof AnyNewExpr) {
		// AnyNewExpr an = (AnyNewExpr)v;
		// return shortLabelForType(an.getType());
		// }
		return shortLabelForType(v.getType());
	}

	private String shortLabelForType(Type t) {
		if (t instanceof ArrayType) {
			return shortLabelForType(((ArrayType) t).getArrayElementType()) + "[]";
		} else if (t instanceof RefType) {
			return ((RefType) t).getSootClass().getName();
		}
		return t.toString();
	}

	@Override
	protected void copy(Map<Value, Node> in, Map<Value, Node> out) {
		out.putAll(in);
	}

	@Override
	protected void merge(Map<Value, Node> in1, Map<Value, Node> in2, Map<Value, Node> out) {
		out.putAll(in1);
		for (Entry<Value, Node> entry : in2.entrySet()) {
			if (!out.containsKey(entry.getKey())) {
				out.put(entry.getKey(), entry.getValue());
			}
		}

	}

	@Override
	protected Map<Value, Node> newInitialFlow() {
		return new LinkedHashMap<Value, Node>();
	}

	public void toDot(String filename) {
		File fpw = new File(filename);
		try {
			PrintWriter pw = new PrintWriter(fpw);
			pw.println("digraph dot {");
			for (Node n : this.allNodes) {
				String shape = " shape=oval ";
				if (n instanceof MethodNode) {
					shape = " shape=octagon ";
				}
				// if (((Node)n).isSource) {
				// shape = " shape=doubleoctagon ";
				// } else if (((Node)n).isSink) {
				// shape = " shape=house ";
				// }
				pw.println("\t\"" + n.toString() + "\" " + "[label=\"" + n.getDebugLabel() + "\" " + shape + "];\n");
			}
			pw.append("\n");
			for (Edge e : this.allEdges) {
				// String label = e.label.toString();
				String label = "";
				pw.append("\t\"" + e.source.toString() + "\" -> \"" + e.sink.toString() + "\" " + "[label=\"" + label
						+ "\"];\n");
			}

			pw.println("}");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
