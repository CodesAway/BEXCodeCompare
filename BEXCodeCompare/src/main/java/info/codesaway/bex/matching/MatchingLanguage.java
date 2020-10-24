package info.codesaway.bex.matching;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.matching.BEXMatchingUtilities.stringChar;

import java.util.Optional;
import java.util.Set;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.ImmutableIntRangeMap;

/**
 * Interface to allow defining your own MatchingLanguage outside of {@link BEXMatchingLanguage}
 * @since 0.11
 */
@FunctionalInterface
public interface MatchingLanguage {
	/**
	 *
	 * @param text the text to parse and determine the states
	 * @return unmodifiable map from range in text to MatchingStateOption
	 */
	public ImmutableIntRangeMap<MatchingStateOption> parse(final CharSequence text);

	/**
	 * Determines if a delimiter (such as a bracket) is found at the specified index in the text
	 * @param text the text
	 * @param index the index to check for a delimiter
	 * @param settings the settings to apply
	 * @return the optional delimiter (start delimiter / end delimiter pair) found at the specified index in the text; else, {@link Optional#empty()}
	 */
	public default Optional<BEXPair<String>> findStartDelimiter(final CharSequence text, final int index,
			final Set<MatchingLanguageSetting> settings) {

		char c = text.charAt(index);
		boolean shouldMatchAngleBrackets = settings.contains(MatchingLanguageOption.MATCH_ANGLE_BRACKETS);

		String bracketStarts;
		String bracketEnds;

		if (shouldMatchAngleBrackets) {
			bracketStarts = "([{<";
			bracketEnds = ")]}>";
		} else {
			bracketStarts = "([{";
			bracketEnds = ")]}";
		}

		int indexOfBracket = bracketStarts.indexOf(c);

		if (indexOfBracket == -1) {
			return Optional.empty();
		}

		return Optional.of(bexPair(stringChar(bracketStarts, indexOfBracket), stringChar(bracketEnds, indexOfBracket)));
	}

	/**
	 * Determines if a delimiter (such as a bracket) is found at the specified index in the text
	 * @param lastDelimiter the last delimiter (or <code>null</code>
	 * @param text the text
	 * @param index the index to check for a delimiter
	 * @param settings the settings to apply
	 * @return the optional delimiter (start delimiter / end delimiter pair) found at the specified index in the text; else, {@link Optional#empty()}
	 */
	public default MatchingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter,
			final CharSequence text, final int index,
			final Set<MatchingLanguageSetting> settings) {

		char c = text.charAt(index);
		boolean shouldMatchAngleBrackets = settings.contains(MatchingLanguageOption.MATCH_ANGLE_BRACKETS);

		//		String bracketStarts;
		String bracketEnds;

		if (shouldMatchAngleBrackets) {
			//			bracketStarts = "([{<";
			bracketEnds = ")]}>";
		} else {
			//			bracketStarts = "([{";
			bracketEnds = ")]}";
		}

		int indexOfBracket = bracketEnds.indexOf(c);

		if (indexOfBracket == -1) {
			return MatchingDelimiterState.NOT_FOUND;
		}

		String delimiter = stringChar(bracketEnds, indexOfBracket);
		MatchingDelimiterResult result = lastDelimiter != null && delimiter.equals(lastDelimiter.getRight())
				? MatchingDelimiterResult.FOUND
				: MatchingDelimiterResult.MISMATCHED;

		return new MatchingDelimiterState(result, delimiter);
	}
}
