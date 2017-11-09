package com.ensoftcorp.open.slice.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;

public class ProgramDependenceSliceCodePainter extends DependenceSliceCodePainter {

	@Override
	public String getTitle() {
		return "Program Dependence Slice";
	}
	
	@Override
	public String getDescription() {
		return "Explores the program dependence graph (PDG), which is the unioned result of the corresponding control dependence graph (CDG) and data dependence graph (DDG).";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node function) {
		return DependenceGraph.Factory.buildPDG(function);
	}

}
