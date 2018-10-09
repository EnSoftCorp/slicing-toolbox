package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
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
	
	public Q getSlice(SliceDirection direction, AtlasSet<Node> criteria) {
		Q dependenceGraph = getGraph();
		Q slice = Common.empty();
		
		Q statements = Common.toQ(getStatements(criteria));
		
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(dependenceGraph.reverse(statements));
		} 
		
		if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			slice = slice.union(dependenceGraph.forward(statements));
		}
		
		return slice;
	}
	
	/**
	 * Returns the control flow nodes given a mixed set of control and data flow nodes
	 * If a node is a data flow node, its parent control flow node is returned
	 * @param criteria
	 * @return
	 */
	public static AtlasSet<Node> getStatements(AtlasSet<Node> criteria){
		AtlasSet<Node> statements = new AtlasHashSet<Node>();
		for(Node criterion : criteria){
			if(criterion.taggedWith(XCSG.DataFlow_Node)){
				statements.add(DataDependenceGraph.getStatement(criterion));
			} else if(criterion.taggedWith(XCSG.ControlFlow_Node)){
				statements.add(criterion);
			}
		}
		return statements;
	}
	
	/**
	 * Returns the control flow node for the corresponding data flow node
	 */
	public static Node getStatement(Node dataFlowNode){
		return Common.toQ(dataFlowNode).parent().eval().nodes().one();
	}
	
	public static class Factory {
		
		/**
		 * Returns an intra-procedural Control Dependence Graph (CDG) for the given function
		 * @param function
		 * @return
		 */
		public static ControlDependenceGraph buildCDG(Node function){
			Q controlFlowEdges = Query.universe().edges(XCSG.ControlFlow_Edge);
			Q cfg = controlFlowEdges.forward(Common.toQ(function).contained().nodes(XCSG.controlFlowRoot));
			ControlDependenceGraph cdg = new ControlDependenceGraph(cfg.eval());
			return cdg;
		}
		
		/**
		 * Returns an intra-procedural Data Dependence Graph (DDG) for the given function
		 * @param function
		 * @return
		 */
		public static DataDependenceGraph buildDDG(Node function){
			Q localDataFlowEdges = Query.universe().edges(XCSG.LocalDataFlow);
			Q localDFG = Common.toQ(function).contained().nodes(XCSG.DataFlow_Node).induce(localDataFlowEdges);
			Q dfg = Common.toQ(localDFG.eval());
			dfg = localDataFlowEdges.reverseStep(dfg); // get parameters, identity
			dfg = localDataFlowEdges.forwardStep(dfg); // get return values
			DataDependenceGraph ddg = new DataDependenceGraph(dfg.eval());
			return ddg;
		}
		
		/**
		 * Returns an intra-procedural Program Dependence Graph (PDG) for the given function
		 * @param function
		 * @return
		 */
		public static ProgramDependenceGraph buildPDG(Node function){
			Q controlFlowEdges = Query.universe().edges(XCSG.ControlFlow_Edge);
			Q cfg = controlFlowEdges.forward(Common.toQ(function).contained().nodes(XCSG.controlFlowRoot));
			
			Q dataFlowEdges = Query.universe().edges(XCSG.DataFlow_Edge);
			Q localDataFlowEdges = Query.universe().edges(XCSG.LocalDataFlow);
			Q dfg = Common.toQ(function).contained().nodes(XCSG.DataFlow_Node).induce(dataFlowEdges);
			dfg = localDataFlowEdges.reverseStep(dfg); // get parameters
			dfg = localDataFlowEdges.forwardStep(dfg); // get return values
			
			ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.eval(), dfg.eval());
			return pdg;
		}
	}
	
}
