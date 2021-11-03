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
 * @author Payas Awadhutkar
 */

public class CDataDependenceGraph extends DataDependenceGraph2 {
	
	public CDataDependenceGraph(Graph dfg) {
		super(dfg);
	}

	public void create(Graph dfg){
		super.create();
		Graph dfgWithEdges = Common.toQ(dfg).induce(Query.universe().edges(XCSG.DataFlow_Edge)).eval();

		// sanity checks
		if(dfgWithEdges.nodes().isEmpty() || dfgWithEdges.edges().isEmpty()){
			this.dfg = Common.toQ(dfgWithEdges).eval();
			this.ddg = Common.empty().eval();
			return;
		}

		this.dfg = dfgWithEdges;

		AtlasSet<Edge> dataDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> pointerDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> backwardDataDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> interDataDependenceEdgeSet = new AtlasHashSet<Edge>();

		for(Edge dfEdge : dfgWithEdges.edges()){
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
				Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
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

				Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}

		// add data dependencies for array indexes
		Q arrayIndexForEdges = Query.universe().edges(XCSG.ArrayIndexFor);
		for(Node arrayRead : Common.toQ(dfg).nodes(XCSG.ArrayRead).eval().nodes()){
			for(Node arrayIndexFor : arrayIndexForEdges.predecessors(Common.toQ(arrayRead)).eval().nodes()){
				Node fromStatement = arrayIndexFor;
				if(!fromStatement.taggedWith(XCSG.Parameter) && !fromStatement.taggedWith(XCSG.Field)){
					fromStatement = getStatement(arrayIndexFor);
				}

				Node toStatement = getStatement(arrayRead);

				if(fromStatement == null || toStatement == null || fromStatement.equals(toStatement)){
					continue;
				}

				Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
			}
		}

		/**
		 * Handle Pointers
		 */
		Q pointerFlows = Common.toQ(dfg).nodes(XCSG.C.Provisional.Star,XCSG.C.Provisional.Ampersand);
		for(Node pointerFlow : pointerFlows.eval().nodes()) {
			Node toStatement = CommonQueries.getContainingControlFlowNode(pointerFlow);
			Q predecessors = Query.universe().edges("pointerDereference","addressOf",XCSG.InterproceduralDataFlow).predecessors(Common.toQ(pointerFlow));
			Q edgesToProcess = Common.empty();
			if(!CommonQueries.isEmpty(predecessors.nodes(XCSG.C.Provisional.Star))) {
				// check if the current node is part of a double pointer dereference
				// If so, ignore as it should be processed at the outermost dereferencing
				continue;
			} else {
				edgesToProcess = Query.universe().edges("pointerDereference","addressOf",XCSG.InterproceduralDataFlow).reverseStep(Common.toQ(pointerFlow));
				// Check if there is an incoming pointer. In which case, we should ignore the direct link from the dereferenced variable
				Q pointers = getPointersContained(predecessors);
				if(!CommonQueries.isEmpty(pointers)) {
					edgesToProcess = edgesToProcess.forward(pointers);
				}
			}
			// Q edgesToProcess = Query.universe().edges("pointerDereference","addressOf",XCSG.InterproceduralDataFlow).reverseStep(Common.toQ(pointerFlow));
			for(Edge edgeToProcess : edgesToProcess.eval().edges()) {
				Node dependentVariable = edgeToProcess.from();
				if(dependentVariable.taggedWith(XCSG.Assignment)) {
					Node fromStatement = CommonQueries.getContainingControlFlowNode(dependentVariable);
					Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(AnalysisXCSG.POINTER_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, dependentVariable.getAttr(XCSG.name).toString());
					}
					dataDependenceEdgeSet.add(dataDependenceEdge);
					pointerDependenceEdgeSet.add(dataDependenceEdge);
				}
			}

			// if it is an ampersand node, then potentially we may have to process some backwards data flow
			if(pointerFlow.taggedWith(XCSG.C.Provisional.Ampersand)) {
				Q target = Query.universe().edges("addressOf").predecessors(Common.toQ(pointerFlow));
				Q stackVariable = Query.universe().edges("identifier").successors(target);
				Q possibleTargetDefinitions = Query.universe().edges("identifier").predecessors(stackVariable);
				for(Node possibleTargetDefinition : possibleTargetDefinitions.eval().nodes()) {
					Node fromStatement = CommonQueries.getContainingControlFlowNode(possibleTargetDefinition);
					Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(AnalysisXCSG.BACKWARD_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, stackVariable.eval().nodes().one().getAttr(XCSG.name).toString());
					}
					dataDependenceEdgeSet.add(dataDependenceEdge);
					backwardDataDependenceEdgeSet.add(dataDependenceEdge);
				}
			}
		}

		if(!pointerDependenceEdgeSet.isEmpty()) {
			for(Edge pointerDependenceEdge : pointerDependenceEdgeSet) {
				Node fromStatement = pointerDependenceEdge.from();
				Node toStatement = pointerDependenceEdge.to();
				String dependentVariableName = pointerDependenceEdge.getAttr(AnalysisXCSG.DEPENDENT_VARIABLE).toString().replace("=", "");
				if(!toStatement.getAttr(XCSG.name).toString().contains(dependentVariableName)) {
					// We need to redirect this edge. This is not the right data dependence edge
					pointerDependenceEdge.untag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdgeSet.remove(pointerDependenceEdge);
					AtlasSet<Node> redirectionTargets = getRedirectionTargets(toStatement, dependentVariableName);
					for(Node redirectionTarget : redirectionTargets) {
						Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(redirectionTarget)).eval().edges().one();
						if(dataDependenceEdge == null){
							dataDependenceEdge = Graph.U.createEdge(fromStatement, redirectionTarget);
							dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
							dataDependenceEdge.tag(AnalysisXCSG.BACKWARD_DATA_DEPENDENCE_EDGE);
							dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
							dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, dependentVariableName);
						}
						dataDependenceEdgeSet.add(dataDependenceEdge);
						backwardDataDependenceEdgeSet.add(dataDependenceEdge);
					}
				}
			}
		}

		/**
		 * Interprocedural Data Dependence
		 * TODO: We need a better schema for this
		 */

		// Handle parameters
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
				Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.tag(AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, returnVariableName);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
				interDataDependenceEdgeSet.add(dataDependenceEdge);
			}
			AtlasSet<Node> parameterVariables = Common.toQ(callSite).predecessorsOn(passedToEdges).eval().nodes();
			for(Node parameterVariable : parameterVariables) {
				Node fromStatement = CommonQueries.getContainingControlFlowNode(callSite);
				Q targets = Common.toQ(parameterVariable).successorsOn(dataFlowEdges).successorsOn(dataFlowEdges);
				Node formalParameter = Common.toQ(parameterVariable).predecessorsOn(Query.universe().edges(XCSG.LocalDataFlow)).eval().nodes().one();
				String parameterVariableName = "";
				if(formalParameter != null) {
					parameterVariableName = formalParameter.getAttr(XCSG.name).toString();
				} else {
					parameterVariableName = parameterVariable.getAttr(XCSG.name).toString();
				}
				for(Node target : targets.eval().nodes()) {
					Node toStatement = CommonQueries.getContainingControlFlowNode(target);
					Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					Q dataDependenceEdges2 = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement));
					Edge dataDependenceEdge = dataDependenceEdges2.selectEdge(AnalysisXCSG.DEPENDENT_VARIABLE, parameterVariableName).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, parameterVariableName);
					}
					dataDependenceEdgeSet.add(dataDependenceEdge);
					interDataDependenceEdgeSet.add(dataDependenceEdge);
				}	
			}
		}

		// Handle Returns
		Q dfReturns = Common.toQ(dfg).nodes(XCSG.Return);
		for(Node dfReturn : dfReturns.eval().nodes()) {
			Node fromStatement = getStatement(dfReturn);
			String returnVariableName = dfReturn.getAttr(XCSG.name).toString().replaceAll("[()]", "");
			Log.info("Returned: " + returnVariableName);
			Q returnValue = Common.toQ(dfReturn).successorsOn(Query.universe().edges(XCSG.LocalDataFlow));
			Q trueTargets = interproceduralDataFlowEdges.successors(returnValue);
			for(Node trueTarget : trueTargets.eval().nodes()) {
				Node toStatement = getStatement(trueTarget);
				Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
				if(dataDependenceEdge == null){
					dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
					dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.tag(AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
					dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, returnVariableName);
				}
				dataDependenceEdgeSet.add(dataDependenceEdge);
				interDataDependenceEdgeSet.add(dataDependenceEdge);
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
					Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
					Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
					if(dataDependenceEdge == null){
						dataDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
						dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.tag(AnalysisXCSG.GLOBAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.GLOBAL_DATA_DEPENDENCE_EDGE);
						dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, globalVariableName);
					}
					dataDependenceEdgeSet.add(dataDependenceEdge);
				}
			}
		}

		// instance variable assignments
		Q instanceVariableAssignments = Common.toQ(dfg).nodes(XCSG.InstanceVariableAssignment);
		for(Node instanceVariableAssignment : instanceVariableAssignments.eval().nodes()) {
			Q instanceVariableAssignmentQ = Common.toQ(instanceVariableAssignment);
			Node use = instanceVariableAssignmentQ.predecessorsOn(Common.edges(XCSG.LocalDataFlow)).eval().nodes().one();
			Node instance = instanceVariableAssignmentQ.successorsOn(Common.edges(XCSG.InterproceduralDataFlow)).eval().nodes().one();
			Q dataDependenceEdges = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
			Edge dataDependenceEdge = dataDependenceEdges.betweenStep(Common.toQ(instanceVariableAssignment).parent(), Common.toQ(instance)).eval().edges().one();
			if(dataDependenceEdge == null){
				dataDependenceEdge = Graph.U.createEdge(instanceVariableAssignmentQ.parent().eval().nodes().one(), instance);
				dataDependenceEdge.tag(AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.tag(AnalysisXCSG.INTERPROCEDURAL_DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.DATA_DEPENDENCE_EDGE);
				dataDependenceEdge.putAttr(AnalysisXCSG.DEPENDENT_VARIABLE, use.getAttr(XCSG.name).toString());
			}
			dataDependenceEdgeSet.add(dataDependenceEdge);
			interDataDependenceEdgeSet.add(dataDependenceEdge);
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
		Q predecessors = Query.universe().edges(AnalysisXCSG.DATA_DEPENDENCE_EDGE).predecessors(Common.toQ(statement));		
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
