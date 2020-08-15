package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testPatternLiteral;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import info.codesaway.bex.IntBEXRange;

class BEXPatternLiteralTest {

	@ParameterizedTest
	@ValueSource(strings = { "This is something: useful", "myemail@something.com", "regular text", "something@-2",
			"array[0]" })
	void testNoNeedToMakeLiteral(final String value) {
		testPatternLiteral(value, value);
	}

	@Test
	void testPatternLiteralColon() {
		testPatternLiteral(":[literal]", ":[:][literal]");
	}

	@Test
	void testPatternLiteralEndsWithColon() {
		testPatternLiteral("this is my list:", "this is my list:[:]");
	}

	@Test
	void testPatternLiteralConcatAfterColon() {
		BEXPattern pattern = BEXPattern.compile(BEXPattern.literal(":") + BEXPattern.literal("[literal]"));
		BEXMatcher matcher = pattern.matcher(":[literal]");

		assertTrue(matcher.find());

		assertThat(matcher.range()).isEqualTo(IntBEXRange.of(0, matcher.text().length()));
	}

	@Test
	void testPatternLiteralEndsWithAtSymbol() {
		testPatternLiteral("this is my list@", "this is my list:[@]");
	}

	@Test
	void testPatternLiteralEndsWithAtSymbolThenDash() {
		testPatternLiteral("this is my list@-", "this is my list:[@]-");
	}

	@Test
	void testPatternLiteralEndsWithRegexSyntax() {
		testPatternLiteral("this is my list@--", "this is my list:[@]--");
	}

	@Test
	void testPatternLiteralConcatAfterAtSymbol() {
		BEXPattern pattern = BEXPattern.compile(BEXPattern.literal("@") + BEXPattern.literal("--"));
		BEXMatcher matcher = pattern.matcher("@--");

		assertTrue(matcher.find());

		assertThat(matcher.range()).isEqualTo(IntBEXRange.of(0, matcher.text().length()));
	}

	@Test
	void testPatternLiteralConcatAfterAtSymbolThenDash() {
		BEXPattern pattern = BEXPattern.compile(BEXPattern.literal("@-") + BEXPattern.literal("-"));
		BEXMatcher matcher = pattern.matcher("@--");

		assertTrue(matcher.find());

		assertThat(matcher.range()).isEqualTo(IntBEXRange.of(0, matcher.text().length()));
	}
}
