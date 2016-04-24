package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;

public class ProgramDependenceGraph extends DependenceGraph {

	private ControlDependenceGraph cdg;
	private DataDependenceGraph ddg;
	private Graph pdg;
	
	public ProgramDependenceGraph(Graph cfg, Graph dfg){
		this.cdg = new ControlDependenceGraph(cfg);
		this.ddg = new DataDependenceGraph(dfg);
		this.pdg = cdg.getGraph().union(ddg.getGraph()).eval();
	}
	
	@Override
	public Q getGraph() {
		return Common.toQ(pdg);
	}

	@Override
	public Q getSlice(SliceDirection direction, AtlasSet<GraphElement> criteria) {
		Q slice = Common.empty();
		Q pdg = getGraph();
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(pdg.reverse(Common.toQ(criteria)));
		} 
		if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(pdg.forward(Common.toQ(criteria)));
		}
		return slice;
	}

}
