package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.BasicDiffType.REFACTOR;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.diff.BasicDiffType.SUBSTITUTE;
import static info.codesaway.bex.util.BEXUtilities.in;

/**
 * Difference type
 */
public interface DiffType {
	/**
	 * Get the symbol used to describe the type of change (for example, a '+' for an insert)
	 *
	 * @return the symbol used to describe the type of change
	 */
	public char getSymbol();

	/**
	 * Indicate if this DiffType describes a move
	 *
	 * @return <code>true</code> if this DiffType indicates a move occurred
	 * @see BasicDiffType#MOVE_LEFT
	 * @see BasicDiffType#MOVE_RIGHT
	 * @see BasicDiffType#MOVE_BLOCK
	 */
	public boolean isMove();

	/**
	 * Indicate if this DiffType describes a substitution / replacement
	 *
	 * @return <code>true</code> if this DiffType indicates a substitution / replacement occurred
	 */
	// 8/16/2019
	public boolean isSubstitution();

	/**
	 * Indicates if differences with this type should be
	 * treated as if they were equal (due to normalization),
	 * even though the actual content may differ
	 * @return <code>true</code> if should treat the difference with this DiffType as normalized equal
	 */
	public default boolean shouldTreatAsNormalizedEqual() {
		return false;
	}

	/**
	 * Indicates if differences with this type should be ignored
	 *
	 * <p><b>It it up to the client to decide how and when to ignore</b>.
	 * This method returning <code>true</code> does not imply the difference will always be ignored.
	 * However, this method returning <code>false</code> should indicate that the difference should <b>not</b> be ignored.
	 * @return <code>true</code> if should ignore the difference with this DiffType
	 * @see BasicDiffType#IGNORE
	 */
	public default boolean shouldIgnore() {
		return false;
	}

	/**
	 *
	 * @param diffType the DiffType
	 * @return <code>true</code> if this DiffType is one defined in BasicDiffType
	 */
	public static boolean isBasicDiffType(final DiffType diffType) {
		return diffType.getClass() == BasicDiffType.class || in(diffType, SUBSTITUTE, REPLACEMENT_BLOCK, REFACTOR);
	}
}
