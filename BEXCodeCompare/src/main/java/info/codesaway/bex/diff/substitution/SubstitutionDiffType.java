package info.codesaway.bex.diff.substitution;

import info.codesaway.bex.diff.DiffType;

public interface SubstitutionDiffType extends DiffType {
	/**
	 * @return always <code>true</code>
	 */
	@Override
	public default boolean isSubstitution() {
		return true;
	}

	/**
	 * Indicates if differences with this type should be
	 * treated as if they were equal (due to normalization),
	 * even though the actual content may differ
	 * @return
	 */
	public default boolean shouldTreatAsNormalizedEqual() {
		return false;
	}
}
