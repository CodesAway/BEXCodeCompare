package info.codesaway.bex.diff.substitution;

// TODO: rename class to something better
public class SubstitutionDiffTypeValue implements SubstitutionDiffType {
	private final char tag;
	private final boolean isMove;
	private final boolean shouldTreatAsNormalizedEqual;

	private final String name;

	public SubstitutionDiffTypeValue(final char tag, final String name) {
		this(tag, name, false, false);
	}

	public SubstitutionDiffTypeValue(final char tag, final String name, final boolean isMove,
			final boolean shouldTreatAsNormalizedEqual) {
		this.tag = tag;
		this.isMove = isMove;
		this.shouldTreatAsNormalizedEqual = shouldTreatAsNormalizedEqual;

		this.name = name;
	}

	@Override
	public char getTag() {
		return this.tag;
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
