package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.MatcherTestHelper.testBECRMatch;
import static info.codesaway.becr.matching.MatcherTestHelper.testJustBECRMatch;
import static info.codesaway.becr.matching.MatcherTestHelper.testNoBECRMatch;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BECRMatcherTest {

	@Test
	void testMatchStringText() {
		String pattern = "\"User:[value]\"";
		String text = "request.addChildTag(\"Users\", \"a\", null)";
		String expectedValue = "s";
		testBECRMatch(pattern, text, expectedValue);
	}

	@Test
	void testMatchStringHasParenthesisText() {
		String pattern = "\":[value](\"";
		String text = "webView.loadUrl(\"javascript:showDetail(\"+m.getId()+\")\");";
		String expectedValue = "javascript:showDetail";
		testBECRMatch(pattern, text, expectedValue);
	}

	@Test
	void testBasicOptionalMatch() {
		// TODO: seems like both regular and optional match the same - not sure if the regular shouldn't match the empty string
		String pattern = "try :[?value] { :[stuff] }";
		String text = "		try {\r\n" +
				"\r\n" +
				"			sqlStatement.execute();\r\n" +
				"		} catch (SQLException e) {\r\n" +
				"";
		String expectedValue = "";
		testBECRMatch(pattern, text, expectedValue);
	}

	// TODO: need to support logic that expects the same group to match the same value
	// https://github.com/comby-tools/comby/blob/ab93d721634a19f802ff361e4335d726c3aeba80/test/common/test_hole_extensions_alpha.ml
	// "implicit_equals"

	@Test
	void testSameMatchGroupMustBeEqual() {
		String pattern = ":[x] = :[x]";
		String text = "a = b";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testUnderscoreGroupMayBeDifferent() {
		String pattern = ":[_] = :[_]";
		String text = "a = b";
		testJustBECRMatch(pattern, text);
	}

	@Test
	void testSameMatchGroupAreEqual() {
		String pattern = ":[value] = :[value]";
		String text = "a = a";
		testBECRMatch(pattern, text, "a");
	}

	@Test
	void testPatternStartsIsOnlyOneGroup() {
		String pattern = ":[value]";
		String text = "a = a";
		testBECRMatch(pattern, text, "a = a");
	}

	@Test
	void testAngleBracketsOkayNotBalanced() {
		String pattern = ":[value]";
		String text = "1 < 2";
		testBECRMatch(pattern, text, "1 < 2");
	}

	@Test
	void testOptionAngleBracketsMustBeBalanced() {
		String pattern = ":[value<>]";
		String text = "1 < 2";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testOptionAngleBracketsMustBeBalancedSuchAsJavaGenerics() {
		String pattern = "List:[value<>] :[variable:w]";
		String text = "List<String, List<Integer>> values";
		String expectedValue = "<String, List<Integer>>";
		BECRMatcher becrMatcher = testBECRMatch(pattern, text, expectedValue);

		assertThat(becrMatcher)
				.extracting(m -> m.get("variable"))
				.isEqualTo("values");
	}

	@Test
	void testEmptyDoesNotMatch() {
		String pattern = ":[value]";
		String text = "";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testEmptyOptionalMatch() {
		String pattern = ":[?value]";
		String text = "";
		testJustBECRMatch(pattern, text);
	}

	@Test
	void testInStringLiteralShouldIgnore() {
		String pattern = "try { }";
		String text = "\"try { }\"";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testLineCommentShouldIgnore() {
		String pattern = "try { }";
		String text = "// try { }";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testMultilineCommentShouldIgnore() {
		String pattern = "try { }";
		String text = "/*\n"
				+ "try\n"
				+ "{\n"
				+ "}\n"
				+ "*/";
		testNoBECRMatch(pattern, text);
	}

	@Test
	void testStillInStringNoMatch() {
		String pattern = "\":[value]\"";
		String text = "\"some text ( with a \\\" in it\"";
		String expectedValue = "some text ( with a \\\" in it";
		testBECRMatch(pattern, text, expectedValue);
	}

	@Test
	void testSingleLinePlusOneOrMoreCharacters() {
		// Doesn't check for balanced, since just getting characters
		// like regex .+?
		String pattern = "something :[value+] fun";
		String text = "something cool(unbalanced fun";
		String expectedValue = "cool(unbalanced";
		testBECRMatch(pattern, text, expectedValue);
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
	//		testBECRMatch(pattern, text, expectedValue);
	//	}

	@Test
	void testEscapeColonInPattern() {
		String pattern = ":[:]";
		String text = ":";
		testJustBECRMatch(pattern, text);
	}
}
