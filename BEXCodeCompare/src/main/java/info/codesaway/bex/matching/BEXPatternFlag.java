package info.codesaway.bex.matching;

public enum BEXPatternFlag {
	// TODO: add support for flags
	/**
	 * Enables case-insensitive matching.
	 *
	 * <p> By default, case-insensitive matching assumes that only characters
	 * in the US-ASCII charset are being matched.  Unicode-aware
	 * case-insensitive matching can be enabled by specifying the {@link
	 * #UNICODE} flag in conjunction with this flag.
	 *
	 * <p> Specifying this flag may impose a slight performance penalty.  </p>
	 */
	CASE_INSENSITIVE(0x01),

	/**
	 * Enables Unicode-aware case folding and matching.
	 *
	 * <p> When this flag is specified then case-insensitive matching, when
	 * enabled by the {@link #CASE_INSENSITIVE} flag, is done in a manner
	 * consistent with the Unicode Standard.  By default, case-insensitive
	 * matching assumes that only characters in the US-ASCII charset are being
	 * matched.</p>
	 *
	 * <p> When this flag is specified then matching is done
	 * in conformance with
	 * <a href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
	 * Standard #18: Unicode Regular Expression</i></a>
	 * <i>Annex C: Compatibility Properties</i>.</p>
	 *
	 * <p>Specifying this flag may impose a performance penalty.</p>
	 */
	UNICODE(0x02),

	/**
	 * By default, spaces in the pattern represent optional spaces in the matched text (except in certain circumstances)
	 *
	 * <p>When this flag is specified, then spaces in the pattern will <b>always</b> represent <b>required</b> spaces in the matched text</p>
	 */
	REQUIRE_SPACE(0x04),
	;

	private final int value;

	private BEXPatternFlag(final int value) {
		this.value = value;
	}

	static int encodeFlags(final BEXPatternFlag... flags) {
		int result = 0;

		for (BEXPatternFlag flag : flags) {
			result |= flag.value;
		}

		return result;
	}
}
