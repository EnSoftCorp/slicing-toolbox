package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ProgramDependenceGraph extends DependenceGraph {

	private ControlDependenceGraph cdg;
	private FlowDependenceGraph fdg;
	
	public ProgramDependenceGraph(Graph cfg, Graph dfg){
		this.cdg = new ControlDependenceGraph(cfg);
		this.fdg = new FlowDependenceGraph(dfg);
	}
	
	@Override
	public Q getGraph() {
		return cdg.getGraph().union(fdg.getGraph());
	}

	@Override
	public Q getSlice(GraphElement dataFlowNode, SliceDirection direction) {
		AtlasSet<GraphElement> result = new AtlasHashSet<GraphElement>();
		
		// get the CDG and FDG slices
		GraphElement controlFlowNode = Common.toQ(dataFlowNode).containers().nodesTaggedWithAny(XCSG.ControlFlow_Node).eval().nodes().getFirst();
		Q cdgSlice = cdg.getSlice(controlFlowNode, direction);
		Q fdgSlice = fdg.getSlice(dataFlowNode, direction);
		
		// intersect the control flow of the CDG and the corresponding FDG control flow nodes
		Q pdgCF = cdgSlice.intersection(fdgSlice.containers().nodesTaggedWithAny(XCSG.ControlFlow_Node));
		Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
		pdgCF = pdgCF.induce(controlFlowEdges);
		Graph pdgCFG = pdgCF.eval();
		result.addAll(pdgCFG.nodes());
		result.addAll(pdgCFG.edges());
		
		// intersect the data flow nodes in the FDG and the nodes contained by statements in the CDG
		Q pdgDF = fdgSlice.intersection(cdgSlice.contained().nodesTaggedWithAny(XCSG.DataFlow_Node));
		Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
		pdgDF = pdgDF.induce(dataFlowEdges);
		Graph pdgDFG = pdgDF.eval();
		result.addAll(pdgDFG.nodes());
		result.addAll(pdgDFG.edges());
		
		return Common.toQ(result);
	}

}
