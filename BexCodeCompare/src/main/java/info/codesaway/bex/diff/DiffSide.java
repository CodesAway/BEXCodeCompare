package info.codesaway.bex.diff;

/**
 * Diff side (either left or right)
 *
 * <p>These are used in BEX to reduce the amount of code that operates on either the left or right diff side.</p>
 *
 *  <p><b>Implementation note</b>: DiffSide will always only contain LEFT / RIGHT</p>
 */
// NOTE: if support 3-way merge, do separately (do not modify this enum)
public enum DiffSide {
	LEFT, RIGHT;
}
