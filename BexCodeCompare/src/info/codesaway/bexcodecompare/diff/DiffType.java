package info.codesaway.bexcodecompare.diff;

public interface DiffType {
	/**
	 * Get the tag used to describe the type of change (for example, an '+' could represent an insert)
	 *
	 * @return
	 */
	public char getTag();

	/**
	 * Indicate if this DiffType describes a move
	 *
	 * @return <code>true</code> if this DiffType indicates a move occurred
	 */
	public boolean isMove();

	/**
	 * Indicate if this DiffType describes a substitution / replacement
	 *
	 * @return <code>true</code> if this DiffType indicates a substitution / replacement occurred
	 */
	// 8/16/2019
	public boolean isSubstitution();

}
