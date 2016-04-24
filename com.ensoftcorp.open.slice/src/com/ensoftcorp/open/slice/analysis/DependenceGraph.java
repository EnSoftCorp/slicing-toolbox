package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public abstract class DependenceGraph {

	public static enum SliceDirection {
		FORWARD, REVERSE, BI_DIRECTIONAL;
		
		@Override
		public String toString(){
			return this.name();
		}
	}
	
	public abstract Q getGraph();
	
	public abstract Q getSlice(SliceDirection direction, AtlasSet<GraphElement> criteria);
	
	public static class Factory {
		/**
		 * Returns an intra-procedural Control Dependence Graph (CDG) for the given method
		 * @param method
		 * @return
		 */
		public static ControlDependenceGraph buildCDG(GraphElement method){
			Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
			Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
			ControlDependenceGraph cdg = new ControlDependenceGraph(cfg.eval());
			return cdg;
		}
		
		/**
		 * Returns an intra-procedural Data Dependence Graph (DDG) for the given method
		 * @param method
		 * @return
		 */
		public static DataDependenceGraph buildDDG(GraphElement method){
			Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
			Q localDataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.LocalDataFlow);
			Q dfg = Common.toQ(method).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).induce(dataFlowEdges);
			dfg = localDataFlowEdges.reverseStep(dfg); // get parameters
			dfg = localDataFlowEdges.forwardStep(dfg); // get return values
			DataDependenceGraph ddg = new DataDependenceGraph(dfg.eval());
			return ddg;
		}
		
		/**
		 * Returns an intra-procedural Program Dependence Graph (PDG) for the given method
		 * @param method
		 * @return
		 */
		public static ProgramDependenceGraph buildPDG(GraphElement method){
			Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
			Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
			
			Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
			Q localDataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.LocalDataFlow);
			Q dfg = Common.toQ(method).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).induce(dataFlowEdges);
			dfg = localDataFlowEdges.reverseStep(dfg); // get parameters
			dfg = localDataFlowEdges.forwardStep(dfg); // get return values
			
			ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.eval(), dfg.eval());
			return pdg;
		}
	}
	
}
