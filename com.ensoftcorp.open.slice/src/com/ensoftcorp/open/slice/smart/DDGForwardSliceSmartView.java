package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph;

public class DDGForwardSliceSmartView extends SliceSmartView {

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
		return "DDG Forward Slice";
	}
	
	@Override
	protected Q getSlice(GraphElement selection) {
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		GraphElement method = StandardQueries.getContainingMethod(selection);
		Q dfg = Common.toQ(method).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).induce(dataFlowEdges);
		DataDependenceGraph ddg = new DataDependenceGraph(dfg.eval());
		return ddg.getSlice(selection, SliceDirection.FORWARD);
	}

}