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

public class DataDependenceGraph extends DependenceGraph {

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

	private Graph dfg; // data flow graph (SSA form)
	private Graph ddg; // data dependency graph

	public DataDependenceGraph(Graph dfg) {
		// sanity checks
		if(dfg.nodes().isEmpty() || dfg.edges().isEmpty()){
			this.dfg = Common.toQ(dfg).eval();
			this.ddg = Common.empty().eval();
			return;
		}

		this.dfg = dfg;

		AtlasSet<Edge> dataDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> pointerDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> backwardDataDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> interDataDependenceEdgeSet = new AtlasHashSet<Edge>();

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
		}

		for(Edge dfEdge : dfg.edges()) {
			Node from = dfEdge.from();
			if(from.taggedWith(XCSG.Identity) || from.taggedWith(XCSG.Parameter)){
				continue;
			}
			Node fromStatement = CommonQueries.getContainingControlFlowNode(from);

			Node to = dfEdge.to();
			if(to.taggedWith(XCSG.ReturnValue)){
				continue;
			}
			
			Node toStatement = CommonQueries.getContainingControlFlowNode(to);

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

			Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
			Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
			if(dataDependenceEdge == null){
				dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
				dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(DEPENDENT_VARIABLE, from.getAttr(XCSG.name).toString());
				Log.info(fromStatement.getAttr(XCSG.name) + " -> " + toStatement.getAttr(XCSG.name));
			}
			dataDependenceEdgeSet.add(dataDependenceEdge);
		}

		// consider field reads
		Q interproceduralDataFlowEdges = Query.universe().edges(XCSG.InterproceduralDataFlow);
		Q fields = Query.universe().nodes(XCSG.Field, XCSG.ArrayComponents);
		Q parameters = Query.universe().nodes(XCSG.Parameter);
		Q returns = Query.universe().nodes(XCSG.Return);
		Q localDFG = Common.toQ(dfg).difference(parameters, returns);
		for(Node field : interproceduralDataFlowEdges.between(fields, localDFG).nodes(XCSG.Field).eval().nodes()){
			for(Node localDFNode : interproceduralDataFlowEdges.forward(Common.toQ(field)).intersection(localDFG).eval().nodes()){
				Node toStatement = localDFNode;
				if(!toStatement.taggedWith(XCSG.ReturnValue)){
					toStatement = getStatement(localDFNode);
				}
				Node fromStatement = field;

				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}

				Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}

		// add data dependencies for array references
		Q arrayIdentityForEdges = Query.universe().edges(XCSG.ArrayIdentityFor);
		for(Node arrayRead : Common.toQ(dfg).nodes(XCSG.ArrayRead).eval().nodes()){
			for(Node arrayIdentityFor : arrayIdentityForEdges.predecessors(Common.toQ(arrayRead)).eval().nodes()){
				Node fromStatement = arrayIdentityFor;
				if(!fromStatement.taggedWith(XCSG.Parameter) && !fromStatement.taggedWith(XCSG.Field)){
					fromStatement = getStatement(arrayIdentityFor);
				}

				Node toStatement = getStatement(arrayRead);

				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}

				Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}

		// add data dependencies for array indexes
		Q arrayIndexForEdges = Query.universe().edges(XCSG.ArrayIndexFor);
		for(Node arrayRead : Common.toQ(dfg).nodes(XCSG.ArrayRead).eval().nodes()) {
			for(Node arrayIndexFor : arrayIndexForEdges.predecessors(Common.toQ(arrayRead)).eval().nodes()){
				Node fromStatement = arrayIndexFor;
				if(!fromStatement.taggedWith(XCSG.Parameter) && !fromStatement.taggedWith(XCSG.Field)){
					fromStatement = getStatement(arrayIndexFor);
				}

				Node toStatement = getStatement(arrayRead);

				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}

				Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}

		/**
		 * Interprocedural Data Dependence
		 * TODO: Java has different categories of parameters.
		 * Currently we don't handle the Identity Pass (passing of a 'this' object to the callsite)
		 */

