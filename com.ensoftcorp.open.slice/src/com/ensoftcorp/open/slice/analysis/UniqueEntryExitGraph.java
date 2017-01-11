package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;

public interface UniqueEntryExitGraph {

	/**
	 * Returns the predecessors of a given node
	 */
	public AtlasSet<Node> getPredecessors(Node node);

	/**
	 * Returns the successors of a given node
	 */
	public AtlasSet<Node> getSuccessors(Node node);

	/**
	 * Returns the master entry node
	 */
	public Node getEntryNode();

	/**
	 * Returns the master exit node
	 */
	public Node getExitNode();

	public AtlasSet<Node> nodes();

	public AtlasSet<Edge> edges();
}