/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
// Adapted from: https://svn.apache.org/repos/asf/flex/falcon/trunk/compiler/src/org/apache/flex/abc/graph/algorithms/DominatorTree.java

package com.ensoftcorp.open.slice.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;

/**
 * An implementation of the O(n log n) Lengauer-Tarjan algorithm for building
 * the <a href="http://en.wikipedia.org/wiki/Dominator_%28graph_theory%29">dominator
 * tree</a> of a {@link ControlFlowGraph cfg}.
 */
public class DominanceAnalysis {
	/**
	 * Control flow graph for dominance computation
	 */
	private UniqueEntryExitGraph cfg;

	/**
	 * To compute dominators or post-dominators
	 */
	private boolean postdom;

	/**
	 * Semidominator numbers by block.
	 */
	private Map<Node, Integer> semi = new HashMap<Node, Integer>();

	/**
	 * Parents by block.
	 */
	private Map<Node, Node> parent = new HashMap<Node, Node>();

	/**
	 * Predecessors by block.
	 */
	private Multimap<Node> pred = new Multimap<Node>();

	/**
	 * Blocks in DFS order; used to look up a block from its semidominator
	 * numbering.
	 */
	private ArrayList<Node> vertex = new ArrayList<Node>();

	/**
	 * Blocks by semidominator block.
	 */
	private Multimap<Node> bucket = new Multimap<Node>();

	/**
	 * idominator map, built iteratively.
	 */
	private Map<Node, Node> idom = new HashMap<Node, Node>();

	/**
	 * Dominance frontiers of this dominator tree, built on demand.
	 */
	private Multimap<Node> dominanceFrontiers = null;

	/**
	 * Dominator tree, built on demand from the idominator map.
	 */
	private Multimap<Node> dominatorTree = null;

	/**
	 * Auxiliary data structure used by the O(m log n) eval/link implementation:
	 * ancestor relationships in the forest (the processed tree as it's built
	 * back up).
	 */
	private Map<Node, Node> ancestor = new HashMap<Node, Node>();

	/**
	 * Auxiliary data structure used by the O(m log n) eval/link implementation:
	 * node with least semidominator seen during traversal of a path from node
	 * to subtree root in the forest.
	 */
	private Map<Node, Node> label = new HashMap<Node, Node>();

	/**
	 * A topological traversal of the dominator tree, built on demand.
	 */
	private LinkedList<Node> topologicalTraversalImpl = null;

	/**
	 * Construct a DominatorTree from a root.
	 * 
	 * @param root
	 *            the root of the graph.
	 */
	public DominanceAnalysis(UniqueEntryExitGraph g, boolean postdom) {
		this.cfg = g;
		this.postdom = postdom;
		if (this.postdom) {
			this.dfs(this.cfg.getExitNode());
		} else {
			this.dfs(this.cfg.getEntryNode());
		}
		this.computeDominators();
	}

	/**
	 * Create and/or fetch the map of immediate dominators.
	 * 
	 * @return the map from each block to its immediate dominator (if it has
	 *         one).
	 */
	public Map<Node, Node> getIdoms() {
		return this.idom;
	}

	/**
	 * Compute and/or fetch the dominator tree as a Multimap.
	 * 
	 * @return the dominator tree.
	 */
	public Multimap<Node> getDominatorTree() {
		if (this.dominatorTree == null) {
			this.dominatorTree = new Multimap<Node>();
			for (Node node : this.idom.keySet()){
				dominatorTree.get(this.idom.get(node)).add(node);
			}
		}
		return this.dominatorTree;
	}

	/**
	 * Compute and/or fetch the dominance frontiers as a Multimap.
	 * 
	 * @return a Multimap where the set of nodes mapped to each key node is the
	 *         set of nodes in the key node's dominance frontier.
	 */
	public Multimap<Node> getDominanceFrontiers() {
		if (this.dominanceFrontiers == null) {
			this.dominanceFrontiers = new Multimap<Node>();

			getDominatorTree(); // touch the dominator tree

			for (Node x : reverseTopologicalTraversal()) {
				Set<Node> dfx = this.dominanceFrontiers.get(x);

				// Compute DF(local)
				for (Node y : getSuccessors(x)){
					if (idom.get(y) != x){
						dfx.add(y);
					}
				}

				// Compute DF(up)
				for (Node z : this.dominatorTree.get(x)){
					for (Node y : this.dominanceFrontiers.get(z)){
						if (idom.get(y) != x){
							dfx.add(y);
						}
					}
				}
			}
		}

		return this.dominanceFrontiers;
	}

	/**
	 * Create and/or fetch a topological traversal of the dominator tree, such
	 * that for every node, idom(node) appears before node.
	 * 
	 * @return the topological traversal of the dominator tree, as an immutable
	 *         List.
	 */
	public List<Node> topologicalTraversal() {
		return Collections.unmodifiableList(getToplogicalTraversalImplementation());
	}

	/**
	 * Create and/or fetch a reverse topological traversal of the dominator
	 * tree, such that for every node, node appears before idom(node).
	 * 
	 * @return a reverse topological traversal of the dominator tree, as an
	 *         immutable List.
	 */
	public Iterable<Node> reverseTopologicalTraversal() {
		return new Iterable<Node>() {
			@Override
			public Iterator<Node> iterator() {
				return getToplogicalTraversalImplementation().descendingIterator();
			}
		};
	}

