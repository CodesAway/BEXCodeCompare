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
}
