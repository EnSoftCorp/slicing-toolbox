package com.ensoftcorp.open.slice.analysis;

import java.util.Map.Entry;
import java.util.Set;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DominanceAnalysis.Multimap;

public class ForwardDominanceTree implements UniqueEntryExitGraph {

	private static final String FORWARD_DOMINANCE_EDGE = "ifdom";
	
	/**
	 * The set of nodes in the current graph
	 */
	private AtlasSet<GraphElement> nodes;
	
	/**
	 * The set of edges in the current graph
	 */
	private AtlasSet<GraphElement> edges;
	
	/** 
	 * @param cfg a ControlFlowGraph (may include ExceptionalControlFlow_Edges)
	 */
	public ForwardDominanceTree(Graph cfg) {
		this.nodes = new AtlasHashSet<GraphElement>();
		this.nodes().addAll(cfg.nodes());
		this.edges = new AtlasHashSet<GraphElement>();
		this.edges().addAll(cfg.edges());
	}
	
	public Graph getForwardDominanceTree(){
		DominanceAnalysis dominanceAnalysis = new DominanceAnalysis(this, true);
		Multimap<GraphElement> dominanceFrontier = dominanceAnalysis.getDominanceFrontiers();
		AtlasSet<GraphElement> dominanceTree = new AtlasHashSet<GraphElement>();
		for(Entry<GraphElement, Set<GraphElement>> entry : dominanceFrontier.entrySet()){
			GraphElement toNode = entry.getKey();
			for(GraphElement fromNode : entry.getValue()){
				Q forwardDominanceEdges = Common.universe().edgesTaggedWithAny(FORWARD_DOMINANCE_EDGE);
				GraphElement forwardDominanceEdge = forwardDominanceEdges.betweenStep(Common.toQ(toNode), Common.toQ(fromNode)).eval().edges().getFirst();
				if(forwardDominanceEdge == null){
					forwardDominanceEdge = Graph.U.createEdge(fromNode, toNode);
					forwardDominanceEdge.tag(FORWARD_DOMINANCE_EDGE);
					forwardDominanceEdge.putAttr(XCSG.name, FORWARD_DOMINANCE_EDGE);
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
	public AtlasSet<GraphElement> getPredecessors(GraphElement node){
		AtlasSet<GraphElement> predecessors = new AtlasHashSet<GraphElement>();
		for(GraphElement edge : this.edges()){
			if(edge.getNode(EdgeDirection.TO).equals(node)){
				GraphElement parent = edge.getNode(EdgeDirection.FROM);
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
	public AtlasSet<GraphElement> getSuccessors(GraphElement node){		
		AtlasSet<GraphElement> successors = new AtlasHashSet<GraphElement>();
		for(GraphElement edge : this.edges()){
			if(edge.getNode(EdgeDirection.FROM).equals(node)){
				GraphElement child = edge.getNode(EdgeDirection.TO);
				successors.add(child);
			}
		}
		return successors;
	}

	@Override
	public GraphElement getEntryNode() {
		return this.nodes().taggedWithAll(XCSG.controlFlowRoot).getFirst();
	}

	@Override
	public GraphElement getExitNode() {
		return this.nodes().taggedWithAll(XCSG.controlFlowExitPoint).getFirst();
	}

	@Override
	public AtlasSet<GraphElement> nodes() {
		return nodes;
	}

	@Override
	public AtlasSet<GraphElement> edges() {
		return edges;
	}

}
