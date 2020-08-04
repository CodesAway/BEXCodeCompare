package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.codesaway.bex.matching.BEXMatcher;
import info.codesaway.bex.matching.BEXPattern;

public final class MatcherTestHelper {
	private MatcherTestHelper() {
		throw new UnsupportedOperationException();
	}

	static void testNoBEXMatch(final String pattern, final String text) {
		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(text);

		assertFalse(bexMatcher.find(), "Should not find match");
	}

	static BEXMatcher testJustBEXMatch(final String pattern, final String text) {
		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(text);

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
}
