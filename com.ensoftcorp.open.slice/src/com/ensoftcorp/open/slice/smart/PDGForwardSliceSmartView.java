package com.ensoftcorp.open.slice.smart;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;

public class PDGForwardSliceSmartView extends SliceSmartView {
	
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
		return "PDG Forward Slice";
	}

	@Override
	protected Q getSlice(GraphElement dataFlowNode, GraphElement method) {
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		Q dfg = Common.toQ(method).contained().induce(dataFlowEdges);
		
		ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.eval(), dfg.eval());
		return pdg.getSlice(dataFlowNode, SliceDirection.FORWARD);
	}
	
	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
		Q filteredSelection = filter(event.getSelection());

		if(filteredSelection.eval().nodes().size() != 1){
			return null;
		}
		
		GraphElement dataFlowNode = filteredSelection.eval().nodes().getFirst();
		GraphElement method = StandardQueries.getContainingMethod(dataFlowNode);
		
		Q completeResult = getSlice(dataFlowNode, method);
		
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(dataFlowNode), Color.CYAN); 

		// compute what to show for current steps
		Q controlFlowNode = Common.toQ(dataFlowNode).containers().nodesTaggedWithAny(XCSG.ControlFlow_Node);
		Q origins = Common.toQ(dataFlowNode).union(controlFlowNode);
		Q f = origins.forwardStepOn(completeResult, forward);
		Q r = origins.reverseStepOn(completeResult, reverse);
		Q result = f.union(r);
		
		// compute what is on the frontier
		Q frontierForward = origins.forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		
		Q frontierReverse = origins.reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(h));
	}

}