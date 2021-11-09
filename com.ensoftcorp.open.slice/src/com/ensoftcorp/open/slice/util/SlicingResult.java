package com.ensoftcorp.open.slice.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class SlicingResult {

	private Q controlDependenceEdges;
	private Q dataDependenceEdges;
	private Q programDependenceEdges;
	private Q pdg;

	public SlicingResult() {
		this(false);
	}

	public SlicingResult(boolean interProcedural) {
		this.controlDependenceEdges = Query.universe().edges("control-dependence");
		if(interProcedural) {
			this.dataDependenceEdges = Query.universe().edges("data-dependence","data-dependence (inter)");
		} else {
			this.dataDependenceEdges = Query.universe().edges("data-dependence");
		}
		this.programDependenceEdges = this.controlDependenceEdges.union(this.dataDependenceEdges);
		this.pdg = Query.empty();
	}

	public void compute(Q selected, String direction) {

		Q selectedControlFlow = getContainingControlFlowNodes(selected);

		if(direction == "forward") {
			this.pdg = programDependenceEdges.forward(selectedControlFlow);
		} else if(direction == "reverse") {
			this.pdg = programDependenceEdges.reverse(selectedControlFlow);
		} else {
			return;
		}

	}

	public void exportToFile(String filename) {
		String result = this.printResult();
		try {
			Path filePath = Paths.get(filename);
			Path f = Files.createFile(filePath);
			BufferedWriter w = new BufferedWriter(new FileWriter(f.toFile()));
			w.write(result);
			w.close();
		} catch(FileAlreadyExistsException e) {
			try {
				File f = new File(filename);
				BufferedWriter w = new BufferedWriter(new FileWriter(f));
				w.write(result);
				w.close();
			} catch(IOException e2) {
				e2.printStackTrace();
			}		
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public String printResult() {

		Q pdgStatements = this.pdg.nodes(XCSG.ControlFlow_Node);
		Q pdgGlobals = this.pdg.difference(pdgStatements);
		AtlasSet<Node> pdgNodes = pdgStatements.eval().nodes();
		// case 20870: Expected to be ordered by line numbers
		Map<IFile, List<Node>> sortedStatements = sort(pdgNodes);
		AtlasSet<Node> pdgGlobalNodes = pdgGlobals.eval().nodes();
		AtlasSet<Edge> pdgEdges = this.pdg.eval().edges();

		StringBuilder sb = new StringBuilder();
		sb.append("\n-----------------------\n");
		sb.append("Program Slice Statements:\n\n");
		for(IFile file: sortedStatements.keySet()) {
			sb.append(file.toString() + "\n");
			List<Node> l = sortedStatements.get(file);
			for(Node n: l) {
				Node functionN = Common.toQ(n).containers().nodes(XCSG.Function).eval().nodes().one();
				SourceCorrespondence sc = (SourceCorrespondence) n.getAttr(XCSG.sourceCorrespondence);
				sb.append("Function: " + functionN.getAttr(XCSG.name) + " ");
				sb.append("Line " + sc.startLine + ": ");
				sb.append(n.getAttr(XCSG.name) + "\n");
			}
			sb.append("\n");
		}

		sb.append("\n-----------------------\n");
		sb.append("External variables:\n\n");
		for(Node pdgGlobal: pdgGlobalNodes) {
			SourceCorrespondence sc = (SourceCorrespondence) pdgGlobal.getAttr(XCSG.sourceCorrespondence);
			sb.append(pdgGlobal.getAttr(XCSG.name) + "\n");
			sb.append(sc.sourceFile.toString() + "\n");
			sb.append("Line " + sc.startLine + "\n");
		}

		sb.append("\n-----------------------\n");
		sb.append("Program Slice Edges:\n\n");
		for(Edge pdgEdge: pdgEdges) {
			Node from = pdgEdge.from();
			Node to = pdgEdge.to();
			sb.append(pdgEdge.getAttr(XCSG.name) + ": " + from.getAttr(XCSG.name) + " -> " + to.getAttr(XCSG.name) + "\n");
		}
		return sb.toString();
	}

	public Q showGraph() {
		return this.pdg;
	}

	private Map<IFile, List<Node>> sort(AtlasSet<Node> pdgStatements) {

		Map<IFile,List<Node>> map = new HashMap<IFile,List<Node>>();
		for(Node pdgStatement: pdgStatements) {
			SourceCorrespondence sc = (SourceCorrespondence) pdgStatement.getAttr(XCSG.sourceCorrespondence);
			IFile file = sc.sourceFile;
			if(map.containsKey(file)) {
				map.get(file).add(pdgStatement);
			} else {
				List<Node> a = new ArrayList<Node>();
				a.add(pdgStatement);
				map.put(file,a);
			}
		}

		for(IFile file: map.keySet()) {
			List<Node> a = map.get(file);
			Collections.sort(a,new Comparator<Node>() {
				@Override
				public int compare(Node a, Node b) {
					SourceCorrespondence sc1 = (SourceCorrespondence) a.getAttr(XCSG.sourceCorrespondence);
					SourceCorrespondence sc2 = (SourceCorrespondence) b.getAttr(XCSG.sourceCorrespondence);
					Integer l1 = sc1.startLine;
					Integer l2 = sc2.startLine;
					return l1.compareTo(l2);
				}
			});
		}

		return map;

	}

	private Q getContainingControlFlowNodes(Q selected) {
		return selected.containers().nodes(XCSG.ControlFlow_Node);
	}

}
