package com.ensoftcorp.open.slice.smart;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;

public abstract class SliceSmartView extends FilteringAtlasSmartViewScript implements IResizableScript {

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
		Q completeResult = getSlice(selection);
		
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(selection), Color.CYAN); 

		// compute what to show for current steps
		Q f = filteredSelection.forwardStepOn(completeResult, forward);
		Q r = filteredSelection.reverseStepOn(completeResult, reverse);
		Q result = f.union(r);
		
		// compute what is on the frontier
		Q frontierForward = filteredSelection.forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		
		Q frontierReverse = filteredSelection.reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(h));
	}

	protected abstract Q getSlice(GraphElement selection);
	
	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}

}