package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;

public class PDGForwardSliceSmartView extends SliceSmartView {
	
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
		return "PDG Forward Slice";
	}

	@Override
	protected Q getSlice(GraphElement selection) {
		GraphElement method = StandardQueries.getContainingMethod(selection);
		
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		Q dfg = Common.toQ(method).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).induce(dataFlowEdges);
		
		ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.eval(), dfg.eval());
		return pdg.getSlice(selection, SliceDirection.FORWARD);
	}

}