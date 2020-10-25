package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.IntBEXRange.singleton;
import static info.codesaway.bex.parsing.BEXParsingState.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.parsing.BEXParsingState.IN_LINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_SECONDARY_MULTILINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_STRING_LITERAL;
import static info.codesaway.bex.parsing.BEXParsingState.IN_TAG;
import static info.codesaway.bex.parsing.BEXParsingUtilities.parsingState;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static info.codesaway.bex.util.BEXUtilities.index;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.Indexed;
import info.codesaway.bex.parsing.BEXParsingLanguage;
import info.codesaway.bex.parsing.BEXString;
import info.codesaway.bex.parsing.ParsingState;
import info.codesaway.bex.parsing.ParsingStateValue;

public class BEXParseJSPTest {
	@Test
	void testExpressionEndsThenAnotherImmediatelyStarts() {
		String text = "<a href=\"<%=value1 %><%= value2 %>\"><%=value3 %></a>";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.JSP);

		Indexed<ParsingState> tagParent = index(0, IN_TAG);
		// Handle nested parents
		ParsingState expressionInString = parsingState(IN_EXPRESSION_BLOCK,
				index(8, new ParsingStateValue(IN_STRING_LITERAL, tagParent)));

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(closedOpen(0, 8), IN_TAG),
						entry(closedOpen(8, 9), parsingState(IN_STRING_LITERAL, tagParent)),
						entry(closed(9, 20), expressionInString),
						entry(closed(21, 33), expressionInString),
						entry(singleton(34), parsingState(IN_STRING_LITERAL, tagParent)),
						entry(singleton(35), IN_TAG),
						entry(closed(36, 47), IN_EXPRESSION_BLOCK),
						entry(closed(48, 51), IN_TAG));
	}

	@Test
	// Issue #93
	void testComments() {
		String text = "I love my <%-- JSP Comments --%> and <!-- HTML comments -->\r\n"
				+ "As well as <% my code /* multi-line comment \r\n"
				+ "More comment */\r\n"
				+ "More code // more comments %>";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.JSP);

		Indexed<ParsingState> expressionParent = index(72, IN_EXPRESSION_BLOCK);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(closed(10, 31), IN_MULTILINE_COMMENT),
						entry(closed(37, 58), IN_SECONDARY_MULTILINE_COMMENT),
						entry(closedOpen(72, 83), IN_EXPRESSION_BLOCK),
						// Part of expression block
						// TODO: check this (issue #105)
						entry(closed(83, 121), parsingState(IN_MULTILINE_COMMENT, expressionParent)),
						entry(closedOpen(122, 134), IN_EXPRESSION_BLOCK),
						// Part of expression block
						// TODO: check this (issue #105)
						entry(closed(134, 150), parsingState(IN_LINE_COMMENT, expressionParent)),
						entry(closed(151, 152), IN_EXPRESSION_BLOCK));
	}
}
