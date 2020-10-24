package info.codesaway.bex.matching;

/**
 * @since 0.11
 */
public interface MatchingStateOption {
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
	 *
	 * @return the parent state, or <ccode>null</code> if none
	 * @since 0.13
	 */
	// TODO: how to best represent?
	// Likely need to keep track of index, but how to also be able to easily check the state?
	// Maybe would be useful to always return an IndexedMatchingState
	// TODO: might want to rename to be ParseState, since not related to matching, just parsing
	public default MatchingStateOption getParent() {
		return null;
	}
}
