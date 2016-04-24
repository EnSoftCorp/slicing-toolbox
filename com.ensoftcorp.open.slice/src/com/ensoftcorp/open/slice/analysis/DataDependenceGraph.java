package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
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
		dfg = Common.toQ(dfg).edgesTaggedWithAny(XCSG.DataFlow_Edge).retainEdges().eval();
		this.dfg = dfg;
		
		AtlasSet<GraphElement> dataDependenceEdgeSet = new AtlasHashSet<GraphElement>();
		for(GraphElement dfEdge : dfg.edges()){
			GraphElement from = dfEdge.getNode(EdgeDirection.FROM);
			GraphElement fromStatement = getStatement(from);
			
			GraphElement to = dfEdge.getNode(EdgeDirection.TO);
			GraphElement toStatement = getStatement(to);
			
			// statement contains both data flow nodes
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
	public Q getSlice(GraphElement selection, SliceDirection direction) {
		Q statement = Common.toQ(selection);
		Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
		Q slice = Common.empty();
		if(direction == SliceDirection.REVERSE || direction == SliceDirection.BI_DIRECTIONAL){
//			slice = slice.union(Common.toQ(dfg).reverse(Common.toQ(dataFlowNode)));
			slice = slice.union(dataDependenceEdges.reverse(statement));
		} 
		if(direction == SliceDirection.FORWARD || direction == SliceDirection.BI_DIRECTIONAL){
//			slice = slice.union(Common.toQ(dfg).forward(Common.toQ(dataFlowNode)));
			slice = slice.union(dataDependenceEdges.forward(statement));
		}
		return slice;
	}
	
	/**
	 * Returns the control flow block for the corresponding data flow node
	 */
	public static GraphElement getStatement(GraphElement dataFlowNode){
		return Common.toQ(dataFlowNode).parent().eval().nodes().getFirst();
	}
	
}
