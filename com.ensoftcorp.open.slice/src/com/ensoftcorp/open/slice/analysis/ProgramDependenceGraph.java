package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ProgramDependenceGraph extends DependenceGraph {

	private ControlDependenceGraph cdg;
	private DataDependenceGraph ddg;
	private Graph pdg;
	
	public ProgramDependenceGraph(Graph cfg, Graph dfg){
		// sanity checks
		if(cfg.nodes().isEmpty() || cfg.edges().isEmpty()){
			pdg = Common.empty().eval();
			return;
		}
		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
			pdg = Common.empty().eval();
			return;
		}
		
		this.cdg = new ControlDependenceGraph(cfg);
		this.ddg = new DataDependenceGraph(dfg);
		this.pdg = cdg.getGraph().union(ddg.getGraph()).eval();
	}
	
	@Override
	public Q getGraph() {
		return Common.toQ(pdg);
	}

	@Override
	public Q getSlice(SliceDirection direction, AtlasSet<Node> criteria) {
		if(direction == SliceDirection.BI_DIRECTIONAL){
			return slice(SliceDirection.REVERSE, criteria).union(slice(SliceDirection.FORWARD, criteria));
		} else {
			return slice(direction, criteria);
		}
	}

	/**
	 * Helper method to break up slicing into only forward or reverse operations
	 * @param direction
	 * @param criteria
	 * @return
	 */
	private Q slice(SliceDirection direction, AtlasSet<Node> criteria) {
		if(direction == SliceDirection.BI_DIRECTIONAL){
			throw new RuntimeException("Bi-directional slicing must be done stepwise...");
		}
		Q criteriaStatements = Common.toQ(criteria).parent();
		Q slice = ddg.getSlice(direction, criteria).union(cdg.getSlice(direction, criteriaStatements.eval().nodes()));
		Q oldCriteria = Common.toQ(criteria).union(criteriaStatements);
		
		// iteratively slice on CDG and DDG of newly reached statements
		// until nothing new is learned
		long numSliceNodes = slice.eval().nodes().size();
		long numSliceEdges = slice.eval().edges().size();
		boolean fixedPoint = false;
		while(!fixedPoint){
			AtlasSet<Node> newCriteriaStatements = slice.nodesTaggedWithAny(XCSG.ControlFlow_Node).difference(oldCriteria).eval().nodes();
			AtlasSet<Node> newCriteria = Common.toQ(newCriteriaStatements).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).difference(oldCriteria).eval().nodes();
			slice = slice.union(ddg.getSlice(direction, newCriteria).union(cdg.getSlice(direction, newCriteriaStatements)));
			oldCriteria = Common.toQ(newCriteria).union(Common.toQ(newCriteriaStatements));
			
			Graph currentSlice = slice.eval();
			long currentNumSliceNodes = currentSlice.nodes().size();
			long currentNumSliceEdges = currentSlice.edges().size();
			if(currentNumSliceNodes == numSliceNodes && currentNumSliceEdges == numSliceEdges){
				fixedPoint = true;
			} else {
				numSliceNodes = currentNumSliceNodes;
				numSliceEdges = currentNumSliceEdges;
			}
			slice = Common.toQ(currentSlice);
		}
		return slice;
	}

}
