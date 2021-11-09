package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;

public class SystemDependenceGraph extends DependenceGraph {
	
	private Graph sdg;
	
	public SystemDependenceGraph(){
		this.sdg = Common.empty().eval();
	}
	
	public void create() {
		if(sdg == null){
			sdg = Common.empty().eval();
			for(Node function : Query.universe().nodes(XCSG.Function).eval().nodes()){
				Graph dfg = CommonQueries.dfg(Common.toQ(function)).eval();
				Graph cfg = CommonQueries.cfg(Common.toQ(function)).eval();
				Graph pdg = new ProgramDependenceGraph(cfg, dfg).getGraph().eval();
				sdg.nodes().addAll(pdg.nodes());
				sdg.edges().addAll(pdg.edges());
			}
		}
		// todo: add data dependence from parameters and returns
		// todo: add call graph edges
	}
	
	@Override
	public Q getGraph() {
		return Common.toQ(sdg);
	}

}
