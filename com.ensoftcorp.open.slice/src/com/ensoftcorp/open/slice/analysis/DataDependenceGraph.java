package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class DataDependenceGraph extends DependenceGraph {

	/**
	 * Used to tag the edges between nodes that contain a data dependence
	 */
	public static final String DATA_DEPENDENCE_EDGE = "data-dependence";
	
	private Graph dfg; // data flow graph (SSA form)
	private Graph ddg; // data dependency graph
	
	public DataDependenceGraph(Graph dfg){
		// sanity checks
		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
			this.dfg = Common.toQ(dfg).eval();
			this.ddg = Common.empty().eval();
			return;
		}
		
		this.dfg = dfg;
		
		AtlasSet<GraphElement> dataDependenceEdgeSet = new AtlasHashSet<GraphElement>();
		for(GraphElement dfEdge : dfg.edges()){
			
			GraphElement from = dfEdge.getNode(EdgeDirection.FROM);
			GraphElement fromStatement = from;
			if(!fromStatement.taggedWith(XCSG.Parameter)){
				fromStatement = getStatement(from);
			}
			
			GraphElement to = dfEdge.getNode(EdgeDirection.TO);
			GraphElement toStatement = to;
			if(!toStatement.taggedWith(XCSG.ReturnValue)){
				toStatement = getStatement(to);
			}
			
			// sanity checks
			if(fromStatement == null){
				Log.warning("From node has no parent or is null: " + from.address().toAddressString());
				continue;
			}
			if(toStatement == null){
				Log.warning("To node has no parent or is null: " + to.address().toAddressString());
				continue;
			}
			
			// statement contains both data flow nodes
			// this is a little noisy to create relationships to all the time
			// example: "x = 1;" is a single statement with a data dependence from 1 to x
			// skip the trivial edges
			if(fromStatement.equals(toStatement)){
				continue;
			}
			
			Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
			GraphElement dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().getFirst();
			if(dataDependenceEdge == null){
				dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
				dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
			}
			dataDependenceEdgeSet.add(dataDependenceEdge);
		}
		this.ddg = Common.toQ(dataDependenceEdgeSet).eval();
	}

	@Override
	public Q getGraph() {
		return Common.toQ(ddg);
	}
	
	/**
	 * Returns the underlying data flow graph
	 * @return
	 */
	public Q getDataFlowGraph(){
		return Common.toQ(dfg);
	}

	@Override
	public Q getSlice(SliceDirection direction, AtlasSet<GraphElement> criteria) {
		Q dataFlowEdges = Common.toQ(dfg);
		Q relevantDataFlowNodes = Common.empty();
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
			relevantDataFlowNodes = relevantDataFlowNodes.union(dataFlowEdges.reverse(Common.toQ(criteria)));
		} 
		if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
			relevantDataFlowNodes = relevantDataFlowNodes.union(dataFlowEdges.forward(Common.toQ(criteria)));
		}
		Q containsEdges = Common.universe().edgesTaggedWithAny(XCSG.Contains);
		Q relevantStatements = containsEdges.predecessors(relevantDataFlowNodes)
				.union(relevantDataFlowNodes.nodesTaggedWithAny(XCSG.Parameter, XCSG.ReturnValue));
		Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
		Q slice = relevantStatements.induce(dataDependenceEdges);
		return slice;
	}
	
	/**
	 * Returns the control flow block for the corresponding data flow node
	 */
	public static GraphElement getStatement(GraphElement dataFlowNode){
		return Common.toQ(dataFlowNode).parent().eval().nodes().getFirst();
	}
	
}
