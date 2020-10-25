package info.codesaway.bex.parsing;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.parsing.BEXParsingUtilities.currentChar;
import static info.codesaway.bex.parsing.BEXParsingUtilities.hasText;
import static info.codesaway.bex.parsing.BEXParsingUtilities.isWordCharacter;
import static info.codesaway.bex.parsing.BEXParsingUtilities.previousChar;
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
import info.codesaway.bex.matching.MatchingLanguageSetting;

public enum BEXParsingLanguage implements ParsingLanguage {
	JAVA(BEXParsingUtilities::parseJavaTextStates),
	JSP(BEXParsingUtilities::parseJSPTextStates),

	/**
	 * SQL Matching language
	 * @since 0.11
	 */
	SQL(BEXParsingUtilities::parseSQLTextStates, "@#$", true, bexPair("BEGIN", "END")),

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
		public ParsingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter, final CharSequence text,
				final int index, final Set<MatchingLanguageSetting> settings) {
			return ParsingDelimiterState.NOT_FOUND;
		}
	},

	// End of enum
	;

	private final Function<CharSequence, ImmutableIntRangeMap<ParsingState>> parseFunction;
	private final String specialWordCharacters;
	private final boolean hasCaseInsensitiveDelimiters;
	private final List<Optional<BEXPair<String>>> delimiters;

	private BEXParsingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<ParsingState>> parseFunction) {
		this.parseFunction = parseFunction;
		this.specialWordCharacters = "";
		this.hasCaseInsensitiveDelimiters = false;
		this.delimiters = Collections.emptyList();
	}

	@SafeVarargs
	private BEXParsingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<ParsingState>> parseFunction,
			final String specialWordCharacters,
			final boolean hasCaseInsensitiveDelimiters,
			final BEXPair<String>... delimiters) {
		this.parseFunction = parseFunction;
		this.specialWordCharacters = specialWordCharacters;
		this.hasCaseInsensitiveDelimiters = hasCaseInsensitiveDelimiters;
		this.delimiters = Arrays.stream(delimiters)
				// Wrap in Optional, so don't have to create new Optional objects for each parsing
				.map(Optional::of)
				.collect(toList());
	}

	@Override
	public ImmutableIntRangeMap<ParsingState> parse(final CharSequence text) {
		return this.parseFunction.apply(text);
	}

	@Override
	public Optional<BEXPair<String>> findStartDelimiter(final CharSequence text, final int index,
			final Set<MatchingLanguageSetting> settings) {

		Optional<BEXPair<String>> delimiter = this.findDelimiter(LEFT, text, index);

		if (delimiter.isPresent()) {
			return delimiter;
		}

		return ParsingLanguage.super.findStartDelimiter(text, index, settings);
	}

	@Override
	public ParsingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter, final CharSequence text,
			final int index,
			final Set<MatchingLanguageSetting> settings) {

		Optional<BEXPair<String>> delimiter = this.findDelimiter(RIGHT, text, index);

		if (delimiter.isPresent()) {
			String s = delimiter.get().getRight();

			BiPredicate<String, String> equals = this.hasCaseInsensitiveDelimiters
					? String::equalsIgnoreCase
					: String::equals;

			ParsingDelimiterResult result = lastDelimiter != null && equals.test(s, lastDelimiter.getRight())
					? ParsingDelimiterResult.FOUND
					: ParsingDelimiterResult.MISMATCHED;

			return new ParsingDelimiterState(result, s);
		}

		return ParsingLanguage.super.findEndDelimiter(lastDelimiter, text, index, settings);
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
