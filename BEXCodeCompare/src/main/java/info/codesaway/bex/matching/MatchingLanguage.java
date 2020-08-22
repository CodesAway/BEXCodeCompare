package info.codesaway.bex.matching;

import info.codesaway.bex.ImmutableIntRangeMap;

/**
 * Interface to allow defining your own MatchingLanguage outside of {@link BEXMatchingLanguage}
 * @since 0.11
 */
public interface MatchingLanguage {
	/**
	 *
	 * @param text the text to parse and determine the states
	 * @return unmodifiable map from range in text to MatchingStateOption
	 */
	public ImmutableIntRangeMap<MatchingStateOption> parse(final CharSequence text);
}
