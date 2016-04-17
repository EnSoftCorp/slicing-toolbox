package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.slice.analysis.FlowDependenceGraph;

public class FDGReverseSliceSmartView extends SliceSmartView {
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{XCSG.DataFlow_Node};
	}

	@Override
	public int getDefaultStepTop() {
		return 1;
	}
	
	@Override
	public int getDefaultStepBottom() {
		return 0;
	}

	@Override
	public String getTitle() {
		return "FDG Reverse Slice";
	}
	
	@Override
	protected Q getSlice(GraphElement dataFlowNode, GraphElement method) {
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		Q dfg = Common.toQ(method).contained().induce(dataFlowEdges);
		FlowDependenceGraph fdg = new FlowDependenceGraph(dfg.eval());
		return fdg.getSlice(dataFlowNode, SliceDirection.REVERSE);
	}

}