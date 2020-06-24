package info.codesaway.bexcodecompare.diff;

public enum DiffTypeEnum implements DiffType {
	INSERT('+'), DELETE('-'), EQUAL(' '),
	/**
	 * Indicates that two lines are equal after normalization
	 */
	NORMALIZE('N'), MOVE('M', true), SUBSTITUTE('S', false, true), IGNORE('X'),

	/**
	 * Can be used to indicate a substitution which isn't similar
	 */
	REPLACEMENT('R', false, true);

	private final char tag;
	private final boolean isMove;
	private final boolean isSubstitution;

	private DiffTypeEnum(final char tag) {
		this(tag, false);
	}

	private DiffTypeEnum(final char tag, final boolean isMove) {
		this(tag, isMove, false);
	}

	private DiffTypeEnum(final char tag, final boolean isMove, final boolean isSubstitution) {
		this.tag = tag;
		this.isMove = isMove;
		this.isSubstitution = isSubstitution;
	}

	@Override
	public char getTag() {
		return this.tag;
	}

	@Override
	public boolean isMove() {
		return this.isMove;
	}

	@Override
	public boolean isSubstitution() {
		return this.isSubstitution;
	}
}
