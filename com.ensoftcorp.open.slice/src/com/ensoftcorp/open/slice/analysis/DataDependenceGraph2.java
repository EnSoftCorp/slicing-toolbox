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

/**
 * Compute Data Dependence Graph
 * 
 * @author Payas Awadhutkar, Ben Holland
 */

public class DataDependenceGraph2 extends DependenceGraph {

	/**
	 * Used to tag the edges between nodes that contain a data dependence
	 */
	public static final String DATA_DEPENDENCE_EDGE = "data-dependence";

	/**
	 * Used to tag the edges between nodes that contain a data dependence due to a pointer
	 */
	public static final String POINTER_DEPENDENCE_EDGE = "pointer-dependence";

	/**
	 * Used to tag the edges between nodes that contain a backwards data dependence
	 */
	public static final String BACKWARD_DATA_DEPENDENCE_EDGE = "backward-data-dependence";

	public static final String GLOBAL_DATA_DEPENDENCE_EDGE = "global-data-dependence";

	/**
	 * Used to tag the edges representing interprocedural data dependence
	 */
	public static final String INTERPROCEDURAL_DATA_DEPENDENCE_EDGE = "data-dependence (inter)";

	/**
	 * Used to simulate the implict data dependency from initialization to instantiation
	 */
	public static final String JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE = "jimple-initialization-data-dependence";

	/**
	 * Used to identify the dependent variable
	 */
	public static final String DEPENDENT_VARIABLE = "dependent-variable";

	protected Graph dfg; // data flow graph (SSA form)
	protected Graph ddg; // data dependency graph

	AtlasSet<Edge> dataDependenceEdgeSet = new AtlasHashSet<Edge>();
	AtlasSet<Edge> interDataDependenceEdgeSet = new AtlasHashSet<Edge>();

	public DataDependenceGraph2(Graph dfg) {
		this.dfg = dfg;
		this.ddg = Common.empty().eval();
	}

	public void create() {
		
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

		}

	}
	
	protected Edge createDataDependenceEdge(Node fromStatement, Node toStatement, String variableName) {
		Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
		Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
		if(dataDependenceEdge == null){
			dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
			dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
			dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
			dataDependenceEdge.putAttr(DEPENDENT_VARIABLE, variableName);
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
