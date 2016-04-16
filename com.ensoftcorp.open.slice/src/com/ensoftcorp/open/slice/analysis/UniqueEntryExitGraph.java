package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;

public interface UniqueEntryExitGraph {

	/**
	 * Returns the predecessors of a given node
	 */
	public AtlasSet<GraphElement> getPredecessors(GraphElement node);

	/**
	 * Returns the successors of a given node
	 */
	public AtlasSet<GraphElement> getSuccessors(GraphElement node);

	/**
	 * Returns the master entry node
	 */
	public GraphElement getEntryNode();

	/**
	 * Returns the master exit node
	 */
	public GraphElement getExitNode();

	public AtlasSet<GraphElement> nodes();

	public AtlasSet<GraphElement> edges();
}