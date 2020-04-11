package com.ensoftcorp.open.slice.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ensoftcorp.open.slice.preferences.SlicePreferences;

/**
 * UI for setting slice preferences
 * 
 * @author Ben Holland
 */
public class SlicePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DESCRIPTION = "Compute program dependence graphs";
	
	private static boolean changeListenerAdded = false;
	
	public SlicePreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		IPreferenceStore preferences = SlicePreferences.getPreferenceStore();
		setPreferenceStore(preferences);
		setDescription("Configure preferences for the Slicing Toolbox plugin.");
		
		// use to update cached values if user edits a preference
		if(!changeListenerAdded){
			getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
					SlicePreferences.loadPreferences();
				}
			});
			changeListenerAdded = true;
		}
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(SlicePreferences.COMPUTE_PROGRAM_DEPENDENCE_GRAPHS, "&" + COMPUTE_PROGRAM_DEPENDENCE_GRAPHS_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(SlicePreferences.COMPUTE_DATA_DEPENDENCE_GRAPHS, "Compute data dependence graphs", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
	}

}
