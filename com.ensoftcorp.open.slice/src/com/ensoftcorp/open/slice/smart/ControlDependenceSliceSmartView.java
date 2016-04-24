package com.ensoftcorp.open.slice.smart;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.slice.analysis.ControlDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;

public class ControlDependenceSliceSmartView extends FilteringAtlasSmartViewScript implements IResizableScript {

	@Override
	public String getTitle() {
		return "Control Dependence Slice";
	}
	
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
		return 0;
	}

	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
		Q filteredSelection = filter(event.getSelection());

		if(filteredSelection.eval().nodes().isEmpty()){
			return null;
		}
		
		AtlasSet<GraphElement> criteria = filteredSelection.eval().nodes();
		AtlasSet<GraphElement> statements = new AtlasHashSet<GraphElement>();
		for(GraphElement criterion : criteria){
			if(criterion.taggedWith(XCSG.DataFlow_Node)){
				statements.add(Common.toQ(criterion).parent().eval().nodes().getFirst());
			} else {
				statements.add(criterion);
			}
		}
		criteria = statements;
		
		Q completeResult = Common.empty();
		for(GraphElement method : StandardQueries.getContainingMethods(Common.toQ(criteria)).eval().nodes()){
			Q relevantCriteria = Common.toQ(method).contained().intersection(Common.toQ(criteria));
			ControlDependenceGraph cdg = DependenceGraph.Factory.buildCDG(method);
			completeResult = Common.toQ(completeResult.union(cdg.getSlice(SliceDirection.BI_DIRECTIONAL, relevantCriteria.eval().nodes())).eval());
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