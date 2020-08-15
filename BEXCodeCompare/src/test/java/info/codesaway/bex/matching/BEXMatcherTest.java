package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatchReplaceAll;
import static info.codesaway.bex.matching.MatcherTestHelper.testJustBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testNoBEXMatch;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class BEXMatcherTest {

	@Test
	void testMatchStringText() {
		String pattern = "\"User:[value]\"";
		String text = "request.addChildTag(\"Users\", \"a\", null)";
		String expectedValue = "s";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testMatchStringHasParenthesisText() {
		String pattern = "\":[value](\"";
		String text = "webView.loadUrl(\"javascript:showDetail(\"+m.getId()+\")\");";
		String expectedValue = "javascript:showDetail";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testBasicMatchCannotBeEmpty() {
		String pattern = "try :[value] { :[stuff] }";
		String text = "		try {\r\n" +
				"\r\n" +
				"			sqlStatement.execute();\r\n" +
				"		} catch (SQLException e) {\r\n" +
				"";
		testNoBEXMatch(pattern, text);
	}

	// TODO: need to support logic that expects the same group to match the same value
	// https://github.com/comby-tools/comby/blob/ab93d721634a19f802ff361e4335d726c3aeba80/test/common/test_hole_extensions_alpha.ml
	// "implicit_equals"

	@Test
	void testSameMatchGroupMustBeEqual() {
		String pattern = ":[x] = :[x]";
		String text = "a = b";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testUnderscoreGroupMayBeDifferent() {
		String pattern = ":[_] = :[_]";
		String text = "a = b";
		testJustBEXMatch(pattern, text);
	}

	@Test
	void testSameMatchGroupAreEqual() {
		String pattern = ":[value] = :[value]";
		String text = "a = a";
		testBEXMatch(pattern, text, "a");
	}

	@Test
	void testPatternSingleGroupMatchesAll() {
		String pattern = ":[value]";
		String text = "a = a";
		testBEXMatch(pattern, text, text);
	}

	@Test
	// Issue #82
	void testPatternSingleGroupMatchesAllAcrossMultipleLines() {
		String pattern = ":[value]";
		String text = "a = a\r\na = a";
		testBEXMatch(pattern, text, text);
	}

	@Test
	// Issue #82
	void testPatternTrailingGroupMatchesAllAcrossMultipleLines() {
		String pattern = "blah :[value]";
		String text = "blah a = a\r\na = a";
		testBEXMatch(pattern, text, "a = a\r\na = a");
	}

	@Test
	void testAngleBracketsOkayNotBalanced() {
		String pattern = ":[value]";
		String text = "1 < 2";
		testBEXMatch(pattern, text, "1 < 2");
	}

	@Test
	void testOptionAngleBracketsMustBeBalanced() {
		String pattern = ":[value<>]";
		String text = "1 < 2";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testOptionAngleBracketsMustBeBalancedSuchAsJavaGenerics() {
		String pattern = "List:[value<>] :[variable:w]";
		String text = "List<String, List<Integer>> values";
		String expectedValue = "<String, List<Integer>>";
		BEXMatcher bexMatcher = testBEXMatch(pattern, text, expectedValue);

		assertThat(bexMatcher)
				.extracting(m -> m.get("variable"))
				.isEqualTo("values");
	}

	@Test
	void testEmptyDoesNotMatch() {
		String pattern = ":[value]";
		String text = "";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testInStringLiteralShouldIgnore() {
		String pattern = "try { }";
		String text = "\"try { }\"";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testInStringLiteralShouldIgnoreEvenWithoutEnding() {
		String pattern = "try { }";
		String text = "\"try { }";
		// Note: doesn't have String literal ending, yet still should be ignored
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testLineCommentShouldIgnore() {
		String pattern = "try { }";
		String text = "// try { }";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testMultilineCommentShouldIgnore() {
		String pattern = "try { }";
		String text = "/*\n"
				+ "try\n"
				+ "{\n"
				+ "}\n"
				+ "*/";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testMultilineCommentShouldIgnoreEvenWithoutEnding() {
		String pattern = "try { }";
		String text = "/*\n"
				+ "try\n"
				+ "{\n"
				+ "}\n";
		// Note: doesn't have multiline comment ending, yet still should be ignored
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testStillInStringNoMatch() {
		String pattern = "\":[value]\"";
		String text = "\"some text ( with a \\\" in it\"";
		String expectedValue = "some text ( with a \\\" in it";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testSingleLinePlusOneOrMoreCharacters() {
		// Doesn't check for balanced, since just getting characters
		// like regex .+?
		String pattern = "something :[value+] fun";
		String text = "something cool(unbalanced fun";
		String expectedValue = "cool(unbalanced";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testSingleLineStarZeroOrMoreCharacters() {
		// Doesn't check for balanced, since just getting characters
		// like regex .+?
		String pattern = "something :[value*] fun";
		String text = "something cool(unbalanced fun";
		String expectedValue = "cool(unbalanced";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testSingleLineEmptyStarZeroCharacters() {
		String pattern = "something :[value*] fun";
		String text = "something fun";
		String expectedValue = "";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testMatchStringBracketTest() {
		String pattern = "\"javascript:showDetail:[value]\"";
		String text = "mWebView.loadUrl(\"javascript:showDetail(\"+mWare.getId()+\")\");";
		String expectedValue = "(";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testEscapeColonInPattern() {
		String pattern = ":[:]";
		String text = ":";
		testJustBEXMatch(pattern, text);
	}

	@Test
	void testOptionalWhitespace() {
		String pattern = "if (:[value])";
		String text = "if(something)";
		String expectedValue = "something";
		testBEXMatch(pattern, text, expectedValue);
	}

	@Test
	void testRequiredWhitespace() {
		String pattern = "if  (:[value])";
		String text = "if(something)";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testRequiredWhitespaceBeforeGroup() {
		String pattern = "int :[value]";
		String text = "integer";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testOptionalWhitespaceBeforeGroupIfNotWord() {
		String pattern = "method(:[1], :[2])";
		String text = "method(1,2)";
		testJustBEXMatch(pattern, text);
	}

	@Test
	void testRequiredWhitespaceAfterGroup() {
		String pattern = ":[value] int";
		String text = "pint";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testOptionalWhitespaceAfterGroupIfNotWord() {
		String pattern = "method(:[1] ,:[2])";
		String text = "method(1,2)";
		testJustBEXMatch(pattern, text);
	}

	@Test
	void testOptionalWhitespaceBeforeAndAfterGroupIfNotWord() {
		String pattern = "method(:[1] , :[2])";
		String text = "method(1,2)";
		testJustBEXMatch(pattern, text);
	}

	@Test
	void testRequireWhitespaceIfRequireSpaceFlagIsSet() {
		String pattern = "method(:[1] , :[2])";
		String text = "method(1,2)";
		testNoBEXMatch(pattern, text, BEXPatternFlag.REQUIRE_SPACE);
	}

	@Test
	void testRequiredWhitespaceBothBeforeAndAfterGroup() {
		String pattern = "int :[value] ger";

		testNoBEXMatch(pattern, "integer");
		testNoBEXMatch(pattern, "int eger");
		testNoBEXMatch(pattern, "inte ger");
	}

	@Test
	// Issue #68
	void testRequiredSpaceBetweenGroups() {
		String pattern = ":[1] :[2]";
		String text = "integer";

		testNoBEXMatch(pattern, text);
	}

	@Test
	void testRequiredSpaceBetweenRequiredAndOptionalGroups() {
		String pattern = ":[1] :[?2]";
		String text = "integer";

		testNoBEXMatch(pattern, text);
	}

	@Test
	void testRequiredWhitespaceBothBeforeAndAfterOptionalGroup() {
		String pattern = "int :[?value] ger";

		testBEXMatch(pattern, "int ger", "");
	}

	@Test
	void testOptionalSpace() {
		String pattern = "if (:[value])";
		testBEXMatch(pattern, "if (something)", "something");
		testBEXMatch(pattern, "if(something)", "something");
	}

	@Test
	void testGreedyDotThenOptionalGroup() {
		String pattern = ":[value.]:[?digits]";
		BEXMatcher bexMatcher = testBEXMatch(pattern, "matcher123", "matcher123");
		assertThat(bexMatcher.group("digits")).isEmpty();
	}

	@Test
	void testGreedyDotLeavesNothing() {
		String pattern = ":[value.]:[digits]";
		testNoBEXMatch(pattern, "matcher123");
	}

	@Test
	void testWordGroup() {
		String pattern = ":[value:w]";
		testBEXMatch(pattern, "matcher123;", "matcher123");
	}

	@Test
	void testDigitGroup() {
		String pattern = ":[value:d]";
		testBEXMatch(pattern, "matcher123;", "123");
	}

	@Test
	void testStarGroup() {
		BEXMatcher bexMatcher = testBEXMatch("blah :[value]", "blah fun", "fun");
		assertThat(bexMatcher.group("*")).isEqualTo("blah fun");
	}

	@Test
	void testHasCommentAndParenthesesInMatch() {
		String pattern = "method(:[1], :[2])";
		String text = "method(1, (2/* comment) */))";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.get("1")).isEqualTo("1");
		assertThat(bexMatcher.get("2")).isEqualTo("(2/* comment) */)");
	}

	@Test
	void testOkayNotBalancedBracketInStringLiteral() {
		String pattern = "method(:[value])";
		String text = "method(\"(\")";
		testBEXMatch(pattern, text, "\"(\"");
	}

	@Test
	void testOkayNotBalancedBracketInOtherStringLiteral() {
		String pattern = "method(:[value])";
		String text = "method('(')";
		testBEXMatch(pattern, text, "'('");
	}

	@Test
	void testUnderscoreGroupNameNotStored() {
		String pattern = "a :[_] c";
		String text = "a b c";
		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);

		assertThatThrownBy(() -> bexMatcher.get("_"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The specified group is not in the pattern: _");
	}

	@Test
	void testDuplicateGroupNameInRegex() {
		String pattern = ":[1] @--(?:(?<day>Sun)day|(?<day>Sat)urday)--! :[day]";
		String text = "something Sunday Sun";
		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);

		assertThat(bexMatcher.entrySet()).containsExactly(entry("1", "something"), entry("day", "Sun"));
	}

	@Test
	void testDuplicateGroupNameInRegexToMatchResult() {
		String pattern = ":[1] @--(?<day>Sun)day|(?<day>Sat)urday--! :[day]";
		String text = "something Sunday Sun";
		BEXMatchResult bexMatcher = testJustBEXMatch(pattern, text).toMatchResult();

		assertThat(bexMatcher.entrySet()).containsExactly(entry("1", "something"), entry("day", "Sun"));
	}

	@Test
	void testDuplicateGroupNameInRegexDoesNotMatchGroupValue() {
		String pattern = ":[1] @--(?<day>Sun)day|(?<day>Sat)urday--! :[day]";
		String text = "something Sunday Sat";
		testNoBEXMatch(pattern, text);
	}

	@Test
	// Issue #75
	void testMismatchedBracketsDoesNotCauseInfiniteLoop() {
		String pattern = "if (:[before]blah:[after])";
		String text = "if (something) blah";

		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> testNoBEXMatch(pattern, text));
	}

	@Test
	// Issue #81
	void testLineSeparatorMatch() {
		String pattern = "something :[value\\n]";
		String text = "something else is at the end of this line\r\n";
		testBEXMatch(pattern, text, "else is at the end of this line\r\n");
	}

	@Test
	// Issue #81
	void testLineSeparatorMatchWithActualNewLine() {
		String pattern = "something :[value\n]";
		String text = "something else is at the end of this line\r\n";
		testBEXMatch(pattern, text, "else is at the end of this line\r\n");
	}

	@Test
	// Issue #81
	void testLineSeparatorMatchOrEndOfText() {
		String pattern = ":[key] = :[value\\n$]";
		String text = "a = b\n"
				+ "c = d";
		testBEXMatchReplaceAll(pattern, text, "", "");
	}

	@Test
	// Issue #81
	void testLineSeparatorMatchOrEndOfTextWithActualNewLine() {
		String pattern = ":[key] = :[value\n$]";
		String text = "a = b\n"
				+ "c = d";
		testBEXMatchReplaceAll(pattern, text, "", "");
	}

	@Test
	// Issue #81
	void testOptionalLineSeparatorMatch() {
		String pattern = "something :[?_\\n] :[value]";
		String text = "something else is at the end of this line";
		testBEXMatch(pattern, text, "else is at the end of this line");
	}

	@Test
	// Issue #81
	void testOptionalLineSeparatorMatchWithActualNewLine() {
		String pattern = "something :[?_\n] :[value]";
		String text = "something else is at the end of this line";
		testBEXMatch(pattern, text, "else is at the end of this line");
	}
}
