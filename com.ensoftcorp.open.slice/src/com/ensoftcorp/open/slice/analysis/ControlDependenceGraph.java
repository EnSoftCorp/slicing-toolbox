package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.java.core.script.CommonQueries;

/**
 * 
 * Constructs the Control Dependence Graph (CGD) from a given Control Flow Graph (CFG)
 * 
 * Computed using the Ferrante, Ottenstein, and Warren algorithm
 * 
 * Reference: 
 * 1) http://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis4.pdf
 * 2) http://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis5.pdf
 * 
 * @author Ben Holland
 */
public class ControlDependenceGraph extends DependenceGraph {

	/**
	 * Used to tag the edges between nodes that contain a control dependence
	 */
	public static final String CONTROL_DEPENDENCE_EDGE = "control-dependence";
	
	private GraphElement master;
	private GraphElement entry;
	private GraphElement exit;
	
	private static final String CFG_ENTRY = "cfg-entry";
	private static final String CFG_EXIT = "cfg-exit";
	
	private static final String AUGMENTATION_NAME = "augmentation";
	private static final String AUGMENTATION_NODE = "augmentation-node";
	private static final String AUGMENTATION_EDGE = "augmentation-edge";
	
	private Graph augmentedCFG;
	private Graph fdt;
	private Graph cdg;
	
	public ControlDependenceGraph(Graph cfg){
		cfg = Common.toQ(cfg).edgesTaggedWithAny(XCSG.ControlFlow_Edge).retainEdges().eval();

		// augment the cfg with a master entry node and a master exit node
		GraphElement cfRoot = Common.toQ(cfg).nodesTaggedWithAny(XCSG.controlFlowRoot).eval().nodes().getFirst();
		AtlasSet<GraphElement> cfExits = Common.toQ(cfg).nodesTaggedWithAny(XCSG.controlFlowExitPoint).eval().nodes();
		
		Q augmentationEdges = Common.universe().edgesTaggedWithAny(AUGMENTATION_EDGE);
		
		// augment the control flow root
		entry = augmentationEdges.predecessors(Common.toQ(cfRoot)).eval().nodes().getFirst();
		if(entry == null){
			entry = Graph.U.createNode();
			entry.tag(AUGMENTATION_NODE);
			entry.tag(CFG_ENTRY);
			entry.putAttr(XCSG.name, "entry");
			
			GraphElement augmentationEdge = Graph.U.createEdge(entry, cfRoot);
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
		for(GraphElement cfExit : cfExits){
			if(augmentationEdges.successors(Common.toQ(cfExit)).eval().nodes().isEmpty()){
				GraphElement augmentationEdge = Graph.U.createEdge(cfExit, exit);
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
			
			GraphElement augmentationEntryEdge = Graph.U.createEdge(master, entry);
			augmentationEntryEdge.tag(AUGMENTATION_EDGE);
			augmentationEntryEdge.putAttr(XCSG.name, AUGMENTATION_NAME);
			
			GraphElement augmentationExitEdge = Graph.U.createEdge(master, exit);
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
		
		// for each edge (X->Y) in the augmented CFG
		// Y is control dependent on X iff there is a path in the CFG
		// from X to Y that doesn't contain the immediate forward 
		// dominator of X
		AtlasSet<GraphElement> controlDependenceEdgeSet = new AtlasHashSet<GraphElement>();
		for(GraphElement cfEdge : augmentedCFG.edges()){
			GraphElement x = cfEdge.getNode(EdgeDirection.FROM);
			GraphElement y = cfEdge.getNode(EdgeDirection.TO);
			
			// get forward dominator of X
			Q fdx = Common.toQ(fdt).successors(Common.toQ(x));
			// paths from X to Y that don't contain immediate forward dominator of x
			Q paths = Common.toQ(augmentedCFG).difference(fdx).between(Common.toQ(x), Common.toQ(y));
			if(!CommonQueries.isEmpty(paths)){
				// Y is control dependent on X
				Q controlDependenceEdges = Common.universe().edgesTaggedWithAny(CONTROL_DEPENDENCE_EDGE);
				GraphElement controlDependenceEdge = controlDependenceEdges.betweenStep(Common.toQ(y), Common.toQ(x)).eval().edges().getFirst();
				if(controlDependenceEdge == null){
					controlDependenceEdge = Graph.U.createEdge(y, x);
					controlDependenceEdge.tag(CONTROL_DEPENDENCE_EDGE);
					controlDependenceEdge.putAttr(XCSG.name, CONTROL_DEPENDENCE_EDGE);
				}
				controlDependenceEdgeSet.add(controlDependenceEdge);
			}
		}
		
		this.cdg = Common.toGraph(controlDependenceEdgeSet);
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
		return Common.toQ(cdg);
	}
	
	@Override
	public Q getSlice(GraphElement controlFlowNode, SliceDirection direction) {
		Q controlDependenceEdges = Common.universe().edgesTaggedWithAny(CONTROL_DEPENDENCE_EDGE);
		Q slice = Common.empty();
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(controlDependenceEdges.reverse(Common.toQ(controlFlowNode)));
		} 
		if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(controlDependenceEdges.forward(Common.toQ(controlFlowNode)));
		}
		return slice;
	}
	
}
