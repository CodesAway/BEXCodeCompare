package info.codesaway.bex.matching;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.matching.BEXMatchingUtilities.currentChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasText;
import static info.codesaway.bex.matching.BEXMatchingUtilities.isWordCharacter;
import static info.codesaway.bex.matching.BEXMatchingUtilities.previousChar;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.ImmutableIntRangeMap;

public enum BEXMatchingLanguage implements MatchingLanguage {
	JAVA(BEXMatchingUtilities::parseJavaTextStates),
	JSP(BEXMatchingUtilities::parseJSPTextStates),

	/**
	 * SQL Matching language
	 * @since 0.11
	 */
	SQL(BEXMatchingUtilities::parseSQLTextStates, "@#$", true, bexPair("BEGIN", "END")),

	/**
	 * Language which gives no special meaning to any characters
	 *
	 * <p>For example, brackets aren't checked that they balance</p>
	 * @since 0.13
	 */
	TEXT(x -> ImmutableIntRangeMap.of()) {
		@Override
		public Optional<BEXPair<String>> findStartDelimiter(final CharSequence text, final int index,
				final Set<MatchingLanguageSetting> settings) {
			return Optional.empty();
		}

		@Override
		public MatchingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter, final CharSequence text,
				final int index, final Set<MatchingLanguageSetting> settings) {
			return MatchingDelimiterState.NOT_FOUND;
		}
	},

	// End of enum
	;

	private final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction;
	private final String specialWordCharacters;
	private final boolean hasCaseInsensitiveDelimiters;
	private final List<Optional<BEXPair<String>>> delimiters;

	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction) {
		this.parseFunction = parseFunction;
		this.specialWordCharacters = "";
		this.hasCaseInsensitiveDelimiters = false;
		this.delimiters = Collections.emptyList();
	}

	@SafeVarargs
	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction,
			final String specialWordCharacters,
			final boolean hasCaseInsensitiveDelimiters,
			final BEXPair<String>... delimiters) {
		this.parseFunction = parseFunction;
		this.specialWordCharacters = specialWordCharacters;
		this.hasCaseInsensitiveDelimiters = hasCaseInsensitiveDelimiters;
		this.delimiters = Arrays.stream(delimiters)
				// Wrap in Optional, so don't have to create new Optional objects for each matching
				.map(Optional::of)
				.collect(toList());
	}

	@Override
	public ImmutableIntRangeMap<MatchingStateOption> parse(final CharSequence text) {
		return this.parseFunction.apply(text);
	}

	@Override
	public Optional<BEXPair<String>> findStartDelimiter(final CharSequence text, final int index,
			final Set<MatchingLanguageSetting> settings) {

		Optional<BEXPair<String>> delimiter = this.findDelimiter(LEFT, text, index);

		if (delimiter.isPresent()) {
			return delimiter;
		}

		return MatchingLanguage.super.findStartDelimiter(text, index, settings);
	}

	@Override
	public MatchingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter, final CharSequence text,
			final int index,
			final Set<MatchingLanguageSetting> settings) {

		Optional<BEXPair<String>> delimiter = this.findDelimiter(RIGHT, text, index);

		if (delimiter.isPresent()) {
			String s = delimiter.get().getRight();

			BiPredicate<String, String> equals = this.hasCaseInsensitiveDelimiters
					? String::equalsIgnoreCase
					: String::equals;

			MatchingDelimiterResult result = lastDelimiter != null && equals.test(s, lastDelimiter.getRight())
					? MatchingDelimiterResult.FOUND
					: MatchingDelimiterResult.MISMATCHED;

			return new MatchingDelimiterState(result, s);
		}

		return MatchingLanguage.super.findEndDelimiter(lastDelimiter, text, index, settings);
	}

	private Optional<BEXPair<String>> findDelimiter(final BEXSide side, final CharSequence text, final int index) {
		if (!this.delimiters.isEmpty() && !this.isPartOfWord(previousChar(text, index))) {
			for (Optional<BEXPair<String>> delimiter : this.delimiters) {
				String s = delimiter.get().get(side);
				char nextChar = currentChar(text, index + s.length());

				if (hasText(text, index, s, this.hasCaseInsensitiveDelimiters) && !this.isPartOfWord(nextChar)) {
					return delimiter;
				}
			}
		}

		return Optional.empty();
	}

	private boolean isPartOfWord(final char c) {
		return isWordCharacter(c) || this.specialWordCharacters.indexOf(c) != -1;
	}
}
