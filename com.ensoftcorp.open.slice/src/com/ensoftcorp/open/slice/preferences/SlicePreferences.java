package com.ensoftcorp.open.slice.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.ensoftcorp.open.slice.Activator;
import com.ensoftcorp.open.slice.log.Log;

public class SlicePreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;
		
	/**
	 * Returns the preference store used for these preferences
	 * @return
	 */
	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/**
	 * Enable/disable Program Dependence Graph Computation
	 */
	public static final String COMPUTE_PROGRAM_DEPENDENCE_GRAPHS = "COMPUTE_PROGRAM_DEPENDENCE_GRAPHS";
	public static final Boolean COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT = false;
	private static boolean computeProgramDependenceGraphsValue = COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT;
	
	/**
	 * Enable/disable Data Dependence Graph Computation
	 */
	public static final String COMPUTE_DATA_DEPENDENCE_GRAPHS = "COMPUTE_DATA_DEPENDENCE_GRAPHS";
	public static final Boolean COMPUTE_DATA_DEPENDENCE_GRAPHS_DEFAULT = false;
	private static boolean computeDataDependenceGraphsValue = COMPUTE_DATA_DEPENDENCE_GRAPHS_DEFAULT;
	
	
	/**
	 * Configures inference rule logging
	 */
	public static void enableComputeProgramDependenceGraphs(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS, enabled);
		loadPreferences();
	}
	
	public static boolean isComputeProgramDependenceGraphsEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return computeProgramDependenceGraphsValue;
	}
	
	public static void enableComputeDataDependenceGraphs(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(COMPUTE_DATA_DEPENDENCE_GRAPHS, enabled);
		loadPreferences();
	}
	
	public static boolean isComputeDataDependenceGraphsEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return computeDataDependenceGraphsValue;
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setDefault(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS, COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT);
		preferences.setDefault(COMPUTE_DATA_DEPENDENCE_GRAPHS, COMPUTE_DATA_DEPENDENCE_GRAPHS_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS, COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT);
		preferences.setDefault(COMPUTE_DATA_DEPENDENCE_GRAPHS, COMPUTE_DATA_DEPENDENCE_GRAPHS_DEFAULT);
		loadPreferences();
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			computeProgramDependenceGraphsValue = preferences.getBoolean(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS);
			computeDataDependenceGraphsValue = preferences.getBoolean(COMPUTE_DATA_DEPENDENCE_GRAPHS);
		} catch (Exception e){
			Log.warning("Error accessing slicing preferences, using defaults...", e);
		}
		initialized = true;
	}
}