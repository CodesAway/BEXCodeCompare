package info.codesaway.bexcodecompare.diff;

public enum LineChangeType {
	/**
	 * Indicates a line of code which has been commented out
	 */
	COMMENTED_OUT,

	/**
	 * Indicates a line of code which has been uncommented
	 */
	UNCOMMENTED,

	/**
	 * Indicates a line of code which is a comment (before and after change)
	 */
	COMMENT(ImpactType.NONE),

	/**
	 * Indicates a line of code which is an added comment
	 */
	ADDED_COMMENT(ImpactType.NONE),

	BLANK_LINE(ImpactType.NONE),

	UNKNOWN,

	ADDED_GENERICS(ImpactType.NONE),

	REMOVED_CASTING(ImpactType.NONE),

	DEPRECATED(ImpactType.NONE),

	OVERRIDE(ImpactType.NONE),

	SUPPRESS_WARNINGS(ImpactType.NONE),

	REMOVED_SUPPRESS_WARNINGS(ImpactType.NONE),

	GET_DB_INSTANCE(ImpactType.NONE),

	GET_FABM_INSTANCE(ImpactType.NONE),

	ADDED_THIS_KEYWORD(ImpactType.NONE),

	ADDED_JClaretyConstants_CLASS_NAME(ImpactType.NONE),

	;

	private final ImpactType impactType;

	private LineChangeType() {
		this(ImpactType.UNKNOWN);
	}

	private LineChangeType(final ImpactType impactType) {
		this.impactType = impactType;
	}

	public ImpactType getImpactType() {
		return this.impactType;
	};
}
