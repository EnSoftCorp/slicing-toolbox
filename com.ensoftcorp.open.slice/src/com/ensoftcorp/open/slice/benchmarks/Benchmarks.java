package com.ensoftcorp.open.slice.benchmarks;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.slice.analysis.ControlDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DataDependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.analysis.DependenceGraph.SliceDirection;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;
import com.ensoftcorp.open.commons.analysis.SetDefinitions;

public class Benchmarks {

	public static void evaluateCDG(File outputFile) throws Exception {
		FileWriter output = new FileWriter(outputFile);
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		
		output.write("Method,Graph Construction Time (ms),"
					+ "CDG Nodes,CDG Edges,"
					+ "Average Reverse Slice Time (ms),Average Reverse Slice Nodes,Average Reverse Slice Edges,"
					+ "Average Forward Slice Time (ms),Average Forward Slice Nodes,Average Forward Slice Edges,"
					+ "Average Bi-Directional Slice Time (ms),Average Bi-Directional Slice Nodes,Average Bi-Directional Slice Edges\n");
		
		AtlasSet<Node> methods = SetDefinitions.app().nodesTaggedWithAny(XCSG.Method).eval().nodes();
		AtlasSet<Node> methodsToIterate = new AtlasHashSet<Node>();
		methodsToIterate.addAll(methods);
		for(Node method : methodsToIterate){
			output.write(getQualifiedElementName(method));
			
			long start = System.nanoTime();
			ControlDependenceGraph cdg = DependenceGraph.Factory.buildCDG(method);
			long stop = System.nanoTime();
			double time = (stop-start)/1000.0/1000.0;
			
			Graph graph = cdg.getGraph().eval();
			
			output.write("," + decimalFormat.format(time) + "," + graph.nodes().size() + "," + graph.edges().size());
			
			// get average reverse slice size
			double reverseSliceTime = 0;
			long reverseSliceNodes = 0;
			long reverseSliceEdges = 0;
			for(GraphElement sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = cdg.getSlice(SliceDirection.REVERSE, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				reverseSliceTime += sliceTime;
				reverseSliceNodes += slice.nodes().size();
				reverseSliceEdges += slice.edges().size();
			}
			double averageReverseSliceTime = graph.nodes().size() == 0 ? 0 : (reverseSliceTime / ((double) graph.nodes().size()));
			double averageReverseSliceNodes = graph.nodes().size() == 0 ? 0 : (reverseSliceNodes / ((double) graph.nodes().size()));
			double averageReverseSliceEdges = graph.nodes().size() == 0 ? 0 : (reverseSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageReverseSliceTime) + "," + decimalFormat.format(averageReverseSliceNodes) + "," + decimalFormat.format(averageReverseSliceEdges));
			
			// get average forward slice size
			double forwardSliceTime = 0;
			long forwardSliceNodes = 0;
			long forwardSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = cdg.getSlice(SliceDirection.FORWARD, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				forwardSliceTime += sliceTime;
				forwardSliceNodes += slice.nodes().size();
				forwardSliceEdges += slice.edges().size();
			}
			double averageForwardSliceTime = graph.nodes().size() == 0 ? 0 : (forwardSliceTime / ((double) graph.nodes().size()));
			double averageForwardSliceNodes = graph.nodes().size() == 0 ? 0 : (forwardSliceNodes / ((double) graph.nodes().size()));
			double averageForwardSliceEdges = graph.nodes().size() == 0 ? 0 : (forwardSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageForwardSliceTime) + "," + decimalFormat.format(averageForwardSliceNodes) + "," + decimalFormat.format(averageForwardSliceEdges));
			
			// get average bi-directional slice size
			double bidirectionalSliceTime = 0;
			long bidirectionalSliceNodes = 0;
			long bidirectionalSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = cdg.getSlice(SliceDirection.BI_DIRECTIONAL, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				bidirectionalSliceTime += sliceTime;
				bidirectionalSliceNodes += slice.nodes().size();
				bidirectionalSliceEdges += slice.edges().size();
			}
			double averageBidirectionalSliceTime = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceTime / ((double) graph.nodes().size()));
			double averageBidirectionalSliceNodes = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceNodes / ((double) graph.nodes().size()));
			double averageBidirectionalSliceEdges = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageBidirectionalSliceTime) + "," + decimalFormat.format(averageBidirectionalSliceNodes) + "," + decimalFormat.format(averageBidirectionalSliceEdges));
			
			output.write("\n");
			output.flush();
		}
		output.close();
	}
	
	public static void evaluateDDG(File outputFile) throws Exception {
		FileWriter output = new FileWriter(outputFile);
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		
		output.write("Method,Graph Construction Time (ms),"
					+ "DDG Nodes,DDG Edges,"
					+ "Average Reverse Slice Time (ms),Average Reverse Slice Nodes,Average Reverse Slice Edges,"
					+ "Average Forward Slice Time (ms),Average Forward Slice Nodes,Average Forward Slice Edges,"
					+ "Average Bi-Directional Slice Time (ms),Average Bi-Directional Slice Nodes,Average Bi-Directional Slice Edges\n");
		
		AtlasSet<Node> methods = SetDefinitions.app().nodesTaggedWithAny(XCSG.Method).eval().nodes();
		AtlasSet<Node> methodsToIterate = new AtlasHashSet<Node>();
		methodsToIterate.addAll(methods);
		for(Node method : methodsToIterate){
			output.write(getQualifiedElementName(method));
			
			long start = System.nanoTime();
			DataDependenceGraph ddg = DependenceGraph.Factory.buildDDG(method);
			long stop = System.nanoTime();
			double time = (stop-start)/1000.0/1000.0;
			
			Graph graph = ddg.getGraph().eval();
			
			output.write("," + decimalFormat.format(time) + "," + graph.nodes().size() + "," + graph.edges().size());
			
			// get average reverse slice size
			double reverseSliceTime = 0;
			long reverseSliceNodes = 0;
			long reverseSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = ddg.getSlice(SliceDirection.REVERSE, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				reverseSliceTime += sliceTime;
				reverseSliceNodes += slice.nodes().size();
				reverseSliceEdges += slice.edges().size();
			}
			double averageReverseSliceTime = graph.nodes().size() == 0 ? 0 : (reverseSliceTime / ((double) graph.nodes().size()));
			double averageReverseSliceNodes = graph.nodes().size() == 0 ? 0 : (reverseSliceNodes / ((double) graph.nodes().size()));
			double averageReverseSliceEdges = graph.nodes().size() == 0 ? 0 : (reverseSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageReverseSliceTime) + "," + decimalFormat.format(averageReverseSliceNodes) + "," + decimalFormat.format(averageReverseSliceEdges));
			
			// get average forward slice size
			double forwardSliceTime = 0;
			long forwardSliceNodes = 0;
			long forwardSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = ddg.getSlice(SliceDirection.FORWARD, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				forwardSliceTime += sliceTime;
				forwardSliceNodes += slice.nodes().size();
				forwardSliceEdges += slice.edges().size();
			}
			double averageForwardSliceTime = graph.nodes().size() == 0 ? 0 : (forwardSliceTime / ((double) graph.nodes().size()));
			double averageForwardSliceNodes = graph.nodes().size() == 0 ? 0 : (forwardSliceNodes / ((double) graph.nodes().size()));
			double averageForwardSliceEdges = graph.nodes().size() == 0 ? 0 : (forwardSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageForwardSliceTime) + "," + decimalFormat.format(averageForwardSliceNodes) + "," + decimalFormat.format(averageForwardSliceEdges));
			
			// get average bi-directional slice size
			double bidirectionalSliceTime = 0;
			long bidirectionalSliceNodes = 0;
			long bidirectionalSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = ddg.getSlice(SliceDirection.BI_DIRECTIONAL, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				bidirectionalSliceTime += sliceTime;
				bidirectionalSliceNodes += slice.nodes().size();
				bidirectionalSliceEdges += slice.edges().size();
			}
			double averageBidirectionalSliceTime = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceTime / ((double) graph.nodes().size()));
			double averageBidirectionalSliceNodes = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceNodes / ((double) graph.nodes().size()));
			double averageBidirectionalSliceEdges = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageBidirectionalSliceTime) + "," + decimalFormat.format(averageBidirectionalSliceNodes) + "," + decimalFormat.format(averageBidirectionalSliceEdges));
			
			output.write("\n");
			output.flush();
		}
		output.close();
	}
	
	public static void evaluatePDG(File outputFile) throws Exception {
		FileWriter output = new FileWriter(outputFile);
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		
		output.write("Method,Graph Construction Time (ms),"
					+ "PDG Nodes,PDG Edges,"
					+ "Average Reverse Slice Time (ms),Average Reverse Slice Nodes,Average Reverse Slice Edges,"
					+ "Average Forward Slice Time (ms),Average Forward Slice Nodes,Average Forward Slice Edges,"
					+ "Average Bi-Directional Slice Time (ms),Average Bi-Directional Slice Nodes,Average Bi-Directional Slice Edges\n");
		
		AtlasSet<Node> methods = SetDefinitions.app().nodesTaggedWithAny(XCSG.Method).eval().nodes();
		AtlasSet<Node> methodsToIterate = new AtlasHashSet<Node>();
		methodsToIterate.addAll(methods);
		for(Node method : methodsToIterate){
			output.write(getQualifiedElementName(method));
			
			long start = System.nanoTime();
			ProgramDependenceGraph pdg = DependenceGraph.Factory.buildPDG(method);
			long stop = System.nanoTime();
			double time = (stop-start)/1000.0/1000.0;
			
			Graph graph = pdg.getGraph().eval();
			
			output.write("," + decimalFormat.format(time) + "," + graph.nodes().size() + "," + graph.edges().size());
			
			// get average reverse slice size
			double reverseSliceTime = 0;
			long reverseSliceNodes = 0;
			long reverseSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = pdg.getSlice(SliceDirection.REVERSE, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				reverseSliceTime += sliceTime;
				reverseSliceNodes += slice.nodes().size();
				reverseSliceEdges += slice.edges().size();
			}
			double averageReverseSliceTime = graph.nodes().size() == 0 ? 0 : (reverseSliceTime / ((double) graph.nodes().size()));
			double averageReverseSliceNodes = graph.nodes().size() == 0 ? 0 : (reverseSliceNodes / ((double) graph.nodes().size()));
			double averageReverseSliceEdges = graph.nodes().size() == 0 ? 0 : (reverseSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageReverseSliceTime) + "," + decimalFormat.format(averageReverseSliceNodes) + "," + decimalFormat.format(averageReverseSliceEdges));
			
			// get average forward slice size
			double forwardSliceTime = 0;
			long forwardSliceNodes = 0;
			long forwardSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = pdg.getSlice(SliceDirection.FORWARD, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				forwardSliceTime += sliceTime;
				forwardSliceNodes += slice.nodes().size();
				forwardSliceEdges += slice.edges().size();
			}
			double averageForwardSliceTime = graph.nodes().size() == 0 ? 0 : (forwardSliceTime / ((double) graph.nodes().size()));
			double averageForwardSliceNodes = graph.nodes().size() == 0 ? 0 : (forwardSliceNodes / ((double) graph.nodes().size()));
			double averageForwardSliceEdges = graph.nodes().size() == 0 ? 0 : (forwardSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageForwardSliceTime) + "," + decimalFormat.format(averageForwardSliceNodes) + "," + decimalFormat.format(averageForwardSliceEdges));
			
			// get average bi-directional slice size
			double bidirectionalSliceTime = 0;
			long bidirectionalSliceNodes = 0;
			long bidirectionalSliceEdges = 0;
			for(Node sliceCriterion : graph.nodes()){
				AtlasSet<Node> criteria = new AtlasHashSet<Node>();
				criteria.add(sliceCriterion);
				long startSlice = System.nanoTime();
				Graph slice = pdg.getSlice(SliceDirection.BI_DIRECTIONAL, criteria).eval();
				long stopSlice = System.nanoTime();
				double sliceTime = (stopSlice-startSlice)/1000.0/1000.0;
				bidirectionalSliceTime += sliceTime;
				bidirectionalSliceNodes += slice.nodes().size();
				bidirectionalSliceEdges += slice.edges().size();
			}
			double averageBidirectionalSliceTime = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceTime / ((double) graph.nodes().size()));
			double averageBidirectionalSliceNodes = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceNodes / ((double) graph.nodes().size()));
			double averageBidirectionalSliceEdges = graph.nodes().size() == 0 ? 0 : (bidirectionalSliceEdges / ((double) graph.nodes().size()));
			output.write("," + decimalFormat.format(averageBidirectionalSliceTime) + "," + decimalFormat.format(averageBidirectionalSliceNodes) + "," + decimalFormat.format(averageBidirectionalSliceEdges));
			
			output.write("\n");
			output.flush();
		}
		output.close();
	}
	
	private static String getQualifiedElementName(Node graphElement) {
		String name = graphElement.getAttr(XCSG.name).toString();
		// qualify the element with structural information
		Q containsEdges = Common.universe().edgesTaggedWithAny(XCSG.Contains);
		Node parent = containsEdges.predecessors(Common.toQ(graphElement)).eval().nodes().getFirst();
		while (parent != null) {
			// skip adding qualified part for default package
			if (!(parent.tags().contains(XCSG.Package) && parent.getAttr(XCSG.name).toString().equals(""))) {
				name = parent.getAttr(XCSG.name).toString() + "." + name;
			}
			parent = containsEdges.predecessors(Common.toQ(parent)).eval().nodes().getFirst();
		}
		return name;
	}
}
