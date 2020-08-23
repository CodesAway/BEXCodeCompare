package info.codesaway.bex.matching;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.matching.BEXMatchingUtilities.currentChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasCaseInsensitiveText;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasText;
import static info.codesaway.bex.matching.BEXMatchingUtilities.isWordCharacter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.ImmutableIntRangeMap;

public enum BEXMatchingLanguage implements MatchingLanguage {
	JAVA(BEXMatchingUtilities::parseJavaTextStates),
	JSP(BEXMatchingUtilities::parseJSPTextStates),

	/**
	 * SQL Matching language
	 * @since 0.11
	 */
	SQL(BEXMatchingUtilities::parseSQLTextStates, true, bexPair("BEGIN", "END")),

	// End of enum
	;

	private final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction;
	private final boolean hasCaseInsensitiveDelimiters;
	private final List<BEXPair<String>> delimiters;

	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction) {
		this.parseFunction = parseFunction;
		this.hasCaseInsensitiveDelimiters = false;
		this.delimiters = Collections.emptyList();
	}

	@SafeVarargs
	private BEXMatchingLanguage(
			final Function<CharSequence, ImmutableIntRangeMap<MatchingStateOption>> parseFunction,
			final boolean hasCaseInsensitiveDelimiters,
			final BEXPair<String>... delimiters) {
		this.parseFunction = parseFunction;
		this.hasCaseInsensitiveDelimiters = hasCaseInsensitiveDelimiters;
		this.delimiters = Arrays.asList(delimiters);
	}

	@Override
	public ImmutableIntRangeMap<MatchingStateOption> parse(final CharSequence text) {
		return this.parseFunction.apply(text);
	}

	@Override
	public Optional<BEXPair<String>> findStartDelimiter(final CharSequence text, final int index,
			final Set<MatchingLanguageSetting> settings) {

		for (BEXPair<String> delimiter : this.delimiters) {
			String s = delimiter.getLeft();

			if (this.hasCaseInsensitiveDelimiters) {
				if (hasCaseInsensitiveText(text, index, s)
						&& !isWordCharacter(currentChar(text, index + s.length()))) {
					return Optional.of(delimiter);
				}
			} else {
				if (hasText(text, index, s) && !isWordCharacter(currentChar(text, index + s.length()))) {
					return Optional.of(delimiter);
				}
			}
		}

		return MatchingLanguage.super.findStartDelimiter(text, index, settings);
	}

	@Override
	public MatchingDelimiterState findEndDelimiter(final BEXPair<String> lastDelimiter, final CharSequence text,
			final int index,
			final Set<MatchingLanguageSetting> settings) {

		for (BEXPair<String> delimiter : this.delimiters) {
			String s = delimiter.getRight();

			if (this.hasCaseInsensitiveDelimiters) {
				if (hasCaseInsensitiveText(text, index, s)
						&& !isWordCharacter(currentChar(text, index + s.length()))) {
					MatchingDelimiterResult result = lastDelimiter != null
							&& s.equalsIgnoreCase(lastDelimiter.getRight())
									? MatchingDelimiterResult.FOUND
									: MatchingDelimiterResult.MISMATCHED;

					return new MatchingDelimiterState(result, s);
				}
			} else {
				if (hasText(text, index, s) && !isWordCharacter(currentChar(text, index + s.length()))) {
					MatchingDelimiterResult result = lastDelimiter != null && s.equals(lastDelimiter.getRight())
							? MatchingDelimiterResult.FOUND
							: MatchingDelimiterResult.MISMATCHED;

					return new MatchingDelimiterState(result, s);
				}
			}
		}

		return MatchingLanguage.super.findEndDelimiter(lastDelimiter, text, index, settings);
	}
}
