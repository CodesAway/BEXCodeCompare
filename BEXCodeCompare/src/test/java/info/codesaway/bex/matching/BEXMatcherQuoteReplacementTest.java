package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testQuoteReplacement;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BEXMatcherQuoteReplacementTest {

	@ParameterizedTest
	@ValueSource(strings = { "This is something: useful", "myemail@something.com", "regular text" })
	void testNoNeedToQuoteReplacement(final String value) {
		testQuoteReplacement(value, value);
	}

	@Test
	void testQuoteReplacementColon() {
		testQuoteReplacement(":[literal]", ":[:][literal]");
	}

	@Test
	void testQuoteReplacementEndsWithColon() {
		testQuoteReplacement("this is my list:", "this is my list:[:]");
	}

	@Test
	void testQuoteReplacementConcat() {
		BEXPattern pattern = BEXPattern.compile("blah");
		BEXMatcher matcher = pattern.matcher("blah");
		String replacement = matcher
				.replaceFirst(BEXMatcher.quoteReplacement(":") + BEXMatcher.quoteReplacement("[literal]"));

		assertThat(replacement).isEqualTo(":[literal]");
	}

	@Test
	void testQuoteReplacementAtSymbolDoesNotMeanRegexInReplacement() {
		BEXPattern pattern = BEXPattern.compile("blah");
		BEXMatcher matcher = pattern.matcher("blah");
		String replacement = matcher.replaceFirst(BEXMatcher.quoteReplacement("@--"));

		assertThat(replacement).isEqualTo("@--");
	}
}
