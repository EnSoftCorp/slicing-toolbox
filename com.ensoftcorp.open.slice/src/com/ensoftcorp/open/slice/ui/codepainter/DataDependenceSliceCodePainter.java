package com.ensoftcorp.open.slice.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class DataDependenceSliceCodePainter extends DependenceSliceCodePainter {

	@Override
	public String getTitle() {
		return "Data Dependence Slice (DDG)";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node function) {
		return DependenceGraph.Factory.buildDDG(function);
	}

}