package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.BasicDiffType.REFACTOR;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.diff.BasicDiffType.SUBSTITUTE;
import static info.codesaway.bex.util.Utilities.in;

public interface DiffType {
	/**
	 * Get the tag used to describe the type of change (for example, a '+' for an insert)
	 *
	 * @return
	 */
	// TODO: rename to something better
	public char getTag();

	/**
	 * Indicate if this DiffType describes a move
	 *
	 * @return <code>true</code> if this DiffType indicates a move occurred
	 */
	public boolean isMove();

	//	/**
	//	 * Gets the side where the move originates
	//	 *
	//	 * <p>This should return <code>null</code> for DiffType which don't represent a move (when {@link #isMove()} returns <code>false</code>)
	//	 * @return
	//	 */
	//	public default DiffSide getMoveSide() {
	//		return null;
	//	}

	/**
	 * Indicate if this DiffType describes a substitution / replacement
	 *
	 * @return <code>true</code> if this DiffType indicates a substitution / replacement occurred
	 */
	// 8/16/2019
	public boolean isSubstitution();

	public static boolean isBasicDiffType(final DiffType diffType) {
		return diffType.getClass() == BasicDiffType.class || in(diffType, SUBSTITUTE, REPLACEMENT_BLOCK, REFACTOR);
	}
}
