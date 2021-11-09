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
import com.ensoftcorp.open.commons.algorithms.DominanceAnalysis;
import com.ensoftcorp.open.commons.algorithms.UniqueEntryExitControlFlowGraph;
import com.ensoftcorp.open.commons.algorithms.UniqueEntryExitGraph;
import com.ensoftcorp.open.commons.preferences.CommonsPreferences;
import com.ensoftcorp.open.slice.log.Log;
import com.ensoftcorp.open.slice.xcsg.AnalysisXCSG;

/**
 * Constructs the Control Dependence Graph (CGD) from a given Control Flow Graph (CFG)
 * 
 * Instead of computing control dependence using the Ferrante, Ottenstein, and Warren algorithm
 * this uses the post-dominance frontier to compute the dominance edges.
 * 
 * Reference: 
 * 1) http://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis4.pdf
 * 2) https://www.cc.gatech.edu/~harrold/6340/cs6340_fall2009/Slides/BasicAnalysis5.pdf
 * 3) http://www.cs.utexas.edu/~pingali/CS380C/2016-fall/lectures/Dominators.pdf
 * 
 * @author Ben Holland
 */
public class ControlDependenceGraph extends DependenceGraph {

	private Graph cfg;
	private Graph cdg;

	public ControlDependenceGraph(Graph cfg){
		this.cfg = cfg;
		this.cdg = Common.empty().eval();
	}

	public void create() {
		// sanity checks
		if(cfg.nodes().isEmpty() || cfg.edges().isEmpty()){
			return;
		}

		// compute dominance on-demand if needed
		if(!CommonsPreferences.isComputeControlFlowGraphDominanceEnabled()) {
			AtlasSet<Node> roots = Common.toQ(cfg).nodes(XCSG.controlFlowRoot).eval().nodes();
			AtlasSet<Node> exits = Common.toQ(cfg).nodes(XCSG.controlFlowExitPoint).eval().nodes();
			if(cfg.nodes().isEmpty() || roots.isEmpty() || exits.isEmpty()){
				// nothing to compute
			} else {
				try {
					UniqueEntryExitGraph uexg = new UniqueEntryExitControlFlowGraph(cfg, roots, exits, CommonsPreferences.isMasterEntryExitContainmentRelationshipsEnabled());
					DominanceAnalysis.computeDominance(uexg);
				} catch (Exception e){
					Log.error("Error computing control flow graph dominance tree", e);
				}
			}
		}

		AtlasSet<Edge> controlDependenceEdgeSet = new AtlasHashSet<Edge>();
		AtlasSet<Edge> dominanceFrontierEdges = DominanceAnalysis.getPostDominanceFrontierEdges().eval().edges();

		// for each edge in the dominance frontier edges (x --pdomf--> y)
		// find or create a control dependence edge from the successor to the predecessor (y --control-dependence--> x)
		for(Edge dominanceFrontierEdge : dominanceFrontierEdges) {
			controlDependenceEdgeSet.add(findOrCreateControlDependenceEdge(dominanceFrontierEdge.to(), dominanceFrontierEdge.from()));
		}

		this.cdg = Common.toQ(controlDependenceEdgeSet).eval();
	}

	/**
	 * Finds or creates a control dependence edge
	 * @param controlDependenceEdgeSet
	 * @param fromStatement
	 * @param toStatement
	 */
	private Edge findOrCreateControlDependenceEdge(Node fromStatement, Node toStatement) {
		Q controlDependenceEdges = Query.universe().edges(AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
		Edge controlDependenceEdge = controlDependenceEdges.betweenStep(Common.toQ(fromStatement), Common.toQ(toStatement)).eval().edges().one();
		if(controlDependenceEdge == null){
			controlDependenceEdge = Graph.U.createEdge(fromStatement, toStatement);
			controlDependenceEdge.tag(AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
			controlDependenceEdge.putAttr(XCSG.name, AnalysisXCSG.CONTROL_DEPENDENCE_EDGE);
		}
		return controlDependenceEdge;
	}

	public Q getControlFlowGraph(){
		return Common.toQ(cfg);
	}

	public Q getGraph(){
		return Common.toQ(cdg);
	}

}
