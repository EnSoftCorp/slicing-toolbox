package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.common.ControlDependenceGraph;

public class CDGForwardSliceSmartView extends SliceSmartView {
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{XCSG.ControlFlow_Node};
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
		return "CDG Forward Slice";
	}
	
	@Override
	protected Q getSlice(GraphElement selection, GraphElement method) {
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		ControlDependenceGraph cdg = new ControlDependenceGraph(cfg.eval());

		// if the user selects the method we just show the whole cdg
		Q completeResult = cdg.getControlDependenceGraph(); 
		
		// if the user selects a control flow node, then we show cdg slice for the selected statement
		if(selection.taggedWith(XCSG.ControlFlow_Node)){
			completeResult = cdg.getControlDependenceGraphSlice(selection, false);
		}
		return completeResult;
	}

}