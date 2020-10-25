package info.codesaway.bex.parsing;

import info.codesaway.bex.Indexed;

/**
 * @since 0.11
 */
public interface ParsingState {
	public default boolean isCode() {
		return false;
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default boolean isComment() {
		return false;
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default boolean isStringLiteral() {
		return false;
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default boolean isWhitespace() {
		return false;
	}

	/**
	 * Indicates if there is a parent state (for nested / hierarchial states)
	 * @return <code>true</code> if there is a parent state; otherwise, <code>false</code>.
	 * @since 0.13
	 */
	public default boolean hasParent() {
		return this.getParent() != null;
	}

	/**
	 * Gets the parent state
	 * @return the parent state, or <code>null</code> if there is no parent
	 * @since 0.13
	 * @see #hasParent()
	 */
	public default Indexed<ParsingState> getParent() {
		return null;
	}
}
