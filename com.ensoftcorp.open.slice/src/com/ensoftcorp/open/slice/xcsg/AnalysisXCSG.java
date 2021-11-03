package com.ensoftcorp.open.slice.xcsg;

public interface AnalysisXCSG {
	
	/**
	 * Used to tag the edges between nodes that contain a data dependence
	 */
	public static final String DATA_DEPENDENCE_EDGE = "data-dependence";

	/**
	 * Used to tag the edges between nodes that contain a data dependence due to a pointer
	 */
	public static final String POINTER_DEPENDENCE_EDGE = "pointer-dependence";

	/**
	 * Used to tag the edges between nodes that contain a backwards data dependence
	 */
	public static final String BACKWARD_DATA_DEPENDENCE_EDGE = "backward-data-dependence";

	public static final String GLOBAL_DATA_DEPENDENCE_EDGE = "global-data-dependence";

	/**
	 * Used to tag the edges representing interprocedural data dependence
	 */
	public static final String INTERPROCEDURAL_DATA_DEPENDENCE_EDGE = "data-dependence (inter)";

	/**
	 * Used to simulate the implict data dependency from initialization to instantiation
	 */
	public static final String JIMPLE_INITIALIZATION_DATA_DEPENDENCE_EDGE = "jimple-initialization-data-dependence";

	/**
	 * Used to identify the dependent variable
	 */
	public static final String DEPENDENT_VARIABLE = "dependent-variable";

}
