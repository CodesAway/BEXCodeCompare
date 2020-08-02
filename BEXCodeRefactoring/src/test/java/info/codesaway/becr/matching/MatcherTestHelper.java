package info.codesaway.becr.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MatcherTestHelper {
	private MatcherTestHelper() {
		throw new UnsupportedOperationException();
	}

	static void testNoBECRMatch(final String pattern, final String text) {
		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		assertFalse(becrMatcher.find(), "Should not find match");
	}

	static BECRMatcher testJustBECRMatch(final String pattern, final String text) {
		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		assertTrue(becrMatcher.find(), "Could not find match");

		return becrMatcher;
	}

	/**
	 * Test that a match occurred and that the group named "value" matches the expectedValue
	 * @param pattern the pattern to match
	 * @param text the text to match
	 * @param expectedValue the expected value
	 */
	static BECRMatcher testBECRMatch(final String pattern, final String text, final String expectedValue) {
		BECRMatcher becrMatcher = testJustBECRMatch(pattern, text);

		assertThat(becrMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo(expectedValue);

		return becrMatcher;
	}
}
