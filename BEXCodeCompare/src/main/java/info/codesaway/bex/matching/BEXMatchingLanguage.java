package info.codesaway.bex.matching;

import java.util.function.Function;

import info.codesaway.bex.ImmutableIntRangeMap;

public enum BEXMatchingLanguage {
	JAVA(BEXMatchingUtilities::extractJavaTextStates),
	JSP(BEXMatchingUtilities::extractJSPTextStates),

	// End of enum
	;

	private final Function<CharSequence, ImmutableIntRangeMap<BEXMatchingStateOption>> extractFunction;

	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<BEXMatchingStateOption>> extractFunction) {
		this.extractFunction = extractFunction;
	}

	public ImmutableIntRangeMap<BEXMatchingStateOption> extract(final CharSequence text) {
		return this.extractFunction.apply(text);
	}
}
