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
}