	/**
	 * Depth-first search the graph and initialize data structures.
	 * 
	 * @param roots
	 *            the root(s) of the flowgraph. One of these is the start block,
	 *            the others are exception handlers.
	 */
	private void dfs(Node entryNode) {
		Iterator<Node> it = new DepthFirstPreorderIterator(this.cfg, entryNode, this.postdom);

		while (it.hasNext()) {
			Node node = it.next();

			if (!semi.containsKey(node)) {
				vertex.add(node);

				// Initial assumption: the node's semidominator is itself.
				semi.put(node, semi.size());
				label.put(node, node);

				for (Node child : getSuccessors(node)) {
					pred.get(child).add(node);
					if (!semi.containsKey(child)) {
						parent.put(child, node);
					}
				}
			}
		}
	}

	/**
	 * Steps 2, 3, and 4 of Lengauer-Tarjan.
	 */
	private void computeDominators() {
		int lastSemiNumber = semi.size() - 1;

		for (int i = lastSemiNumber; i > 0; i--) {
			Node w = vertex.get(i);
			Node p = this.parent.get(w);

			// step 2: compute semidominators
			// for each v in pred(w)...
			int semidominator = semi.get(w);
			for (Node v : pred.get(w)){
				semidominator = Math.min(semidominator, semi.get(eval(v)));
			}

			semi.put(w, semidominator);
			bucket.get(vertex.get(semidominator)).add(w);

			// Link w into the forest via its parent, p
			link(p, w);

			// step 3: implicitly compute idominators
			// for each v in bucket(parent(w)) ...
			for (Node v : bucket.get(p)) {
				Node u = eval(v);
				if (semi.get(u) < semi.get(v)){
					idom.put(v, u);
				} else {
					idom.put(v, p);
				}
			}

			bucket.get(p).clear();
		}

		// step 4: explicitly compute idominators
		for (int i = 1; i <= lastSemiNumber; i++) {
			Node w = vertex.get(i);
			if (idom.get(w) != vertex.get((semi.get(w)))){
				idom.put(w, idom.get(idom.get(w)));
			}
		}
	}

	/**
	 * Extract the node with the least-numbered semidominator in the (processed)
	 * ancestors of the given node.
	 * 
	 * @param v
	 *            - the node of interest.
	 * @return "If v is the root of a tree in the forest, return v. Otherwise,
	 *         let r be the root of the tree which contains v. Return any vertex
	 *         u != r of miniumum semi(u) on the path r-*v."
	 */
	private Node eval(Node v) {
		// This version of Lengauer-Tarjan implements
		// eval(v) as a path-compression procedure.
		compress(v);
		return label.get(v);
	}

	/**
	 * Traverse ancestor pointers back to a subtree root, then propagate the
	 * least semidominator seen along this path through the "label" map.
	 */
	private void compress(Node v) {
		Stack<Node> worklist = new Stack<Node>();
		worklist.add(v);

		Node a = this.ancestor.get(v);

		// Traverse back to the subtree root.
		while (this.ancestor.containsKey(a)) {
			worklist.push(a);
			a = this.ancestor.get(a);
		}

		// Propagate semidominator information forward.
		Node ancestor = worklist.pop();
		int leastSemi = semi.get(label.get(ancestor));

		while (!worklist.empty()) {
			Node descendent = worklist.pop();
			int currentSemi = semi.get(label.get(descendent));

			if (currentSemi > leastSemi){
				label.put(descendent, label.get(ancestor));
			} else {
				leastSemi = currentSemi;
			}

			// Prepare to process the next iteration.
			ancestor = descendent;
		}
	}

	/**
	 * Simple version of link(parent,child) simply links the child into the
	 * parent's forest, with no attempt to balance the subtrees or otherwise
	 * optimize searching.
	 */
	private void link(Node parent, Node child) {
		this.ancestor.put(child, parent);
	}

	/**
	 * Multimap maps a key to a set of values.
	 */
	@SuppressWarnings("serial")
	public static class Multimap<T> extends HashMap<T, Set<T>> {
		/**
		 * Fetch the set for a given key, creating it if necessary.
		 * 
		 * @param key
		 *            - the key.
		 * @return the set of values mapped to the key.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Set<T> get(Object key) {
			if (!this.containsKey(key))
				this.put((T) key, new HashSet<T>());

			return super.get(key);
		}
	}

	/**
	 * Create/fetch the topological traversal of the dominator tree.
	 * 
	 * @return {@link this.topologicalTraversal}, the traversal of the dominator
	 *         tree such that for any node n with a dominator, n appears before
	 *         idom(n).
	 */
	private LinkedList<Node> getToplogicalTraversalImplementation() {
		if (this.topologicalTraversalImpl == null) {
			this.topologicalTraversalImpl = new LinkedList<Node>();
			for (Node node : this.vertex) {
				int idx = this.topologicalTraversalImpl.indexOf(this.idom.get(node));
				if (idx != -1){
					this.topologicalTraversalImpl.add(idx + 1, node);
				} else {
					this.topologicalTraversalImpl.add(node);
				}
			}
		}
		return this.topologicalTraversalImpl;
	}

	private AtlasSet<Node> getSuccessors(Node node) {
		if (postdom) {
			return this.cfg.getPredecessors(node);
		} else {
			return this.cfg.getSuccessors(node);
		}
	}
}