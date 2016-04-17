package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.ControlDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;

public class CDGReverseSliceSmartView extends SliceSmartView {
	
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
		return "CDG Reverse Slice";
	}

	@Override
	protected Q getSlice(GraphElement selection) {
		// convert the data flow node selection to a control flow node
//		if(!Common.toQ(selection).nodesTaggedWithAny(XCSG.DataFlow_Node).eval().nodes().isEmpty()){
//			selection = Common.toQ(selection).containers().nodesTaggedWithAny(XCSG.ControlFlow_Node).eval().nodes().getFirst();
//		}
		
		GraphElement method = StandardQueries.getContainingMethod(selection);
		
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		ControlDependenceGraph cdg = new ControlDependenceGraph(cfg.eval());
		return cdg.getSlice(selection, SliceDirection.REVERSE);
	}

}