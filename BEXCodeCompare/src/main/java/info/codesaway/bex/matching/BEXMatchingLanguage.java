package info.codesaway.bex.matching;

import java.util.NavigableMap;
import java.util.function.Function;

public enum BEXMatchingLanguage {
	JAVA(BEXMatchingUtilities::extractJavaTextStates),
	JSP(BEXMatchingUtilities::extractJSPTextStates),

	// End of enum
	;

	private final Function<CharSequence, NavigableMap<Integer, BEXMatchingTextState>> extractFunction;

	private BEXMatchingLanguage(
			final Function<CharSequence, NavigableMap<Integer, BEXMatchingTextState>> extractFunction) {
		this.extractFunction = extractFunction;
	}

	public NavigableMap<Integer, BEXMatchingTextState> extract(final CharSequence text) {
		return this.extractFunction.apply(text);
	}
}
