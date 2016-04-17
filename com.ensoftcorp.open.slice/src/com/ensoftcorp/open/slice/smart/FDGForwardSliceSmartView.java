package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.slice.analysis.FlowDependenceGraph;

public class FDGForwardSliceSmartView extends SliceSmartView {
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{XCSG.DataFlow_Node};
	}

	@Override
	public int getDefaultStepTop() {
		return 0;
	}
	
	@Override
	public int getDefaultStepBottom() {
		return 1;
	}

	@Override
	public String getTitle() {
		return "FDG Forward Slice";
	}
	
	@Override
	protected Q getSlice(GraphElement selection, GraphElement method) {
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		Q dfg = Common.toQ(method).contained().induce(dataFlowEdges);
		FlowDependenceGraph fdg = new FlowDependenceGraph(dfg.eval());
		return fdg.getSlice(selection, SliceDirection.FORWARD);
	}

}