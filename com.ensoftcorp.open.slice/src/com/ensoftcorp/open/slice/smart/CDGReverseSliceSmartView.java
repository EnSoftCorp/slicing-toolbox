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
		GraphElement method = StandardQueries.getContainingMethod(selection);
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		ControlDependenceGraph cdg = new ControlDependenceGraph(cfg.eval());
		return cdg.getSlice(selection, SliceDirection.REVERSE);
	}

}