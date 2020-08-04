package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testJustBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testNoBEXMatch;
import static org.assertj.core.api.Assertions.assertThat;

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
	void testBasicOptionalMatch() {
		String pattern = "try :[?value] { :[stuff] }";
		String text = "		try {\r\n" +
				"\r\n" +
				"			sqlStatement.execute();\r\n" +
				"		} catch (SQLException e) {\r\n" +
				"";
		String expectedValue = "";
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
		testBEXMatch(pattern, text, "a = a");
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
	void testEmptyOptionalMatch() {
		String pattern = ":[?value]";
		String text = "";
		testJustBEXMatch(pattern, text);
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

	// TODO: this test fails
	// * Expecting to match "("
	// * Currently, the code expects the match to have balanced parentheses
	// * However, this isn't required, since it's part of a String
	// Noticed that Comby is only happy if has trailing quote, so this is a niche scenario that I don't plan to handle yet
	//	@Test
	//	void testMatchStringBracketTest() {
	//		String pattern = "\"javascript:showDetail:[value]\"";
	//		String text = "mWebView.loadUrl(\"javascript:showDetail(\"+mWare.getId()+\")\");";
	//		String expectedValue = "(";
	//		testBEXMatch(pattern, text, expectedValue);
	//	}

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
	void testRequiredWhitespaceAfterGroup() {
		String pattern = ":[value] int";
		String text = "pint";
		testNoBEXMatch(pattern, text);
	}

	@Test
	void testRequiredWhitespaceBothBeforeAndAfterGroup() {
		String pattern = "int :[value] ger";

		testNoBEXMatch(pattern, "integer");
		testNoBEXMatch(pattern, "int eger");
		testNoBEXMatch(pattern, "inte ger");
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
}
