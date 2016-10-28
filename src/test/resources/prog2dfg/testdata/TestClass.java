/**
 * 
 */
package prog2dfg.testdata;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author schaef
 *
 */
class Body {}
class Vector2{
	public int x, y;
	public void add(Vector2 v) {x+=v.x; y+=v.y;};
}

public class TestClass {

	ArrayList<Vector2> forces = new ArrayList<Vector2>(1);
	boolean sleep = true;
	float mass;
	
	public TestClass applyForce(Vector2 force) {
		// check for null
		if (force == null) throw new NullPointerException("dynamics.body.nullForce");
		// check the linear mass of the body
		if (this.mass== 0.0) {
			// this means that applying a force will do nothing
			// so, just return
			return this;
		}
		// apply the force
		this.forces.add(force);
		// wake up the body
		sleep = false;
		// return this body to facilitate chaining
		return this;
	}
	
//	boolean[] primes = new boolean[10000];
//
//
//	public void fillSieve() {
//		boolean[] primes = this.primes;
//		Arrays.fill(primes, true); 
//		primes[0] = primes[1] = false;
//		for (int i = 2; i < primes.length; i++) {
//			if (primes[i]) {
//				System.out.println(i);
//				for (int j = 2; i * j < primes.length; j++) {
//					primes[i * j] = false;
//				}
//			}
//		}
//	}

	public void reverseFile(String infile, String outfile) throws IOException {
		String data = readFile(infile);
		data = new StringBuilder(data).reverse().toString();
		writeFile(outfile, data);
	}

	public String readFile(String fname) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(fname));
		return new String(encoded, Charset.defaultCharset());
	}

	public void writeFile(String fname, String data) {
		try (Writer out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fname), Charset.defaultCharset()));) {
			out.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public enum State {
		Visited, Unvisited;
	}

	public abstract class Node {
		public State state = null;
		public List<Node> vertices;
		public int value;

		public abstract List<Node> getChild();

		public abstract String getVertex();
	}

	/*
	 * Taken from
	 * http://codereview.stackexchange.com/questions/48518/depth-first
	 * -search-breadth-first-search-implementation
	 */
	public void bfs01(Node root) {
		// Since queue is a interface
		Queue<Node> queue = new LinkedList<Node>();

		if (root == null)
			return;

		root.state = State.Visited;
		// Adds to end of queue
		queue.add(root);

		while (!queue.isEmpty()) {
			// removes from front of queue
			Node r = queue.remove();
			System.out.print(r.getVertex() + "\t");

			// Visit child first before grandchild
			for (Node n : r.getChild()) {
				if (n.state == State.Unvisited) {
					queue.add(n);
					n.state = State.Visited;
				}
			}
		}
	}

	/*
	 * Taken from
	 * http://www.sanfoundry.com/java-program-traverse-graph-using-bfs/
	 */
	public void bfs02(int adjacency_matrix[][], int source) {
		Queue<Integer> queue = new LinkedList<Integer>();
		int number_of_nodes = adjacency_matrix[source].length - 1;

		int[] visited = new int[number_of_nodes + 1];
		int i, element;

		visited[source] = 1;
		queue.add(source);

		while (!queue.isEmpty()) {
			element = queue.remove();
			i = element;
			System.out.print(i + "\t");
			while (i <= number_of_nodes) {
				if (adjacency_matrix[element][i] == 1 && visited[i] == 0) {
					queue.add(i);
					visited[i] = 1;
				}
				i++;
			}
		}
	}

	/*
	 * http://www.mathcs.emory.edu/~cheung/Courses/323/Syllabus/Graph/Progs/bfs/
	 * Graph1.java
	 */
	public void bfs03(int[][] adjMatrix, int rootNode, int NNodes, boolean[] visited) {

		Queue<Integer> q = new LinkedList<Integer>();

		q.add(rootNode);
		visited[rootNode] = true;

		printNode(rootNode);

		while (!q.isEmpty()) {
			int n, child;

			n = (q.peek()).intValue();

			child = getUnvisitedChildNode(n, adjMatrix, NNodes, visited); // Returns
																			// -1
																			// if
																			// no
																			// unvisited
			// niode left

			if (child != -1) { // Found an unvisted node

				visited[child] = true; // Mark as visited

				printNode(child);

				q.add(child); // Add to queue
			} else {
				q.remove(); // Process next node
			}
		}
	}

	int getUnvisitedChildNode(int n, int[][] adjMatrix, int NNodes, boolean[] visited) {
		int j;
		for (j = 0; j < NNodes; j++) {
			if (adjMatrix[n][j] > 0) {
				if (!visited[j])
					return (j);
			}
		}
		return (-1);
	}

	void printNode(int node) {

	}

	/*
	 * Adopted implementation from:
	 * http://codefordummies.blogspot.com/2013/11/bfs-breadth-first-search-for.
	 * html
	 */
	public Node bfs04(Node root, int element) {
		// #1: Initialize queue (q)
		Queue<Node> q = new ConcurrentLinkedQueue<Node>(); // some queue
		// implementation
		// #2: Push root node to queue
		q.add(root);

		// #3: While queue not empty
		while (!q.isEmpty()) {

			// #:4 Dequeue n
			Node n = q.poll();
			// visit this node
			n.state = State.Visited;

			// #5: If n == required_node, return n;
			if (n.value == element)
				return n;

			// #5: foreach vertices v of n
			for (Node v : n.vertices) {
				// #6: if v is visited, continue
				if (v.state == State.Visited)
					continue;
				// #7: else enque v
				q.add(v);
			}
		}
		// #8: return null;
		return null; // cannot find element
	}
}
