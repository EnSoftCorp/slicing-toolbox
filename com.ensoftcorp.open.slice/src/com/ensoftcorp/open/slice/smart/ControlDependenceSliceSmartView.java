package com.ensoftcorp.open.slice.smart;

import java.awt.Color;
import java.io.IOException;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.commons.analysis.StandardQueries;
import com.ensoftcorp.open.commons.utilities.FormattedSourceCorrespondence;
import com.ensoftcorp.open.slice.analysis.ControlDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;

public class ControlDependenceSliceSmartView extends DependenceSliceSmartView {

	@Override
	public String getTitle() {
		return "Control Dependence Slice";
	}
	
	@Override
	protected DependenceGraph getDependenceGraph(Node method) {
		return DependenceGraph.Factory.buildCDG(method);
	}

}