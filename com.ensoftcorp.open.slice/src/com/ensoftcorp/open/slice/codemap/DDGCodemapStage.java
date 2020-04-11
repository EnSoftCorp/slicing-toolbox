package com.ensoftcorp.open.slice.codemap;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.slice.analysis.CDataDependenceGraph;
import com.ensoftcorp.open.slice.log.Log;
import com.ensoftcorp.open.slice.preferences.SlicePreferences;

public class DDGCodemapStage extends PrioritizedCodemapStage {

	public static final String IDENTIFIER = "com.ensoftcorp.open.slice.ddg";
	
	@Override
	public String getDisplayName() {
		return "Data Dependence Graph";
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String[] getCodemapStageDependencies() {
		// TODO Auto-generated method stub
		return new String[]{};
	}

	@Override
	public boolean performIndexing(IProgressMonitor monitor) {
		boolean runIndexer = SlicePreferences.isComputeProgramDependenceGraphsEnabled();
		if(runIndexer){
			Log.info("Computing Data Dependence Graphs...");
			for(Node function : Query.universe().nodes(XCSG.Function).eval().nodes()){
				new CDataDependenceGraph(CommonQueries.cfg(function).eval());
			}
		}
		return runIndexer;
	}

}
