package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(closedOpen(2, 11), IN_MULTILINE_COMMENT),
						entry(closed(11, 21), IN_MULTILINE_COMMENT),
						entry(closed(22, 51), IN_MULTILINE_COMMENT));

		assertThat(bexString.substring(closed(11, 21))).asString()
				.isEqualTo("/*\r\n" +
						"def\r\n" +
						"*/");
	}

	@Test
	void testLineComment() {
		String text = "1 -- text";
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closedOpen(2, text.length()), IN_LINE_COMMENT));
	}

	@Test
	void testSingleQuoteEscape() {
		String text = "'before''after'";
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closed(0, text.length() - 1), IN_STRING_LITERAL));
	}

	@Test
	void testBackslashDoesNotEscapeSingleQuote() {
		String text = "select 'text\\' as text";
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.SQL);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closed(7, 13), IN_STRING_LITERAL));
	}
}
