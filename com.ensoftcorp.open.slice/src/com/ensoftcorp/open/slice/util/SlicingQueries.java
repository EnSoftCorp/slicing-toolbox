package com.ensoftcorp.open.slice.util;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph2;

public class SlicingQueries {
	
	private static Q dataDependenceEdges = Common.universe().edges(DataDependenceGraph2.DATA_DEPENDENCE_EDGE);
	
	public static Q variableSpecificDataDependence(Q selectedStatement, String variableName) {
		Q selectedControlFlow = Common.toQ(CommonQueries.getContainingControlFlowNode(selectedStatement.eval().nodes().one()));
		Q variableSpecificDataDependenceGraph = Common.empty();
		Q variableDependenceEdges = dataDependenceEdges.selectEdge(DataDependenceGraph2.DEPENDENT_VARIABLE, variableName);
		variableDependenceEdges = variableDependenceEdges.union(Common.universe().selectEdge(DataDependenceGraph2.DEPENDENT_VARIABLE, variableName + "="));
		Q immediateDependencies = variableDependenceEdges.reverseStep(selectedControlFlow);
		variableSpecificDataDependenceGraph = dataDependenceEdges.reverse(immediateDependencies.roots());
		variableSpecificDataDependenceGraph = variableSpecificDataDependenceGraph.union(immediateDependencies);
		return variableSpecificDataDependenceGraph;
	}
	
	

}
