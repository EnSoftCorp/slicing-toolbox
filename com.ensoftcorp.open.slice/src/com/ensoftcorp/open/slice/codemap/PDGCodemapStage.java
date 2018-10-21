package com.ensoftcorp.open.slice.codemap;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.slice.analysis.DependenceGraph;
import com.ensoftcorp.open.slice.log.Log;
import com.ensoftcorp.open.slice.preferences.SlicePreferences;

/**
 * Builds the PDGs for each function
 * 
 * @author Ben Holland
 */
public class PDGCodemapStage extends PrioritizedCodemapStage {

	/**
	 * The unique identifier for the PDG codemap stage
	 */
	public static final String IDENTIFIER = "com.ensoftcorp.open.slice.pdg";
	
	@Override
	public String getDisplayName() {
		return "Program Dependence Graph";
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String[] getCodemapStageDependencies() {
		return new String[]{}; // no dependencies
	}

	@Override
	public boolean performIndexing(IProgressMonitor monitor) {
		boolean runIndexer = SlicePreferences.isComputeProgramDependenceGraphsEnabled();
		if(runIndexer){
			Log.info("Computing Program Dependence Graphs...");
			for(Node function : Query.universe().nodes(XCSG.Function).eval().nodes()){
				DependenceGraph.Factory.buildPDG(function);
			}
		}
		return runIndexer;
	}

}