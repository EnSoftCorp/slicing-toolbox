package com.ensoftcorp.open.slice.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class ControlDependenceSliceCodePainter extends DependenceSliceCodePainter {

	@Override
	public String getName() {
		return "Control Dependence Slice";
	}
	
	@Override
	public String getDescription() {
		return "Explores the control dependence graph (CDG).";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node function) {
		return DependenceGraph.Factory.buildCDG(function);
	}

}
