package com.ensoftcorp.open.slice.analysis;

import java.awt.Color;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.IMarkup;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.commons.analysis.StandardQueries;

/**
 * A program dependence graph based taint graph (also known as chopping)
 *
 * @author Ben Holland
 */
public class TaintGraph {

	private Node source;
	private Node sink;
	
	private Graph taintGraph = Common.empty().eval();
	
	public TaintGraph(Node source, Node sink){
		this.source = source;
		this.sink = sink;
		
		Node sourceFunction = StandardQueries.getContainingFunction(source);
		Node sinkFunction = StandardQueries.getContainingFunction(sink);
		
		// only considering intra-procedural case
		if(sourceFunction.equals(sinkFunction)){
			Node function = sourceFunction;
			
			// build program dependence graph
			ProgramDependenceGraph pdg = DependenceGraph.Factory.buildPDG(function);
			
			// taint graph is the forward taint of the source intersected with the reverse taint of the sink
//			AtlasSet<Node> sources = new AtlasHashSet<Node>();
//			sources.add(source);
//			Q forwardTaintSlice = pdg.getSlice(SliceDirection.FORWARD, sources);
//			AtlasSet<Node> sinks = new AtlasHashSet<Node>();
//			sinks.add(sink);
//			Q reverseTaintSlice = pdg.getSlice(SliceDirection.REVERSE, sinks);
//			taintGraph = forwardTaintSlice.intersection(reverseTaintSlice).union(Common.toQ(source), Common.toQ(sink)).eval();
			
			// Atlas between operation is much more efficient
			taintGraph = pdg.getGraph().between(Common.toQ(source), Common.toQ(sink)).eval();
		}
	}

	public Node getSource() {
		return source;
	}

	public Node getSink() {
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
	
	public IMarkup getMarkup(){
		Highlighter h = new Highlighter();
		h.highlight(Common.toQ(getSource()), Color.BLUE);
		h.highlight(Common.toQ(getSink()), Color.RED);
		return new MarkupFromH(h);
	}
}
