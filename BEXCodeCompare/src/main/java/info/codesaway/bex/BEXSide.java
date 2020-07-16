package info.codesaway.bex;

/**
 * Side (either {@link #LEFT} or {@link #RIGHT})
 *
 * <p>These are used in BEX to reduce the amount of code that operates on either the left or right side.</p>
 *
 *  <p><b>Implementation note</b>: BEXSide will <b>always</b> only contain LEFT / RIGHT</p>
 */
// NOTE: if support 3-way merge, do separately (do not modify this enum)
public enum BEXSide {
	LEFT, RIGHT;

	/**
	 * Returns the other BEXSide
	 * @return {@link #RIGHT} if called from {@link #LEFT} and {@link #LEFT} if called from {@link #RIGHT}
	 * @since 0.4
	 */
	public BEXSide other() {
		return this == LEFT ? RIGHT : LEFT;
	}
}
