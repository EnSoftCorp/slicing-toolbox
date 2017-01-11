package com.ensoftcorp.open.slice.analysis;

import java.util.Map.Entry;
import java.util.Set;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DominanceAnalysis.Multimap;

public class ForwardDominanceTree implements UniqueEntryExitGraph {

	/**
	 * Used to tag the edges that immediately forward dominate (post-dominate) a node
	 */
	public static final String IMMEDIATE_FORWARD_DOMINANCE_EDGE = "ifdom";
	
	/**
	 * The set of nodes in the current graph
	 */
	private AtlasSet<Node> nodes;
	
	/**
	 * The set of edges in the current graph
	 */
	private AtlasSet<Edge> edges;

	private String[] entryNodeTags = { XCSG.controlFlowRoot };

	private String[] exitNodeTags = { XCSG.controlFlowExitPoint }; // may have multiple exit points...
	
	/** 
	 * @param cfg a ControlFlowGraph (may include ExceptionalControlFlow_Edges)
	 */
	public ForwardDominanceTree(Graph cfg) {
		this.nodes = new AtlasHashSet<Node>();
		this.nodes().addAll(cfg.nodes());
		this.edges = new AtlasHashSet<Edge>();
		this.edges().addAll(cfg.edges());
	}
	
	public ForwardDominanceTree(Graph cfg, String[] entryNodeTags, String[] exitNodeTags) {
		this.nodes = new AtlasHashSet<Node>();
		this.nodes().addAll(cfg.nodes());
		this.edges = new AtlasHashSet<Edge>();
		this.edges().addAll(cfg.edges());
		
		this.entryNodeTags = entryNodeTags;
		this.exitNodeTags = exitNodeTags;
	}
	
	public Graph getForwardDominanceTree(){
		DominanceAnalysis dominanceAnalysis = new DominanceAnalysis(this, true);
		Multimap<Node> dominanceFrontier = dominanceAnalysis.getDominatorTree();
		AtlasSet<Node> dominanceTree = new AtlasHashSet<Node>();
		for(Entry<Node, Set<Node>> entry : dominanceFrontier.entrySet()){
			Node fromNode = entry.getKey();
			for(Node toNode : entry.getValue()){
				Q forwardDominanceEdges = Common.universe().edgesTaggedWithAny(IMMEDIATE_FORWARD_DOMINANCE_EDGE);
				Edge forwardDominanceEdge = forwardDominanceEdges.betweenStep(Common.toQ(fromNode), Common.toQ(toNode)).eval().edges().getFirst();
				if(forwardDominanceEdge == null){
					forwardDominanceEdge = Graph.U.createEdge(fromNode, toNode);
					forwardDominanceEdge.tag(IMMEDIATE_FORWARD_DOMINANCE_EDGE);
					forwardDominanceEdge.putAttr(XCSG.name, IMMEDIATE_FORWARD_DOMINANCE_EDGE);
				}
				dominanceTree.add(forwardDominanceEdge);
			}
		}
		return Common.toQ(dominanceTree).eval();
	}

	/**
	 * Gets the predecessors of a given node
	 * @param node
	 * @return Predecessors of node
	 */
	@Override
	public AtlasSet<Node> getPredecessors(Node node){
		AtlasSet<Node> predecessors = new AtlasHashSet<Node>();
		for(Edge edge : this.edges()){
			if(edge.getNode(EdgeDirection.TO).equals(node)){
				Node parent = edge.getNode(EdgeDirection.FROM);
				predecessors.add(parent);
			}
		}
		return predecessors;
	}

	/**
	 * Gets the successors of a given node 
	 * @param node
	 * @return Successors of node
	 */
	@Override
	public AtlasSet<Node> getSuccessors(Node node){		
		AtlasSet<Node> successors = new AtlasHashSet<Node>();
		for(Edge edge : this.edges()){
			if(edge.getNode(EdgeDirection.FROM).equals(node)){
				Node child = edge.getNode(EdgeDirection.TO);
				successors.add(child);
			}
		}
		return successors;
	}

	@Override
	public Node getEntryNode() {
		return this.nodes().taggedWithAny(entryNodeTags).getFirst();
	}

	@Override
	public Node getExitNode() {
		return this.nodes().taggedWithAll(exitNodeTags).getFirst();
	}

	@Override
	public AtlasSet<Node> nodes() {
		return nodes;
	}

	@Override
	public AtlasSet<Edge> edges() {
		return edges;
	}

}
