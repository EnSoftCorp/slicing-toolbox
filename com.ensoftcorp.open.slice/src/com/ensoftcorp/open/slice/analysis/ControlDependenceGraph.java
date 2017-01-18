package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.algorithms.ForwardDominanceTree;
import com.ensoftcorp.open.commons.analysis.StandardQueries;

/**
 * 
 * Constructs the Control Dependence Graph (CGD) from a given Control Flow Graph (CFG)
 * 
 * Computed using the Ferrante, Ottenstein, and Warren algorithm
 * 
 * Reference: 
 * 1) http://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis4.pdf
 * 
 * @author Ben Holland
 */
public class ControlDependenceGraph extends DependenceGraph {

	/**
	 * Used to tag the edges between nodes that contain a control dependence
	 */
	public static final String CONTROL_DEPENDENCE_EDGE = "control-dependence";
	
	private Node master;
	private Node entry;
	private Node exit;
	
	private static final String CFG_ENTRY = "cfg-entry";
	private static final String CFG_EXIT = "cfg-exit";
	
	private static final String AUGMENTATION_NAME = "augmentation";
	private static final String AUGMENTATION_NODE = "augmentation-node";
	private static final String AUGMENTATION_EDGE = "augmentation-edge";
	
	private Graph augmentedCFG;
	private Graph fdt;
	private Graph cdg;
	
	public ControlDependenceGraph(Graph cfg){
		// sanity checks
		if(cfg.nodes().isEmpty() || cfg.edges().isEmpty()){
			this.cdg = Common.toQ(cfg).eval();
			this.fdt = Common.empty().eval();
			this.augmentedCFG = Common.toQ(cfg).eval();
			return;
		}
		
		// augment the cfg with a master entry node and a master exit node
		Node cfRoot = Common.toQ(cfg).nodesTaggedWithAny(XCSG.controlFlowRoot).eval().nodes().getFirst();
		AtlasSet<Node> cfExits = Common.toQ(cfg).nodesTaggedWithAny(XCSG.controlFlowExitPoint).eval().nodes();
		
		Q augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
		
		// augment the control flow root
		entry = augmentationEdges.predecessors(Common.toQ(cfRoot)).eval().nodes().getFirst();
		if(entry == null){
			entry = Graph.U.createNode();
			entry.tag(AUGMENTATION_NODE);
			entry.tag(CFG_ENTRY);
			entry.putAttr(XCSG.name, "entry");
			
			Edge augmentationEdge = Graph.U.createEdge(entry, cfRoot);
			augmentationEdge.tag(AUGMENTATION_EDGE);
			augmentationEdge.putAttr(XCSG.name, AUGMENTATION_NAME);
			augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
		}
		
		// augment the control flow exits
		exit = augmentationEdges.successors(Common.toQ(cfExits)).eval().nodes().getFirst();
		if(exit == null){
			exit = Graph.U.createNode();
			exit.tag(AUGMENTATION_NODE);
			exit.tag(CFG_EXIT);
			exit.putAttr(XCSG.name, "exit");
			augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
		}
		for(Node cfExit : cfExits){
			if(augmentationEdges.successors(Common.toQ(cfExit)).eval().nodes().isEmpty()){
				Edge augmentationEdge = Graph.U.createEdge(cfExit, exit);
				augmentationEdge.tag(AUGMENTATION_EDGE);
				augmentationEdge.putAttr(XCSG.name, AUGMENTATION_NAME);
				augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
			}
		}
		
		// add a master augmentation node with an edge to the start and the exit
		master = augmentationEdges.predecessors(Common.toQ(entry)).intersection(augmentationEdges.predecessors(Common.toQ(exit))).eval().nodes().getFirst();
		if(master == null){
			master = Graph.U.createNode();
			master.tag(AUGMENTATION_NODE);
			master.putAttr(XCSG.name, AUGMENTATION_NAME);
			
			Edge augmentationEntryEdge = Graph.U.createEdge(master, entry);
			augmentationEntryEdge.tag(AUGMENTATION_EDGE);
			augmentationEntryEdge.putAttr(XCSG.name, AUGMENTATION_NAME);
			
			Edge augmentationExitEdge = Graph.U.createEdge(master, exit);
			augmentationExitEdge.tag(AUGMENTATION_EDGE);
			augmentationExitEdge.putAttr(XCSG.name, AUGMENTATION_NAME);
			augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
		}
		
		this.augmentedCFG = Common.toQ(cfg)
				.union(augmentationEdges.reverseStep(Common.toQ(cfRoot)))
				.union(augmentationEdges.forwardStep(Common.toQ(cfExits)))
				.union(augmentationEdges.forwardStep(Common.toQ(master)))
				.eval();
		
		String[] entryTags = new String[] { CFG_ENTRY };
		String[] exitTags = new String[] { CFG_EXIT };
		this.fdt = new ForwardDominanceTree(augmentedCFG, entryTags, exitTags).getForwardDominanceTree();
		
		// For each edge (X -> Y) in augmented CFG, 
		// find nodes in the forward dominance tree from Y
		// to the least common ancestor (LCA) of X and Y 
		// (including LCA if LCA is X and excluding LCA if LCA is not X)
		AtlasSet<Edge> controlDependenceEdgeSet = new AtlasHashSet<Edge>();
		for(Edge cfEdge : augmentedCFG.edges()){
			Node x = cfEdge.getNode(EdgeDirection.FROM);
			Node y = cfEdge.getNode(EdgeDirection.TO);
			
			// least common ancestor in forward dominance tree
			Node lca = StandardQueries.leastCommonAncestor(x, y, fdt);
			
			// sanity check
			if(lca == null){
				Log.warning("No common ancestor of " + x.address().toAddressString() + " and " + y.address().toAddressString());
				continue;
			}
			
			// nodes between lca -> Y (nodes dependent on X)
			AtlasSet<Node> nodesControlDependentOnX = new AtlasHashSet<Node>();
			nodesControlDependentOnX.addAll(Common.toQ(fdt).between(Common.toQ(lca), Common.toQ(y)).eval().nodes());
			// remove LCA if LCA is not X
			if(!lca.equals(x)){
				nodesControlDependentOnX.remove(lca);
			}
			
			// add control dependence edges
			for(Node node : nodesControlDependentOnX){
				Q controlDependenceEdges = Common.universe().edgesTaggedWithAny(CONTROL_DEPENDENCE_EDGE);
				Edge controlDependenceEdge = controlDependenceEdges.betweenStep(Common.toQ(x), Common.toQ(node)).eval().edges().getFirst();
				if(controlDependenceEdge == null){
					controlDependenceEdge = Graph.U.createEdge(x, node);
					controlDependenceEdge.tag(CONTROL_DEPENDENCE_EDGE);
					controlDependenceEdge.putAttr(XCSG.name, CONTROL_DEPENDENCE_EDGE);
				}
				controlDependenceEdgeSet.add(controlDependenceEdge);
			}
		}
		
		this.cdg = Common.toQ(controlDependenceEdgeSet).eval();
	}
	
	public Q getControlFlowGraph(){
		return Common.toQ(augmentedCFG).difference(Common.universe().nodesTaggedWithAny(AUGMENTATION_NODE));
	}
	
	public Q getAugmentedControlFlowGraph(){
		return Common.toQ(augmentedCFG);
	}
	
	public Q getForwardDominanceTree(){
		return Common.toQ(fdt);
	}
	
	public Q getGraph(){
		return Common.toQ(cdg).difference(Common.universe().nodesTaggedWithAny(AUGMENTATION_NODE));
	}
	
}
