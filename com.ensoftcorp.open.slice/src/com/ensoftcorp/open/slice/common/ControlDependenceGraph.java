package com.ensoftcorp.open.slice.common;

import java.util.LinkedList;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.slice.analysis.ForwardDominanceTree;

public class ControlDependenceGraph {

	private Graph cfg;
	private Graph fdt;
	
	public ControlDependenceGraph(Graph cfg){
		this.cfg = cfg;
		this.fdt = new ForwardDominanceTree(cfg).getForwardDominanceTree();
	}
	
	public Q getControlDependenceGraph(){
		Q result = Common.toQ(cfg).union(Common.toQ(fdt));
		return result;
	}
	
	public Q getControlDependenceGraphSlice(GraphElement controlFlowNode, boolean reverse){
		AtlasSet<GraphElement> slice = new AtlasHashSet<GraphElement>();

		if(reverse){
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
		} else {
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
