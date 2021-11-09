package com.ensoftcorp.open.slice.xcsg;

import com.ensoftcorp.open.commons.xcsg.XCSG_Extension;

public interface AnalysisXCSG {
	
	/**
	 * Used to tag the edges between nodes that contain a control dependence
	 */
	public static final String CONTROL_DEPENDENCE_EDGE = "control-dependence";

	public static final String AUGMENTED_CFG_ENTRY = "cfg-entry";
	public static final String AUGMENTED_CFG_EXIT = "cfg-exit";
	
	public static final String AUGMENTATION_NAME = "augmentation";
	public static final String AUGMENTATION_NODE = "augmentation-node";
	public static final String AUGMENTATION_EDGE = "augmentation-edge";
	
	/**
	 * Used to tag the edges between nodes that contain a data dependence
	 */
	@XCSG_Extension
	public static final String DATA_DEPENDENCE_EDGE = "data-dependence";

	/**
	 * Used to tag the edges between nodes that contain a data dependence due to a pointer
	 */
	@XCSG_Extension
	public static final String POINTER_DEPENDENCE_EDGE = "pointer-dependence";

	/**
	 * Used to tag the edges between nodes that contain a backwards data dependence
	 */
	@XCSG_Extension
	public static final String BACKWARD_DATA_DEPENDENCE_EDGE = "backward-data-dependence";

	/**
	 * Used to tag the edges representing global data dependence
	 */
	@XCSG_Extension
	public static final String GLOBAL_DATA_DEPENDENCE_EDGE = "global-data-dependence";

	/**
	 * Used to tag the edges representing interprocedural data dependence
	 */
	public static final String INTERPROCEDURAL_DATA_DEPENDENCE_EDGE = "data-dependence (inter)";

	/**
	 * Used to simulate the implict data dependency from initialization to instantiation
	 */
	@XCSG_Extension
	public static final String JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE = "jimple-initialization-data-dependence";

	/**
	 * Used to identify the dependent variable
	 */
	@XCSG_Extension
	public static final String DEPENDENT_VARIABLE = "dependent-variable";

}