		// Handle parameters and returns
		Q callSites = Common.toQ(dfg).nodes(XCSG.CallSite);
		Q dataFlowEdges = Common.universe().edges(XCSG.DataFlow_Edge);
		Q passedToEdges = Common.universe().edges(XCSG.PassedTo);
		for(Node callSite : callSites.eval().nodes()) {
			// Can there be more than one return values to a callsite?
			Node returnVariable = Common.toQ(callSite).predecessorsOn(dataFlowEdges).predecessorsOn(dataFlowEdges).eval().nodes().one();
			if(returnVariable != null) {
				String returnVariableName = returnVariable.getAttr(XCSG.name).toString();
				Node fromStatement = CommonQueries.getContainingControlFlowNode(returnVariable);
				Node toStatement = CommonQueries.getContainingControlFlowNode(callSite);
				Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.tag(INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(DEPENDENT_VARIABLE, returnVariableName);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
				interDataDependenceEdgeSet.add(dataDependenceEdge);
			}
			AtlasSet<Node> parameterVariables = new AtlasHashSet<Node>();
			if(!CommonQueries.isEmpty(Common.toQ(dfg).nodes(XCSG.Language.C))) {
				 parameterVariables.addAll(Common.toQ(callSite).predecessorsOn(passedToEdges).eval().nodes());
			} else {
				parameterVariables.addAll(Common.toQ(callSite).predecessorsOn(passedToEdges).nodes(XCSG.Parameter).eval().nodes());
			}
			for(Node parameterVariable : parameterVariables) {
				Node fromStatement = CommonQueries.getContainingControlFlowNode(callSite);
				Q targets = Common.toQ(parameterVariable).successorsOn(dataFlowEdges).successorsOn(dataFlowEdges);
				String parameterVariableName = parameterVariable.getAttr(XCSG.name).toString();
				for(Node target : targets.eval().nodes()) {
					Node toStatement = CommonQueries.getContainingControlFlowNode(target);
					Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
					Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(DEPENDENT_VARIABLE, parameterVariableName);
					}
					dataDependenceEdgeSet.add(dataDependenceEdge);
					interDataDependenceEdgeSet.add(dataDependenceEdge);
				}	
			}
		}

		// Handle global variables
		Q definitions = Common.toQ(dfg).nodes(XCSG.Assignment);
		Q globalVariables = definitions.successorsOn(Common.universe().edges(XCSG.C.Provisional.NominalDataFlow)).nodes(XCSG.C.Provisional.TentativeGlobalVariableDefinition);
		for(Node globalVariable : globalVariables.eval().nodes()) {
			String globalVariableName = globalVariable.getAttr(XCSG.name).toString();
			Q globalVariableUses = Common.toQ(globalVariable).successorsOn(Common.universe().edges(XCSG.C.Provisional.NominalDataFlow));
			for(Node globalVariableUse : globalVariableUses.eval().nodes()) {
				for(Node definition : definitions.eval().nodes()) {
					Node fromStatement = CommonQueries.getContainingControlFlowNode(definition);
					Node toStatement = CommonQueries.getContainingControlFlowNode(globalVariableUse);
					Q dataDependenceEdges = Query.universe().edges(DATA_DEPENDENCE_EDGE);
					Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(GLOBAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, GLOBAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(DEPENDENT_VARIABLE, globalVariableName);
					}
				}
			}
		}

		this.ddg = Common.toQ(dataDependenceEdgeSet).eval();
	}

	public static Q getPointersContained(Q variables) {
		Q pointers = Common.empty();
		for(Node variable : variables.eval().nodes()) {
			Node type = Query.universe().edges(XCSG.TypeOf).successors(Common.toQ(variable)).eval().nodes().one();
			if(type != null && type.taggedWith(XCSG.Pointer)) {
				pointers = pointers.union(Common.toQ(variable));
			}
		}
		return pointers;
	}

	public AtlasSet<Node> getRedirectionTargets(Node statement, String variable) {
		Log.info(statement.getAttr(XCSG.name) + " : " + variable);
		AtlasSet<Node> redirectionTargets = new AtlasHashSet<Node>();
		Q predecessors = Query.universe().edges(DATA_DEPENDENCE_EDGE).predecessors(Common.toQ(statement));		
		for(Node predecessor : predecessors.eval().nodes()) {
			if(predecessor.getAttr(XCSG.name).toString().contains(variable)) {
				// this is a redirection candidate
				Log.info(predecessor.getAttr(XCSG.name) + "");
				redirectionTargets.add(predecessor);
			}
		}

		return redirectionTargets;
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
