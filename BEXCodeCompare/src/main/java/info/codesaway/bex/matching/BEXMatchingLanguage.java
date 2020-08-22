package info.codesaway.bex.matching;

import java.util.function.Function;

import info.codesaway.bex.ImmutableIntRangeMap;

public enum BEXMatchingLanguage implements MatchingLanguage {
	JAVA(BEXMatchingUtilities::parseJavaTextStates),
	JSP(BEXMatchingUtilities::parseJSPTextStates),

	/**
	 * SQL Matching language
	 * @since 0.11
	 */
	SQL(BEXMatchingUtilities::parseSQLTextStates),

	// End of enum
	;

	private final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction;

	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction) {
		this.parseFunction = parseFunction;
	}

	@Override
	public ImmutableIntRangeMap<MatchingStateOption> parse(final CharSequence text) {
		return this.parseFunction.apply(text);
	}
}
