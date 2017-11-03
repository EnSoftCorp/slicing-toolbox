package com.ensoftcorp.open.slice.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class ControlDependenceSliceCodePainter extends DependenceSliceCodePainter {

	@Override
	public String getTitle() {
		return "Control Dependence Slice (CDG)";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node function) {
		return DependenceGraph.Factory.buildCDG(function);
	}

}
