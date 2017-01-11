package com.ensoftcorp.open.slice.smart;

import java.awt.Color;
import java.io.IOException;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.commons.analysis.StandardQueries;
import com.ensoftcorp.open.commons.utilities.FormattedSourceCorrespondence;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;

public abstract class DependenceSliceSmartView extends FilteringAtlasSmartViewScript implements IResizableScript {

	@Override
	protected String[] getSupportedNodeTags() {
		return new String[] { XCSG.ControlFlow_Node, XCSG.DataFlow_Node };
	}
	
	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}
	
	@Override
	public int getDefaultStepTop() {
		return 1;
	}
	
	@Override
	public int getDefaultStepBottom() {
		return 1;
	}
	
	/*
	 * Converts the slicing criteria to a human readable string
	 * Useful for debugging
	 */
	protected String getSlicingCriteriaSummary(AtlasSet<Node> criteria) throws IOException {
		StringBuilder summary = new StringBuilder();
		String prefix = "";
		for(Node criterion : criteria){
			FormattedSourceCorrespondence fsc = FormattedSourceCorrespondence.getSourceCorrespondent(criterion);
			long startLine = fsc.getStartLineNumber();
			long endLine = fsc.getEndLineNumber(); 
			String lines = startLine == endLine ? ("line " + startLine) : ("lines " + startLine + "-" + endLine);
			summary.append(prefix + criterion.getAttr(XCSG.name).toString() + " (" + lines + " of " + fsc.getFile().getName() + ")");
			prefix = ", ";
		}
		return summary.toString();
	}
	
	protected abstract DependenceGraph getDependenceGraph(Node method);
	
	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
		
		Q filteredSelection = filter(event.getSelection());
		AtlasSet<Node> criteria = DependenceGraph.getStatements(filteredSelection.eval().nodes());
		
		if(criteria.isEmpty()){
			return null; // no applicable selection, do nothing
		}
		
		// debug
//		try {
//			Log.info("Slicing Criteria: " + getSlicingCriteriaSummary(criteria));
//		} catch (Exception e){
//			Log.warning("Error summarizing slicing criteria", e);
//		}
		
		Q completeResult = Common.empty();
		for(Node method : StandardQueries.getContainingFunctions(Common.toQ(criteria)).eval().nodes()){
			Q relevantCriteria = Common.toQ(method).contained().intersection(Common.toQ(criteria));
			DependenceGraph dg = getDependenceGraph(method);
			completeResult = Common.toQ(completeResult.union(dg.getSlice(SliceDirection.BI_DIRECTIONAL, relevantCriteria.eval().nodes())).eval());
		}
		
		// make sure the selected criteria is highlighted 
		// (since it may have been computed and not be what the user actually selected)
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(criteria), Color.CYAN);
		
		// compute what to show for current steps
		Q f = Common.toQ(criteria).forwardStepOn(completeResult, forward);
		Q r = Common.toQ(criteria).reverseStepOn(completeResult, reverse);
		Q result = f.union(r).union(Common.toQ(criteria));
		
		// compute what is on the frontier
		Q frontierForward = Common.toQ(criteria).forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		
		Q frontierReverse = Common.toQ(criteria).reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(h));
	}

	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}
	
}
