package info.codesaway.bex.diff.substitution;

// TODO: rename class to something better
public final class SubstitutionDiffTypeValue implements SubstitutionDiffType {
	private final char symbol;
	private final boolean isMove;
	private final boolean shouldTreatAsNormalizedEqual;

	private final String name;

	public SubstitutionDiffTypeValue(final char symbol, final String name) {
		this(symbol, name, false, false);
	}

	public SubstitutionDiffTypeValue(final char symbol, final String name, final boolean isMove,
			final boolean shouldTreatAsNormalizedEqual) {
		this.symbol = symbol;
		this.isMove = isMove;
		this.shouldTreatAsNormalizedEqual = shouldTreatAsNormalizedEqual;

		this.name = name;
	}

	@Override
	public char getSymbol() {
		return this.symbol;
	}

	@Override
	public boolean isMove() {
		return this.isMove;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean shouldTreatAsNormalizedEqual() {
		return this.shouldTreatAsNormalizedEqual;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
