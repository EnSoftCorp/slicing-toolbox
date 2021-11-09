package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.algorithms.DominanceAnalysis;
import com.ensoftcorp.open.commons.algorithms.UniqueEntryExitCustomGraph;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.slice.log.Log;
import com.ensoftcorp.open.slice.xcsg.AnalysisXCSG;

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
public class FOWControlDependenceGraph extends DependenceGraph {

	private Graph cfg;
	private Graph augmentedCFG;
	private Graph fdt;
	private Graph cdg;

	private boolean purgeAugmentations;

	public FOWControlDependenceGraph(Graph cfg){
		this(cfg, true);
	}

	public FOWControlDependenceGraph(Graph cfg, boolean purgeAugmentations){
		this.purgeAugmentations = purgeAugmentations;
		this.cdg = Common.toQ(cfg).eval();
		this.cfg = Common.toQ(cfg).eval();
		this.fdt = Common.empty().eval();
		this.augmentedCFG = Common.toQ(cfg).eval();
	}

	public void create() {
		
		// sanity checks
		if(cfg.nodes().isEmpty() || cfg.edges().isEmpty()){
			return;
		}

		// augment the cfg with a master entry node and a master exit node
		Node cfRoot = Common.toQ(cfg).nodes(XCSG.controlFlowRoot).eval().nodes().one();
		AtlasSet<Node> cfExits = Common.toQ(cfg).nodes(XCSG.controlFlowExitPoint).eval().nodes();

		Q augmentationEdges = Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE);

		// augment the control flow root
		Node master;
		Node entry;
		Node exit;
		entry = augmentationEdges.predecessors(Common.toQ(cfRoot)).eval().nodes().one();
		if(entry == null){
			entry = Graph.U.createNode();
			entry.tag(AnalysisXCSG.AUGMENTATION_NODE);
			entry.tag(AnalysisXCSG.AUGMENTED_CFG_ENTRY);
			entry.putAttr(XCSG.name, "entry");

			Edge augmentationEdge = Graph.U.createEdge(entry, cfRoot);
			augmentationEdge.tag(AnalysisXCSG.AUGMENTATION_EDGE);
			augmentationEdge.putAttr(XCSG.name, AnalysisXCSG.AUGMENTATION_NAME);
			augmentationEdges = Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE);
		}

		// augment the control flow exits
		exit = augmentationEdges.successors(Common.toQ(cfExits)).eval().nodes().one();
		if(exit == null){
			exit = Graph.U.createNode();
			exit.tag(AnalysisXCSG.AUGMENTATION_NODE);
			exit.tag(AnalysisXCSG.AUGMENTED_CFG_EXIT);
			exit.putAttr(XCSG.name, "exit");
			augmentationEdges = Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE);
		}
		for(Node cfExit : cfExits){
			if(augmentationEdges.successors(Common.toQ(cfExit)).eval().nodes().isEmpty()){
				Edge augmentationEdge = Graph.U.createEdge(cfExit, exit);
				augmentationEdge.tag(AnalysisXCSG.AUGMENTATION_EDGE);
				augmentationEdge.putAttr(XCSG.name, AnalysisXCSG.AUGMENTATION_NAME);
				augmentationEdges = Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE);
			}
		}

		// add a master augmentation node with an edge to the start and the exit
		master = augmentationEdges.predecessors(Common.toQ(entry)).intersection(augmentationEdges.predecessors(Common.toQ(exit))).eval().nodes().one();
		if(master == null){
			master = Graph.U.createNode();
			master.tag(AnalysisXCSG.AUGMENTATION_NODE);
			master.putAttr(XCSG.name, AnalysisXCSG.AUGMENTATION_NAME);

			Edge augmentationEntryEdge = Graph.U.createEdge(master, entry);
			augmentationEntryEdge.tag(AnalysisXCSG.AUGMENTATION_EDGE);
			augmentationEntryEdge.putAttr(XCSG.name, AnalysisXCSG.AUGMENTATION_NAME);

			Edge augmentationExitEdge = Graph.U.createEdge(master, exit);
			augmentationExitEdge.tag(AnalysisXCSG.AUGMENTATION_EDGE);
			augmentationExitEdge.putAttr(XCSG.name, AnalysisXCSG.AUGMENTATION_NAME);
			augmentationEdges = Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE);
		}

		augmentedCFG = Common.toQ(cfg)
				.union(augmentationEdges.reverseStep(Common.toQ(cfRoot)))
				.union(augmentationEdges.forwardStep(Common.toQ(cfExits)))
				.union(augmentationEdges.forwardStep(Common.toQ(master)))
				.eval();

		fdt = DominanceAnalysis.computePostDominanceTree(new UniqueEntryExitCustomGraph(augmentedCFG, entry, exit));

		// For each edge (X -> Y) in augmented CFG, 
		// find nodes in the forward dominance tree from Y
		// to the least common ancestor (LCA) of X and Y 
		// (including LCA if LCA is X and excluding LCA if LCA is not X)
		AtlasSet<Edge> controlDependenceEdgeSet = new AtlasHashSet<Edge>();
		for(Edge cfEdge : augmentedCFG.edges()){
			Node x = cfEdge.from();
			Node y = cfEdge.to();

			// least common ancestor in forward dominance tree
			Node lca = CommonQueries.leastCommonAncestor(x, y, fdt);

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
				if(!x.equals(node)) {
					Q controlDependenceEdges = Query.universe().edges(AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
					Edge controlDependenceEdge = controlDependenceEdges.betweenStep(Common.toQ(x), Common.toQ(node)).eval().edges().one();
					if(controlDependenceEdge == null && !x.taggedWith(AnalysisXCSG.AUGMENTATION_NODE) && !node.taggedWith(AnalysisXCSG.AUGMENTATION_NODE)){
						controlDependenceEdge = Graph.U.createEdge(x, node);
						controlDependenceEdge.tag(AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
						controlDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
					}
					controlDependenceEdgeSet.add(controlDependenceEdge);
				}
			}
		}

		this.cdg = Common.toQ(controlDependenceEdgeSet).eval();

		if(purgeAugmentations){
			// purge augmentation edges
			AtlasHashSet<Edge> augmentationEdgesToRemove = new AtlasHashSet<Edge>(Query.universe().edges(AnalysisXCSG.AUGMENTATION_EDGE).eval().edges());
			while(!augmentationEdgesToRemove.isEmpty()){
				Edge edge = augmentationEdgesToRemove.one();
				if(edge != null){
					Graph.U.delete(edge);
					augmentationEdgesToRemove.remove(edge);
				}
			}
			// purge augmentation nodes
			AtlasHashSet<Node> augmentationNodesToRemove = new AtlasHashSet<Node>(Query.universe().nodes(AnalysisXCSG.AUGMENTATION_NODE).eval().nodes());
			while(!augmentationNodesToRemove.isEmpty()){
				Node node = augmentationNodesToRemove.one();
				if(node != null){
					Graph.U.delete(node);
					augmentationNodesToRemove.remove(node);
				}
			}
		}
	}

	public Q getControlFlowGraph(){
		return Common.toQ(cfg);
	}

	public Q getForwardDominanceTree(){
		return Common.toQ(fdt);
	}

	public Q getAugmentedControlFlowGraph() {
		if(purgeAugmentations) {
			throw new IllegalArgumentException("Augmented CFG has been purged.");
		}
		return Common.toQ(augmentedCFG);
	}

	public Q getGraph(){
		return Common.toQ(cdg);
	}

}
