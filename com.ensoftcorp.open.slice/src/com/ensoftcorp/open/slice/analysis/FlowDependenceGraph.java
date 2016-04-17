package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;

public class FlowDependenceGraph extends DependenceGraph {

//	private Graph dfg;
	private Graph fdg;
	
	
	public FlowDependenceGraph(Graph dfg){
//		this.dfg = dfg;
		
		// note: Atlas already provides this because of its SSA form graph
		this.fdg = dfg;
	}

	@Override
	public Q getGraph() {
		return Common.toQ(fdg);
	}

	@Override
	public Q getSlice(GraphElement dataFlowNode, SliceDirection direction) {
		Q result = Common.empty();
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			result = result.union(Common.toQ(fdg).reverse(Common.toQ(dataFlowNode)));
		} else if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			result = result.union(Common.toQ(fdg).forward(Common.toQ(dataFlowNode)));
		}
		return result;
	}
	
}
