package com.ensoftcorp.open.slice.smart;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.slice.analysis.ForwardDominanceTree;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;

public class ForwardDominanceTreeSmartView extends FilteringAtlasSmartViewScript implements IResizableScript {

	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{XCSG.ControlFlow_Node};
	}
	
	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}

	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
		Q filteredSelection = filter(event.getSelection());

		if(filteredSelection.eval().nodes().isEmpty()){
			return null;
		}
		
		GraphElement selection = filteredSelection.eval().nodes().getFirst();
		
		// convert the data flow node selection to a control flow node
//		if(!Common.toQ(selection).nodesTaggedWithAny(XCSG.DataFlow_Node).eval().nodes().isEmpty()){
//			selection = Common.toQ(selection).containers().nodesTaggedWithAny(XCSG.ControlFlow_Node).eval().nodes().getFirst();
//		}
		
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(selection), Color.CYAN);
		
		GraphElement method = StandardQueries.getContainingMethod(selection);
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
		Q completeResult = Common.toQ(new ForwardDominanceTree(cfg.eval()).getForwardDominanceTree()).union(Common.toQ(selection));

		// compute what to show for current steps
		Q f = Common.toQ(selection).forwardStepOn(completeResult, forward);
		Q r = Common.toQ(selection).reverseStepOn(completeResult, reverse);
		Q result = f.union(r);
		
		// compute what is on the frontier
		Q frontierForward = Common.toQ(selection).forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		
		Q frontierReverse = Common.toQ(selection).reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(h));
	}
	
	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}

	@Override
	public String getTitle() {
		return "Post Dominance Tree";
	}

	@Override
	public int getDefaultStepBottom() {
		return 1;
	}

	@Override
	public int getDefaultStepTop() {
		return 1;
	}

}