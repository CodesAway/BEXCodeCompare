package info.codesaway.bexcodecompare.diff;

/**
 * Indicates a substitution where either the old line fully contains the new line or the new line fully contains the old line
 *
 * @return
 * @since
 * <pre> Change History
 * ========================================================================================
 * Version  Change #        Developer           Date        Description
 * =======  =============== =================== ==========  ===============================
 * TRS.01T                  Amy Brennan-Luna    03/26/2019  Initial coding
 *</pre>***********************************************************************************
 */
public class SubstitutionContainsDiffType implements DiffType {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.direction == null) ? 0 : this.direction.hashCode());
		result = prime * result + ((this.prefix == null) ? 0 : this.prefix.hashCode());
		result = prime * result + ((this.suffix == null) ? 0 : this.suffix.hashCode());
		return result;
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
		if (this.direction != other.direction) {
			return false;
		}
		if (this.prefix == null) {
			if (other.prefix != null) {
				return false;
			}
		} else if (!this.prefix.equals(other.prefix)) {
			return false;
		}
		if (this.suffix == null) {
			if (other.suffix != null) {
				return false;
			}
		} else if (!this.suffix.equals(other.suffix)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.prefix + this.direction + this.suffix;
	}

	@Override
	public char getTag() {
		// 'S' to indicate a substitution
		return 'S';
	}

	@Override
	public boolean isMove() {
		return false;
	}

	@Override
	public boolean isSubstitution() {
		return true;
	}

	public static enum Direction {
		OLD_CONTAINS_NEW, NEW_CONTAINS_OLD
	}
}
