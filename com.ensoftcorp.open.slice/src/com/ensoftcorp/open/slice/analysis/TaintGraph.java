package com.ensoftcorp.open.slice.analysis;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
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
			ProgramDependenceGraph pdg = DependenceGraph.Factory.buildPDG(method);
			
			// taint graph is the forward taint of the source intersected with the reverse taint of the sink
			AtlasSet<GraphElement> sources = new AtlasHashSet<GraphElement>();
			sources.add(source);
			Q forwardTaintSlice = pdg.getSlice(SliceDirection.FORWARD, sources);
			AtlasSet<GraphElement> sinks = new AtlasHashSet<GraphElement>();
			sinks.add(sink);
			Q reverseTaintSlice = pdg.getSlice(SliceDirection.REVERSE, sinks);
			taintGraph = forwardTaintSlice.intersection(reverseTaintSlice).union(Common.toQ(source), Common.toQ(sink)).eval();
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
	
	public Highlighter getHighlighter(){
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(getSource()), Color.BLUE);
		h.highlight(Common.toQ(getSink()), Color.RED);
		return h;
	}
}
