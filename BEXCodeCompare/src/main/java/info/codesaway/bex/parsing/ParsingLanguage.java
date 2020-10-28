package info.codesaway.bex.parsing;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.parsing.BEXParsingUtilities.stringChar;

import java.util.Optional;
import java.util.Set;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.ImmutableIntRangeMap;
import info.codesaway.bex.matching.MatchingLanguageOption;
import info.codesaway.bex.matching.MatchingLanguageSetting;

/**
 * Interface to allow defining your own ParsingLanguage outside of {@link BEXParsingLanguage}
 * @since 0.11
 */
@FunctionalInterface
public interface ParsingLanguage {
	/**
	 *
	 * @param text the text to parse and determine the states
	 * @return unmodifiable map from range in text to ParsingState
	 */
	public ImmutableIntRangeMap<ParsingState> parse(final CharSequence text);

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
	public default ParsingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter,
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
			return ParsingDelimiterState.NOT_FOUND;
		}

		String delimiter = stringChar(bracketEnds, indexOfBracket);
		ParsingDelimiterResult result = lastDelimiter != null && delimiter.equals(lastDelimiter.getRight())
				? ParsingDelimiterResult.FOUND
				: ParsingDelimiterResult.MISMATCHED;

		return new ParsingDelimiterState(result, delimiter);
	}
}
