package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testJustBEXMatch;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BEXJSPMatcherTest {
	@Test
	void testExpressionInString() {
		String pattern = "getValue(:[value])";
		String text = "		return test(\"<%=getValue(parameter)%>\");";
		testJustBEXJSPMatch(pattern, text);
	}

	@Test
	void testExpressionInSecondaryString() {
		String pattern = "getValue(:[value])";
		String text = "		return test('<%=getValue(parameter)%>');";
		testJustBEXJSPMatch(pattern, text);
	}

	private static BEXMatcher testJustBEXJSPMatch(final String pattern, final String text,
			final BEXPatternFlag... flags) {
		return testJustBEXMatch(pattern, text, BEXMatchingLanguage.JSP, flags);
	}

	/**
	 * Test that a match occurred and that the group named "value" matches the expectedValue
	 * @param pattern the pattern to match
	 * @param text the text to match
	 * @param expectedValue the expected value
	 */
	private static BEXMatcher testBEXMatch(final String pattern, final String text, final String expectedValue) {
		BEXMatcher bexMatcher = testJustBEXJSPMatch(pattern, text);

		assertThat(bexMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo(expectedValue);

		return bexMatcher;
	}
}
