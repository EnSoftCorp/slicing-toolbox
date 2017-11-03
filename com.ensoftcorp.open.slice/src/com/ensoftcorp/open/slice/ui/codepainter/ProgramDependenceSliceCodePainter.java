package com.ensoftcorp.open.slice.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class ProgramDependenceSliceCodePainter extends DependenceSliceCodePainter {

	@Override
	public String getTitle() {
		return "Program Dependence Slice (PDG)";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node function) {
		return DependenceGraph.Factory.buildPDG(function);
	}

}
