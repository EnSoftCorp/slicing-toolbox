package com.ensoftcorp.open.slice.ui.codepainter;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.codepainter.ColorPalette;
import com.ensoftcorp.open.slice.analysis.ControlDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph;

public class ProgramDependenceColorPalette extends ColorPalette {

	public static final Color CONTROL_DEPENDENCE_COLOR = Color.YELLOW.darker().darker();
	public static final Color DATA_DEPENDENCE_COLOR = Color.GREEN.darker();
	
	private Map<Edge, Color> edgeColors = new HashMap<Edge,Color>();
	
	@Override
	protected void canvasChanged() {
		edgeColors.clear();
		
		// need to expand the canvas for frontier edges
		// expand to the full function of any statements on the canvas
		Q canvasStatements = Common.toQ(canvas).nodes(XCSG.ControlFlow_Node);
		Q fullCanvasStatements = CommonQueries.cfg(CommonQueries.getContainingFunctions(canvasStatements)).nodes(XCSG.ControlFlow_Node);
		Q controlDependenceEdges = fullCanvasStatements.induce(Common.universe().edges(ControlDependenceGraph.CONTROL_DEPENDENCE_EDGE));
		Q dataDependenceEdges = fullCanvasStatements.induce(Common.universe().edges(DataDependenceGraph.DATA_DEPENDENCE_EDGE));

		// color the control dependence edges
		for(Edge edge : controlDependenceEdges.eval().edges()){
			edgeColors.put(edge, CONTROL_DEPENDENCE_COLOR);
		}
		
		// color the data dependence edges
		for(Edge edge : dataDependenceEdges.eval().edges()){
			edgeColors.put(edge, DATA_DEPENDENCE_COLOR);
		}
	}

	@Override
	public String getName() {
		return "Program Dependence Edge Color Palette";
	}

	@Override
	public String getDescription() {
		return "A color scheme to differentiate control and data dependence edges.";
	}

	@Override
	public Map<Node, Color> getNodeColors() {
		return new HashMap<Node,Color>();
	}

	@Override
	public Map<Edge, Color> getEdgeColors() {
		return new HashMap<Edge,Color>(edgeColors);
	}

	@Override
	public Map<Color, String> getNodeColorLegend() {
		return new HashMap<Color,String>();
	}

	@Override
	public Map<Color, String> getEdgeColorLegend() {
		HashMap<Color,String> legend = new HashMap<Color,String>();
		legend.put(CONTROL_DEPENDENCE_COLOR, "Control Dependence");
		legend.put(DATA_DEPENDENCE_COLOR, "Data Dependence");
		return legend;
	}

}
