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

public class JimpleDataDependenceGraph extends DependenceGraph {

	private Graph dfg; // data flow graph (SSA form)
	private Graph ddg; // data dependency graph

	public JimpleDataDependenceGraph(Graph dfg) {
		// sanity checks
		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
			this.dfg = Common.toQ(dfg).eval();
			this.ddg = Common.empty().eval();
			return;
		}

		this.dfg = dfg;
		
		AtlasSet<Edge> dataDependenceEdgeSet = new AtlasHashSet<Edge>();
		
		// this is sort of a logical patch for an oddity in jimple
		// 1. $r0 = new java.io.FileInputStream;
		// 2. $r3 = args[0];
		// 3. specialinvoke $r0.<java.io.FileInputStream: void <init>(java.lang.String)>($r3);
		// 4. use of $r0
		// consider the use of r0, the data flow that Atlas produces has an incoming edge from line 1
		// but there is not dependency on line 3, even though line 3 is necessary to instantiate r0
		// to this end we can add a fake local data flow edge from 3 to 1 to simulate the dependency
		Q initializationCallsites = CommonQueries.nodesContaining(Common.toQ(dfg).nodes(XCSG.Language.Jimple).nodes(XCSG.ObjectOrientedStaticCallSite), "<init>");
		for(Node initializationCallsite : initializationCallsites.eval().nodes()) {
			Node initializationStatement = Common.toQ(initializationCallsite).parent().nodes(XCSG.ControlFlow_Node).eval().nodes().one();
			if(initializationStatement != null) {
				Node identityPass = Common.toQ(initializationStatement).children().nodes(XCSG.IdentityPass).eval().nodes().one();
				if(identityPass != null) {
					Q instantiationStatements = Common.toQ(dfg).nodes(XCSG.Language.Jimple).nodes(XCSG.Instantiation).parent().nodes(XCSG.ControlFlow_Node);
					Node instantiationStatement = Common.toQ(dfg).predecessors(Common.toQ(identityPass)).parent()
							.intersection(instantiationStatements).eval().nodes().one();
					if(instantiationStatement != null) {
						// create a data dependency edge from the initialization statement to the instantiation statement
						// if one does not already exist
						if(!initializationStatement.equals(instantiationStatement)) {
							if(CommonQueries.isEmpty(Query.universe().edges(DataDependenceGraph.JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE).between(Common.toQ(initializationStatement), Common.toQ(instantiationStatement)))) {
								Edge dataDependenceEdge = Graph.U.createEdge(initializationStatement, instantiationStatement);
								dataDependenceEdge.tag(DataDependenceGraph.DATA_DEPENDENCE_EDGE);
								dataDependenceEdge.tag(DataDependenceGraph.JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE);
								dataDependenceEdge.putAttr(XCSG.name, DataDependenceGraph.DATA_DEPENDENCE_EDGE);
								dataDependenceEdgeSet.add(dataDependenceEdge);
							}
						}
					}
				}
			}
		}
		
		this.ddg = Common.toQ(dataDependenceEdgeSet).eval();
	}

	@Override
	public Q getGraph() {
		return Common.toQ(ddg);
	}

}
