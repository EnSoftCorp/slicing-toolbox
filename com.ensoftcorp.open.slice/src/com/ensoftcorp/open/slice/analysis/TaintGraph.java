package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.toolbox.commons.analysis.utils.StandardQueries;

/**
 * A program dependence graph based taint graph (also known as chopping)
 *
 * @author Ben Holland
 */
public class TaintGraph {

	private GraphElement source;
	private GraphElement sink;
	
	private Graph taintGraph = Common.empty().eval();
	
	public TaintGraph(GraphElement source, GraphElement sink){
		this.source = source;
		this.sink = sink;
		
		GraphElement sourceMethod = StandardQueries.getContainingMethod(source);
		GraphElement sinkMethod = StandardQueries.getContainingMethod(sink);
		
		// only considering intra-procedural case
		if(sourceMethod.equals(sinkMethod)){
			GraphElement method = sourceMethod;
			
			// build program dependence graph
			Q controlFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.ControlFlow_Edge);
			Q cfg = controlFlowEdges.forward(Common.toQ(method).contained().nodesTaggedWithAny(XCSG.controlFlowRoot));
			
			Q dataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.DataFlow_Edge);
			Q dfg = Common.toQ(method).contained().nodesTaggedWithAny(XCSG.DataFlow_Node).induce(dataFlowEdges);
			
			ProgramDependenceGraph pdg = new ProgramDependenceGraph(cfg.eval(), dfg.eval());
			
			// taint graph is the forward taint of the source intersected with the reverse taint of the sink
			Q forwardTaintSlice = pdg.getSlice(source, SliceDirection.FORWARD);
			Q reverseTaintSlice = pdg.getSlice(sink, SliceDirection.REVERSE);
			taintGraph = forwardTaintSlice.intersection(reverseTaintSlice).eval();
		}
	}

	public GraphElement getSource() {
		return source;
	}

	public GraphElement getSink() {
		return sink;
	}
	
	public Q getGraph(){
		return Common.toQ(taintGraph);
	}
	
}
