package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testBEXMatchEntries;
import static info.codesaway.bex.util.BEXUtilities.entry;

import org.junit.jupiter.api.Test;

public class BEXOptionalMatchTest {
	@Test
	void testEmptyOptionalMatch() {
		String pattern = ":[?value]";
		String text = "";
		testBEXMatch(pattern, text, "");
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

	// Referenced https://github.com/comby-tools/comby/blob/master/test/alpha/test_optional_holes.ml

	@Test
	void testEmptyDoubleOptionalMatch() {
		String pattern = ":[?value]:[?2]";
		String text = "";
		testBEXMatchEntries(pattern, text, entry("value", ""), entry("2", ""));
	}

	@Test
	void testMatchAlphaNumericThenEmptyMatch() {
		String pattern = ":[value.] :[?2]";
		String text = "a ";
		testBEXMatchEntries(pattern, text, entry("value", "a"), entry("2", ""));
	}

	@Test
	// Issue #69
	void testMatchThenEmptyMatch() {
		String pattern = ":[value] :[?2]";
		String text = "a ";
		testBEXMatchEntries(pattern, text, entry("value", "a"), entry("2", ""));
	}

	@Test
	void testMatchOptionalThenEmptyMatch() {
		String pattern = ":[?value] :[?2]";
		String text = "foo ";
		testBEXMatchEntries(pattern, text, entry("value", "foo"), entry("2", ""));
	}

	@Test
	void testMatchOptionalAlphanumericThenOptionalSpaces() {
		String pattern = ":[?value:w]:[? 2]";
		String text = "foo";
		testBEXMatchEntries(pattern, text, entry("value", "foo"), entry("2", ""));
	}

	@Test
	void testOptionMatchRequiredSpacesBothSides() {
		String pattern = ":[a] :[?b] :[c]";
		String text = "a c";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", ""), entry("c", "c"));
	}

	@Test
	void testOptionMatchRequiredSpacesBothSidesWithLotsOfSpace() {
		String pattern = ":[a] :[?b] :[c]";
		String text = "a        \tc";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", ""), entry("c", "c"));
	}

	@Test
	void testOptionMatchRequiredSpaces() {
		String pattern = ":[a] :[?b]:[c]";
		String text = "a c";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", ""), entry("c", "c"));
	}

	@Test
	void testOptionalMatchSpace() {
		String pattern = ":[a]:[?b]:[c]";
		String text = "a c";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", ""), entry("c", " c"));
	}

	@Test
	void testMatchSpace() {
		String pattern = ":[a]:[b]:[c]";
		String text = "a c";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", " "), entry("c", "c"));
	}

	@Test
	void testOptionalMatchSpaceAfter() {
		String pattern = ":[a]:[?b] :[c]";
		String text = "a c";
		testBEXMatchEntries(pattern, text, entry("a", "a"), entry("b", ""), entry("c", "c"));
	}

	@Test
	void testMatchOptionalThenOptionalSpaces() {
		String pattern = ":[?value:w]:[? 2]";
		String text = "foo";
		testBEXMatchEntries(pattern, text, entry("value", "foo"), entry("2", ""));
	}

	@Test
	void testOptionalWithSurroundingSpaces() {
		String pattern = "func :[?receiver] foo(:[args])";
		String text = "func foo(bar) {}";
		testBEXMatchEntries(pattern, text, entry("receiver", ""), entry("args", "bar"));
	}
}