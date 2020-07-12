package info.codesaway.bex.diff.substitution;

import java.util.Objects;

/**
 * Indicates a substitution where either the left line fully contains the right line or the right line fully contains the left line
 */
// TODO: give better name
// Maybe SubstitutionFullyContained
public final class SubstitutionContainsDiffType implements SubstitutionDiffType {
	private final String prefix;
	private final Direction direction;
	private final String suffix;

	public SubstitutionContainsDiffType(final String prefix, final Direction direction, final String suffix) {
		this.prefix = prefix;
		this.direction = direction;
		this.suffix = suffix;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public Direction getDirection() {
		return this.direction;
	}

	public String getSuffix() {
		return this.suffix;
	}

	@Override
	public String toString() {
		return this.prefix + this.direction + this.suffix;
	}

	@Override
	public char getSymbol() {
		// 'S' to indicate a substitution
		return 'S';
	}

	@Override
	public boolean isMove() {
		return false;
	}

	public static enum Direction {
		LEFT_CONTAINS_RIGHT, RIGHT_CONTAINS_LEFT
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.direction, this.prefix, this.suffix);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		SubstitutionContainsDiffType other = (SubstitutionContainsDiffType) obj;
		return this.direction == other.direction && Objects.equals(this.prefix, other.prefix)
				&& Objects.equals(this.suffix, other.suffix);
	}
}
