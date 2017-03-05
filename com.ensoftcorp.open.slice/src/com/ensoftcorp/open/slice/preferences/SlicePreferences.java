package com.ensoftcorp.open.slice.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.ensoftcorp.open.slice.Activator;
import com.ensoftcorp.open.slice.log.Log;

public class SlicePreferences extends AbstractPreferenceInitializer {

private static boolean initialized = false;
	
	/**
	 * Enable/disable decompiled loop identification
	 */
	public static final String COMPUTE_PROGRAM_DEPENDENCE_GRAPHS = "COMPUTE_PROGRAM_DEPENDENCE_GRAPHS";
	public static final Boolean COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT = false;
	private static boolean computeProgramDependenceGraphsValue = COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT;
	
	public static boolean isDecompiledLoopRecoveryEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return computeProgramDependenceGraphsValue;
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setDefault(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS, COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DEFAULT);
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			computeProgramDependenceGraphsValue = preferences.getBoolean(COMPUTE_PROGRAM_DEPENDENCE_GRAPHS);
		} catch (Exception e){
			Log.warning("Error accessing slicing preferences, using defaults...", e);
		}
		initialized = true;
	}
}