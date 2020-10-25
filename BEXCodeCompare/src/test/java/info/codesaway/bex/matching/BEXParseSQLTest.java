package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.IntBEXRange.singleton;
import static info.codesaway.bex.matching.MatcherTestHelper.testJustBEXMatch;
import static info.codesaway.bex.matching.MatcherTestHelper.testNoBEXMatch;
import static info.codesaway.bex.parsing.BEXParsingState.IN_LINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_STRING_LITERAL;
import static info.codesaway.bex.parsing.BEXParsingState.LINE_TERMINATOR;
import static info.codesaway.bex.parsing.BEXParsingState.WHITESPACE;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.parsing.BEXParsingLanguage;
import info.codesaway.bex.parsing.BEXString;

public class BEXParseSQLTest {
	@Test
	void testNestedBlockComments() {
		// Text is the following
		//
		//	/*
		//	abc
		//	/*
		//	def
		//	*/
		//
		//	select *
		//	from table
		//
		//	*/
		//
		//	select 1
		String text = "\r\n" +
				"/*\r\n" +
				"abc\r\n" +
				"/*\r\n" +
				"def\r\n" +
				"*/\r\n" +
				"\r\n" +
				"select *\r\n" +
				"from table\r\n" +
				"\r\n" +
				"*/\r\n" +
				"\r\n" +
				"select 1";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(closed(0, 1), LINE_TERMINATOR),
						entry(closedOpen(2, 11), IN_MULTILINE_COMMENT),
						entry(closed(11, 21), IN_MULTILINE_COMMENT),
						entry(closed(22, 51), IN_MULTILINE_COMMENT),
						entry(closed(52, 53), LINE_TERMINATOR),
						entry(closed(54, 55), LINE_TERMINATOR),
						entry(singleton(62), WHITESPACE));

		assertThat(bexString.substring(closed(11, 21))).asString()
				.isEqualTo("/*\r\n" +
						"def\r\n" +
						"*/");
	}

	@Test
	void testLineComment() {
		String text = "1 -- text";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(singleton(1), WHITESPACE),
						entry(closedOpen(2, text.length()), IN_LINE_COMMENT));
	}

	@Test
	void testSingleQuoteEscape() {
		String text = "'before''after'";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closed(0, text.length() - 1), IN_STRING_LITERAL));
	}

	@Test
	void testBackslashDoesNotEscapeSingleQuote() {
		String text = "select 'text\\' as text";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(singleton(6), WHITESPACE),
						entry(closed(7, 13), IN_STRING_LITERAL),
						entry(singleton(14), WHITESPACE),
						entry(singleton(17), WHITESPACE));
	}

	@Test
	void testNestedIfBlocks() {
		// Text is the following
		//	declare @x int = 1
		//	declare @y int = 0
		//
		//	if @x > 0
		//	BEGIN
		//		print 'First if'
		//
		//		if @x > @y
		//		BEGIN
		//			print 'Second if'
		//		END
		//	END
		String text = "			declare @x int = 1\r\n" +
				"			declare @y int = 0\r\n" +
				"		\r\n" +
				"			if @x > 0\r\n" +
				"			BEGIN\r\n" +
				"				print 'First if'\r\n" +
				"		\r\n" +
				"				if @x > @y\r\n" +
				"				BEGIN\r\n" +
				"					print 'Second if'\r\n" +
				"				END\r\n" +
				"			END";

		String pattern = "if :[condition] begin :[stuff] end";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL,
				BEXPatternFlag.CASE_INSENSITIVE);

		assertThat(bexMatcher.get("stuff")).isEqualTo("print 'First if'\r\n" +
				"		\r\n" +
				"				if @x > @y\r\n" +
				"				BEGIN\r\n" +
				"					print 'Second if'\r\n" +
				"				END");
	}

	@Test
	void testTrailingBeginDoesNotMatch() {
		String pattern = ":[stuff]";
		String text = "BEGIN";

		testNoBEXMatch(pattern, text, BEXParsingLanguage.SQL);
	}

	@Test
	void testVariableNameBeginMatches() {
		String pattern = ":[value]";
		String text = "@BEGIN";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL);
		assertThat(bexMatcher.group("value")).isEqualTo("@BEGIN");
	}

	@Test
	void testVariableNameContainingBeginMatches() {
		String pattern = ":[value]";
		String text = "@lets_begin";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL);
		assertThat(bexMatcher.group("value")).isEqualTo("@lets_begin");
	}

	@Test
	void testTrailingEndDoesNotMatch() {
		String pattern = ":[stuff]";
		String text = "END";

		testNoBEXMatch(pattern, text, BEXParsingLanguage.SQL);
	}

	@Test
	void testVariableNameEndMatches() {
		String pattern = ":[value]";
		String text = "@end";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL);
		assertThat(bexMatcher.group("value")).isEqualTo("@end");
	}

	@Test
	void testVariableNameContainingEndMatches() {
		String pattern = ":[value]";
		String text = "@the_end";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL);
		assertThat(bexMatcher.group("value")).isEqualTo("@the_end");
	}

	// Issue #89
	@Test
	void testTableNameContainingEndMatches() {
		String pattern = ":[value]";
		String text = "#end";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text, BEXParsingLanguage.SQL);
		assertThat(bexMatcher.group("value")).isEqualTo("#end");
	}
}
