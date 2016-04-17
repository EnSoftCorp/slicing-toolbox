package com.ensoftcorp.open.slice.analysis;

import java.util.LinkedList;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;

public class ControlDependenceGraph extends DependenceGraph {

	private Graph cfg;
	private Graph fdt;
	
	public ControlDependenceGraph(Graph cfg){
		this.cfg = cfg;
		this.fdt = new ForwardDominanceTree(cfg).getForwardDominanceTree();
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
