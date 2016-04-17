package com.ensoftcorp.open.slice.analysis;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.query.Q;

public abstract class DependenceGraph {

	public static enum SliceDirection {
		FORWARD, REVERSE, BI_DIRECTIONAL
	}
	
	public abstract Q getGraph();
	
	public abstract Q getSlice(GraphElement origin, SliceDirection direction);
	
}
