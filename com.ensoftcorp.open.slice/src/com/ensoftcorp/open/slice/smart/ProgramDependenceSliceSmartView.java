package com.ensoftcorp.open.slice.smart;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class ProgramDependenceSliceSmartView extends DependenceSliceSmartView {

	@Override
	public String getTitle() {
		return "Program Dependence Slice";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node method) {
		return DependenceGraph.Factory.buildPDG(method);
	}
	
}