package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map.Entry;

import info.codesaway.bex.IntBEXRange;

public final class MatcherTestHelper {
	private MatcherTestHelper() {
		throw new UnsupportedOperationException();
	}

	private static final BEXPatternFlag[] NO_FLAGS = {};

	static void testNoBEXMatch(final String pattern, final String text) {
		testNoBEXMatch(pattern, text, NO_FLAGS);
	}

	static void testNoBEXMatch(final String pattern, final String text, final BEXPatternFlag... flags) {
		BEXPattern bexPattern = BEXPattern.compile(pattern, flags);
		BEXMatcher bexMatcher = bexPattern.matcher(text);

		assertFalse(bexMatcher.find(), "Should not find match");
	}

	static BEXMatcher testJustBEXMatch(final String pattern, final String text) {
		return testJustBEXMatch(pattern, text, NO_FLAGS);
	}

	static BEXMatcher testJustBEXMatch(final String pattern, final String text, final BEXPatternFlag... flags) {
		BEXPattern bexPattern = BEXPattern.compile(pattern, flags);
		BEXMatcher bexMatcher = bexPattern.matcher(text);

		assertTrue(bexMatcher.find(), "Could not find match");

		return bexMatcher;
	}

	static BEXMatcher testJustBEXMatch(final String pattern, final String text, final BEXMatchingLanguage language,
			final BEXPatternFlag... flags) {
		BEXPattern bexPattern = BEXPattern.compile(pattern, flags);
		BEXString bexString = new BEXString(text, language);
		BEXMatcher bexMatcher = bexPattern.matcher(bexString);

		assertTrue(bexMatcher.find(), "Could not find match");

		return bexMatcher;
	}

	/**
	 * Test that a match occurred and that the group named "value" matches the expectedValue
	 * @param pattern the pattern to match
	 * @param text the text to match
	 * @param expectedValue the expected value
	 */
	static BEXMatcher testBEXMatch(final String pattern, final String text, final String expectedValue) {
		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);

		assertThat(bexMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo(expectedValue);

		return bexMatcher;
	}

	@SafeVarargs
	static BEXMatcher testBEXMatchEntries(final String pattern, final String text,
			final Entry<String, String>... entries) {
		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.entrySet()).containsExactly(entries);
		return bexMatcher;
	}

	static BEXMatcher testBEXMatchReplaceAll(final String pattern, final String text,
			final String replacement, final String expectedValue) {
		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.replaceAll(replacement)).isEqualTo(expectedValue);
		return bexMatcher;
	}

	static void testQuoteReplacement(final String replacement, final String expectedValue) {
		BEXPattern bexPattern = BEXPattern.compile("blah");
		BEXMatcher bexMatcher = bexPattern.matcher("blah");

		String quoteReplacement = BEXMatcher.quoteReplacement(replacement);

		assertThat(quoteReplacement).isEqualTo(expectedValue);
		assertThat(bexMatcher.replaceFirst(quoteReplacement)).isEqualTo(replacement);
	}

	static void testPatternLiteral(final String pattern, final String expectedValue) {
		String literalPattern = BEXPattern.literal(pattern);
		BEXPattern bexPattern = BEXPattern.compile(literalPattern);
		BEXMatcher bexMatcher = bexPattern.matcher(pattern);

		assertThat(literalPattern).isEqualTo(expectedValue);

		boolean result = bexMatcher.find();
		assertTrue(result);
		assertThat(bexMatcher.startEndPair()).isEqualTo(IntBEXRange.of(0, pattern.length()));
	}
}
