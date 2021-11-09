package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;

public class ProgramDependenceGraph extends DependenceGraph {

	private ControlDependenceGraph cdg;
	private DataDependenceGraph ddg;
	private Graph pdg;
	
	public ProgramDependenceGraph(Graph cfg, Graph dfg){
//		// sanity checks
//		if(cfg.nodes().isEmpty() || cfg.edges().isEmpty()){
//			pdg = Common.empty().eval();
//			return;
//		}
//		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
//			pdg = Common.empty().eval();
//			return;
//		}
		
		this.cdg = new ControlDependenceGraph(cfg);
		this.ddg = new DataDependenceGraph(dfg);
		this.pdg = Common.empty().eval();
	}
		
		
	public void create() {
		cdg.create();
		ddg.create();
		pdg = cdg.getGraph().union(ddg.getGraph()).eval();
	}
	
	@Override
	public Q getGraph() {
		return Common.toQ(pdg);
	}

}
