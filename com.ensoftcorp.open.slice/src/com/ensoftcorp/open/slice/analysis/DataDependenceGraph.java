package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.slice.log.Log;

public class DataDependenceGraph extends DependenceGraph {

	/**
	 * Used to tag the edges between nodes that contain a data dependence
	 */
	public static final String DATA_DEPENDENCE_EDGE = "data-dependence";
	
	/**
	 * Used to simulate the implict data dependency from initialization to instantiation
	 */
	public static final String JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE = "jimple-initialization-data-dependence";
	
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
		
		AtlasSet<Edge> dataDependenceEdgeSet = new AtlasHashSet<Edge>();
		
		if(!CommonQueries.isEmpty(Common.toQ(dfg).nodes(XCSG.Language.Jimple))) {
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
								if(CommonQueries.isEmpty(Common.universe().edges(DataDependenceGraph.JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE).between(Common.toQ(initializationStatement), Common.toQ(instantiationStatement)))) {
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
		}
		
		for(Edge dfEdge : dfg.edges()){
			Node from = dfEdge.getNode(EdgeDirection.FROM);
			Node fromStatement = from;
			if(!fromStatement.taggedWith(XCSG.Identity) && !fromStatement.taggedWith(XCSG.Parameter)){
				fromStatement = getStatement(from);
			}
			
			Node to = dfEdge.getNode(EdgeDirection.TO);
			Node toStatement = to;
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
			Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().getFirst();
			if(dataDependenceEdge == null){
				dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
				dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
			}
			dataDependenceEdgeSet.add(dataDependenceEdge);
		}
		
		// consider field reads
		Q interproceduralDataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.InterproceduralDataFlow);
		Q fields = Common.universe().nodesTaggedWithAny(XCSG.Field, XCSG.ArrayComponents);
		Q parameters = Common.universe().nodesTaggedWithAny(XCSG.Parameter);
		Q returns = Common.universe().nodesTaggedWithAny(XCSG.Return);
		Q localDFG = Common.toQ(dfg).difference(parameters, returns);
		for(Node field : interproceduralDataFlowEdges.between(fields, localDFG).nodesTaggedWithAny(XCSG.Field).eval().nodes()){
			for(Node localDFNode : interproceduralDataFlowEdges.forward(Common.toQ(field)).intersection(localDFG).eval().nodes()){
				Node toStatement = localDFNode;
				if(!toStatement.taggedWith(XCSG.ReturnValue)){
					toStatement = getStatement(localDFNode);
				}
				Node fromStatement = field;
				
				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}
				
				Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().getFirst();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}
		
		// add data dependencies for array references
		Q arrayIdentityForEdges = Common.universe().edgesTaggedWithAny(XCSG.ArrayIdentityFor);
		for(Node arrayRead : Common.toQ(dfg).nodesTaggedWithAny(XCSG.ArrayRead).eval().nodes()){
			for(Node arrayIdentityFor : arrayIdentityForEdges.predecessors(Common.toQ(arrayRead)).eval().nodes()){
				Node fromStatement = arrayIdentityFor;
				if(!fromStatement.taggedWith(XCSG.Parameter) && !fromStatement.taggedWith(XCSG.Field)){
					fromStatement = getStatement(arrayIdentityFor);
				}
				
				Node toStatement = getStatement(arrayRead);
				
				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}
				
				Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().getFirst();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}
		
		// add data dependencies for array indexes
		Q arrayIndexForEdges = Common.universe().edgesTaggedWithAny(XCSG.ArrayIndexFor);
		for(Node arrayRead : Common.toQ(dfg).nodesTaggedWithAny(XCSG.ArrayRead).eval().nodes()){
			for(Node arrayIndexFor : arrayIndexForEdges.predecessors(Common.toQ(arrayRead)).eval().nodes()){
				Node fromStatement = arrayIndexFor;
				if(!fromStatement.taggedWith(XCSG.Parameter) && !fromStatement.taggedWith(XCSG.Field)){
					fromStatement = getStatement(arrayIndexFor);
				}
				
				Node toStatement = getStatement(arrayRead);
				
				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}
				
				Q dataDependenceEdges = Common.universe().edgesTaggedWithAny(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().getFirst();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
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
	
}
