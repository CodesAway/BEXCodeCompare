package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatchEntries;
import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatchReplaceAll;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BEXMatcherRegexTest {
	// Reference: https://github.com/comby-tools/comby/blob/master/test/alpha/test_regex_holes.ml
	@Test
	void testSimpleRegexLongSyntax() {
		String pattern = "@--(?<x>\\w+)--!";
		String text = "foo";
		testBEXMatchEntries(pattern, text, entry("x", "foo"));
	}

	@Test
	void testSimpleRegex() {
		String pattern = ":[x~\\w+]";
		String text = "foo";
		testBEXMatchEntries(pattern, text, entry("x", "foo"));
	}

	@Test
	void testSimplePosix() {
		String pattern = ":[x~[[:alpha:]]]";
		String text = "foo";
		String replacement = "(:[x])";
		String expectedValue = "(f)(o)(o)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexSubstring() {
		String pattern = ":[x~o\\w]()";
		String text = "foo()";
		String replacement = "(:[x])";
		String expectedValue = "f(oo)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testEmptyRegex() {
		String pattern = ":[x~]";
		String text = "foo()";
		String replacement = "(:[x])";
		// Note: this differs from Comby, since end of text also matches, since regex is empty
		// (so has one extra set of () at end of replacement result)
		String expectedValue = "()f()o()o()(())()";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexRepetition() {
		String pattern = ":[x~\\w+]bar()";
		String text = "foobar()";
		String replacement = "(:[x])";
		String expectedValue = "(foo)";

		// Note: unlike Comby, this successfully matches
		// (since the whole pattern above changes into a regex)
		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexNegatedMatchNotClosedParentheses() {
		String pattern = "(:[x~[^)]+])";
		String text = "(literally_anyting_except_close_paren?!@#$%^&*[])";
		String replacement = "(:[x])";
		String expectedValue = "(literally_anyting_except_close_paren?!@#$%^&*[])";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexNegatedMatchNotCertainSymbolsAndSpace() {
		String pattern = ":[x~[^,() ]+]";
		String text = "(arg1, arg2, arg3)";
		String replacement = "(:[x])";
		String expectedValue = "((arg1), (arg2), (arg3))";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexDotStarWithNewline() {
		String pattern = ":[x~.*]";
		String text = "foo()\nbar()";
		String replacement = "(:[x])";
		// Note: this differs from Comby, since end of text also matches, since regex is empty
		// (so has one extra set of () at end of replacement result)
		String expectedValue = "(foo())()\n" +
				"(bar())()";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexHasOptional() {
		String pattern = ":[x~no(vember)?]";
		String text = "nonovember no november no vember";
		String replacement = "(:[x])";
		String expectedValue = "(no)(november) (no) (november) (no) vember";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexSpaceBefore() {
		String pattern = "no :[x~(vember)?]";
		String text = "nonovember no november no vember";
		String replacement = "(:[x])";
		String expectedValue = "nonovember ()november (vember)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexAllOptional() {
		String pattern = "no:[x~(vember)?]";
		String text = "no";
		String replacement = "(:[x])";
		String expectedValue = "()";

		// Note: Comby cannot match this, but since I'm converting to regex, it works as expected
		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexOptionalSpaces() {
		String pattern = ":[x~\\s*?]";
		String text = "foo bar foobar";
		String replacement = "(:[x])";
		// Note: this differs from Comby, since end of text also matches, since regex is empty
		// (so has one extra set of () at end of replacement result)
		String expectedValue = "()f()o()o() ()b()a()r() ()f()o()o()b()a()r()";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexOptionalStripNoFromNovemberOutsideRegex() {
		String pattern = "no:[x~(vember)?]";
		String text = "nonovember no november no vember";
		String replacement = "(:[x])";
		// Note: this differs from Comby, since end of text also matches, since regex is empty
		// (so has one extra set of () at end of replacement result)
		String expectedValue = "()(vember) () (vember) () vember";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexOptionalStripNoFromNovemberInsideRegex() {
		String pattern = ":[x~no(vember)?]";
		String text = "nonovember no november no vember";
		String replacement = "(:[x])";
		// Note: this differs from Comby, since end of text also matches, since regex is empty
		// (so has one extra set of () at end of replacement result)
		String expectedValue = "(no)(november) (no) (november) (no) vember";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexNoTrailingBracketThrowsException() {
		String pattern = ":[x~no";
		assertThatThrownBy(() -> BEXPattern.compile(pattern))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Missing end bracket ']' necessary to end regex: no");
	}

	@Test
	void testRegexWithoutGroupName() {
		String pattern = ":[~[a-z]]";
		String text = "abcd";
		String replacement = "(:[*])";
		String expectedValue = "(a)(b)(c)(d)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexWithCaptureGroup() {
		String pattern = ":[~[a-z](?<digit>\\d)]";
		String text = "a1b2c3d4";
		String replacement = "(:[digit])";
		String expectedValue = "(1)(2)(3)(4)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@Test
	void testRegexOptionalMatch() {
		String pattern = ":[~[a-z]]:[?digit~\\d]";
		String text = "a1b2cd4";
		String replacement = "(:[digit])";
		String expectedValue = "(1)(2)()(4)";

		testBEXMatchReplaceAll(pattern, text, replacement, expectedValue);
	}

	@ParameterizedTest
	@ValueSource(strings = { ":[ group~\\w]", ":[group*~\\w]", ":[group.~\\w]" })
	void testRegexCannotBeCombinedWithAnotherGroup(final String pattern) {
		assertThatThrownBy(() -> BEXPattern.compile(pattern))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid syntax: " + pattern.substring(0, pattern.indexOf('~') + 1));
	}
}
