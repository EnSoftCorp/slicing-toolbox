package com.ensoftcorp.open.slice.analysis;

import java.util.LinkedList;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

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

	private GraphElement master;
	private GraphElement entry;
	private GraphElement exit;
	
	private static final String CFG_ENTRY = "cfg-entry";
	private static final String CFG_EXIT = "cfg-exit";
	
	private static final String AUGMENTATION_NAME = "augmentation";
	private static final String AUGMENTATION_NODE = "augmentation-node";
	private static final String AUGMENTATION_EDGE = "augmentation-edge";
	
	private Graph cfg;
	private Graph augmentedCFG;
	private Graph fdt;
	
	public ControlDependenceGraph(Graph cfg){
		cfg = Common.toQ(cfg).edgesTaggedWithAny(XCSG.ControlFlow_Edge).retainEdges().eval();
		this.cfg = cfg;
		
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
		
		// reference: http://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis4.pdf
		// given the CFG, Y is control dependent on X iff 
		// 1) there exists a path P from X to Y in the CFG with any Z (excluding X,Y) in P post-dominated by Y
		// 2) X is not post-dominated by Y
		
		// There are two edges out of X
		// traversing one edge always leads to Y,
		// traversing the other edge the other may not lead to Y
		
	}
	
	public Q getAugmentedControlFlowGraph(){
		return Common.toQ(augmentedCFG);
	}
	
	public Q getAugmentedForwardDominanceTree(){
		return Common.toQ(fdt);
	}
	
	public Q getGraph(){
		Q result = Common.toQ(cfg).union(Common.toQ(fdt));
		return result;
	}
	
	@Override
	public Q getSlice(GraphElement controlFlowNode, SliceDirection direction) {
		AtlasSet<GraphElement> slice = new AtlasHashSet<GraphElement>();

		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			// first compute the reverse slice
			LinkedList<GraphElement> reverseSliceQueue = new LinkedList<GraphElement>();
			reverseSliceQueue.add(controlFlowNode);
			// Y is control dependent on X iff there is a path in the CFG
			// from X to Y that doesn't contain the immediate forward 
			// dominator of X
			while(!reverseSliceQueue.isEmpty()){
				GraphElement y = reverseSliceQueue.removeFirst();
				for(GraphElement x : Common.toQ(cfg).predecessors(Common.toQ(y)).eval().nodes()){
					Q fdx = Common.toQ(fdt).successors(Common.toQ(x));
					Q cfgPathsXToY = Common.toQ(cfg).difference(fdx).betweenStep(Common.toQ(x), Common.toQ(y));
					if(slice.addAll(cfgPathsXToY.eval().edges())){
						reverseSliceQueue.add(x);
					}
				}
			}
		} 
		
		else if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			// second compute the forward slice (which is just the reverse slice backwards...)
			LinkedList<GraphElement> forwardSliceQueue = new LinkedList<GraphElement>();
			forwardSliceQueue.add(controlFlowNode);
			while(!forwardSliceQueue.isEmpty()){
				GraphElement x = forwardSliceQueue.removeFirst();
				for(GraphElement y : Common.toQ(cfg).successors(Common.toQ(x)).eval().nodes()){
					Q fdx = Common.toQ(fdt).predecessors(Common.toQ(y));
					Q cfgPathsXToY = Common.toQ(cfg).difference(fdx).betweenStep(Common.toQ(x), Common.toQ(y));
					if(slice.addAll(cfgPathsXToY.eval().edges())){
						forwardSliceQueue.add(y);
					}
				}
			}
		}
		
		return Common.toQ(slice).union(Common.toQ(controlFlowNode));
	}
	
}
