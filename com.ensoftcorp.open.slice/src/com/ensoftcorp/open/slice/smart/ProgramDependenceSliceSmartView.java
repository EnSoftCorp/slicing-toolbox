package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
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
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;
import com.ensoftcorp.open.commons.analysis.utils.StandardQueries;

public class ProgramDependenceSliceSmartView extends FilteringAtlasSmartViewScript implements IResizableScript {

	@Override
	public String getTitle() {
		return "Program Dependence Slice";
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[] { XCSG.DataFlow_Node, XCSG.Parameter };
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
		
		AtlasSet<Node> criteria = filteredSelection.eval().nodes();
		Q completeResult = Common.empty();
		for(GraphElement method : StandardQueries.getContainingMethods(Common.toQ(criteria)).eval().nodes()){
			ProgramDependenceGraph pdg = DependenceGraph.Factory.buildPDG(method);
			AtlasSet<Node> relevantCriteria = pdg.getGraph().intersection(Common.toQ(criteria)).eval().nodes();
			completeResult = completeResult.union(pdg.getSlice(SliceDirection.BI_DIRECTIONAL, relevantCriteria));
		}
		
		// result is a statement (control flow) level graph with the exception of parameters and return values
		Q statements = filteredSelection.parent()
				.union(filteredSelection.nodesTaggedWithAny(XCSG.Parameter, XCSG.ReturnValue)); 
		
		// compute what to show for current steps
		Q f = statements.forwardStepOn(completeResult, forward);
		Q r = statements.reverseStepOn(completeResult, reverse);
		Q result = f.union(r).union(Common.toQ(criteria));
		
		// compute what is on the frontier
		Q frontierForward = statements.forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		
		Q frontierReverse = statements.reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(new Highlighter()));
	}

	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}

}