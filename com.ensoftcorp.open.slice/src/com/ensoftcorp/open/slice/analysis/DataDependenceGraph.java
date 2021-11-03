package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.slice.log.Log;
import com.ensoftcorp.open.slice.xcsg.AnalysisXCSG;

/**
 * Compute Data Dependence Graph
 * 
 * @author Payas Awadhutkar, Ben Holland
 */

public class DataDependenceGraph extends DependenceGraph {

	protected Graph dfg; // data flow graph (SSA form)
	protected Graph ddg; // data dependency graph

	AtlasSet<Edge> dataDependenceEdgeSet;
	AtlasSet<Edge> interDataDependenceEdgeSet;

	public DataDependenceGraph(Graph dfg) {
		this.dfg = dfg;
		this.ddg = Common.empty().eval();
		this.dataDependenceEdgeSet = new AtlasHashSet<Edge>();
		this.interDataDependenceEdgeSet = new AtlasHashSet<Edge>();
	}

	public void create() {

		// sanity checks
		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
			this.ddg = Common.empty().eval();
			return;
		}

		for(Edge dfEdge: dfg.edges()) {
			Node from = dfEdge.from();
			Node fromStatement = CommonQueries.getContainingControlFlowNode(from);
			Node to = dfEdge.to();
			Node toStatement = CommonQueries.getContainingControlFlowNode(to);

			// sanity checks
			if(fromStatement == null){
				Log.warning("From node has no parent or is null: " + from.addressBits());
				continue;
			}
			if(toStatement == null){
				Log.warning("To node has no parent or is null: " + to.addressBits());
				continue;
			}

			// statement contains both data flow nodes
			// this is a little noisy to create relationships to all the time
			// example: "x = 1;" is a single statement with a data dependence from 1 to x
			// skip the trivial edges
			if(fromStatement.equals(toStatement)){
				continue;
			}

			Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
			Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
			if(dataDependenceEdge == null){
				dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
				dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, from.getAttr(XCSG.name).toString());
				Log.info(fromStatement.getAttr(XCSG.name) + " -> " + toStatement.getAttr(XCSG.name));
			}
			dataDependenceEdgeSet.add(dataDependenceEdge);

		}

	}

	protected Edge createDataDependenceEdge(Node fromStatement, Node toStatement, String variableName) {
		Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
		Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
		if(dataDependenceEdge == null){
			dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
			dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
			dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
			dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, variableName);
			Log.info(fromStatement.getAttr(XCSG.name) + " -> " + toStatement.getAttr(XCSG.name));
		}
		return dataDependenceEdge;
	}

	@Override
	public Q getGraph() {
		return Common.toQ(ddg);
	}

	/**
	 * Returns the underlying data flow graph
	 * @return
	 */
	protected Q getDataFlowGraph(){
		return Common.toQ(dfg);
	}

}
