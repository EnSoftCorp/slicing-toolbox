package com.ensoftcorp.open.slice.ui.codepainter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CallSiteAnalysis;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.codepainter.CodePainter;
import com.ensoftcorp.open.commons.codepainter.ColorPalette;
import com.ensoftcorp.open.commons.utilities.FormattedSourceCorrespondence;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;

public abstract class DependenceSliceCodePainter extends CodePainter {

	@Override
	public String getCategory() {
		return "Slicing";
	}

	@Override
	public String[] getSupportedNodeTags() {
		return new String[] { XCSG.ControlFlow_Node, XCSG.DataFlow_Node, XCSG.Function };
	}
	
	@Override
	public String[] getSupportedEdgeTags() {
		return NOTHING;
	}

	@Override
	public int getDefaultStepReverse() {
		return 1;
	}
	
	@Override
	public int getDefaultStepForward() {
		return 1;
	}
	
	@Override
	public Q convertSelection(Q filteredSelections){
		Q dataFlowNodes = filteredSelections.nodes(XCSG.DataFlow_Node);
		Q controlFlowNodes = filteredSelections.nodes(XCSG.ControlFlow_Node);
		Q functions = filteredSelections.nodes(XCSG.Function);
		
		// convert data flow nodes to control flow nodes
		return controlFlowNodes.union(functions, dataFlowNodes.parent());
	}

	@Override
	public UnstyledFrontierResult computeFrontierResult(Q filteredSelections, int reverse, int forward) {
		Q selectedFunctions = filteredSelections.nodes(XCSG.Function);
		
		// remove any functions that are selected because callsites were selected
		Q selectedStatements = Common.toQ(DependenceGraph.getStatements(filteredSelections.difference(selectedFunctions).eval().nodes()));
		Q selectedCallsites = selectedStatements.children().nodes(XCSG.CallSite);
		Q selectedCallsiteFunctions = CallSiteAnalysis.getTargets(selectedCallsites);
		selectedFunctions = selectedFunctions.difference(selectedCallsiteFunctions);
		
		AtlasSet<Node> criteria = selectedStatements.eval().nodes();
		
		if(criteria.isEmpty() && selectedFunctions.eval().nodes().isEmpty()){
			return null; // no applicable selection, do nothing
		}
		
		// debug
//		try {
//			Log.info("Slicing Criteria: " + getSlicingCriteriaSummary(criteria));
//		} catch (Exception e){
//			Log.warning("Error summarizing slicing criteria", e);
//		}
		
		// for each selected function, just show the whole dependence graph
		Q completeDependenceGraphs = Common.empty();
		for(Node function : new AtlasHashSet<Node>(selectedFunctions.eval().nodes())){
			DependenceGraph dg = getDependenceGraph(function);
			Graph g = dg.getGraph().eval();
			completeDependenceGraphs = completeDependenceGraphs.union(Common.toQ(g));
		}
		
		// create the slice for each function's statement 
		Q completeResult = Common.empty();
		for(Node function : new AtlasHashSet<Node>(CommonQueries.getContainingFunctions(Common.toQ(criteria)).eval().nodes())){
			Q relevantCriteria = Common.toQ(function).contained().intersection(Common.toQ(criteria));
			DependenceGraph dg = getDependenceGraph(function);
			completeResult = Common.toQ(completeResult.union(dg.getSlice(SliceDirection.BI_DIRECTIONAL, relevantCriteria.eval().nodes())).eval());
		}
		
		// compute what to show for current steps
		Q f = Common.toQ(criteria).forwardStepOn(completeResult, forward);
		Q r = Common.toQ(criteria).reverseStepOn(completeResult, reverse);
		Q result = f.union(r).union(Common.toQ(criteria), completeDependenceGraphs);
		
		// compute what is on the frontier
		Q frontierReverse = Common.toQ(criteria).reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.differenceEdges(result).retainEdges();
		Q frontierForward = Common.toQ(criteria).forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.differenceEdges(result).retainEdges();

		// a selection could include a function, so explicitly include it in the result to be highlighted
		return new UnstyledFrontierResult(result.union(selectedFunctions), frontierReverse, frontierForward);
	}
	
	/*
	 * Converts the slicing criteria to a human readable string
	 * Useful for debugging
	 */
	protected String getSlicingCriteriaSummary(AtlasSet<Node> criteria) throws IOException {
		StringBuilder summary = new StringBuilder();
		String prefix = "";
		for(Node criterion : new AtlasHashSet<Node>(criteria)){
			FormattedSourceCorrespondence fsc = FormattedSourceCorrespondence.getSourceCorrespondent(criterion);
			long startLine = fsc.getStartLineNumber();
			long endLine = fsc.getEndLineNumber(); 
			String lines = startLine == endLine ? ("line " + startLine) : ("lines " + startLine + "-" + endLine);
			summary.append(prefix + criterion.getAttr(XCSG.name).toString() + " (" + lines + " of " + fsc.getFile().getName() + ")");
			prefix = ", ";
		}
		return summary.toString();
	}
	
	protected abstract DependenceGraph getDependenceGraph(Node function);

	
	@Override
	public List<ColorPalette> getDefaultColorPalettes() {
		List<ColorPalette> result = new LinkedList<ColorPalette>();
		result.add(new ProgramDependenceColorPalette());
		return result;
	}


}
