package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.IntBEXRange.singleton;
import static info.codesaway.bex.parsing.BEXParsingState.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.parsing.BEXParsingState.IN_LINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_SECONDARY_MULTILINE_COMMENT;
import static info.codesaway.bex.parsing.BEXParsingState.IN_SECONDARY_STRING_LITERAL;
import static info.codesaway.bex.parsing.BEXParsingState.IN_STRING_LITERAL;
import static info.codesaway.bex.parsing.BEXParsingState.IN_TAG;
import static info.codesaway.bex.parsing.BEXParsingState.LINE_TERMINATOR;
import static info.codesaway.bex.parsing.BEXParsingState.WHITESPACE;
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
						entry(closedOpen(0, 2), IN_TAG),
						entry(singleton(2), parsingState(WHITESPACE, tagParent)),
						entry(closedOpen(3, 8), IN_TAG),
						entry(closedOpen(8, 9), parsingState(IN_STRING_LITERAL, tagParent)),
						entry(closedOpen(9, 18), expressionInString),
						entry(singleton(18), parsingState(WHITESPACE, index(9, expressionInString))),
						entry(closed(19, 20), expressionInString),
						entry(closedOpen(21, 24), expressionInString),
						entry(singleton(24), parsingState(WHITESPACE, index(21, expressionInString))),
						entry(closedOpen(25, 31), expressionInString),
						entry(singleton(31), parsingState(WHITESPACE, index(21, expressionInString))),
						entry(closed(32, 33), expressionInString),
						entry(singleton(34), parsingState(IN_STRING_LITERAL, tagParent)),
						entry(singleton(35), IN_TAG),
						entry(closedOpen(36, 45), IN_EXPRESSION_BLOCK),
						entry(singleton(45), parsingState(WHITESPACE, index(36, IN_EXPRESSION_BLOCK))),
						entry(closed(46, 47), IN_EXPRESSION_BLOCK),
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
		ParsingState whitespaceInExpression = parsingState(WHITESPACE, expressionParent);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(singleton(1), WHITESPACE),
						entry(singleton(6), WHITESPACE),
						entry(singleton(9), WHITESPACE),
						entry(closed(10, 31), IN_MULTILINE_COMMENT),
						entry(singleton(32), WHITESPACE),
						entry(singleton(36), WHITESPACE),
						entry(closed(37, 58), IN_SECONDARY_MULTILINE_COMMENT),
						entry(closed(59, 60), LINE_TERMINATOR),
						entry(singleton(63), WHITESPACE),
						entry(singleton(68), WHITESPACE),
						entry(singleton(71), WHITESPACE),
						entry(closedOpen(72, 74), IN_EXPRESSION_BLOCK),
						entry(singleton(74), whitespaceInExpression),
						entry(closedOpen(75, 77), IN_EXPRESSION_BLOCK),
						entry(singleton(77), whitespaceInExpression),
						entry(closedOpen(78, 82), IN_EXPRESSION_BLOCK),
						entry(singleton(82), whitespaceInExpression),
						// Part of expression block (issue #105)
						entry(closed(83, 121), parsingState(IN_MULTILINE_COMMENT, expressionParent)),
						entry(closed(122, 123), parsingState(LINE_TERMINATOR, expressionParent)),
						entry(closedOpen(124, 128), IN_EXPRESSION_BLOCK),
						entry(singleton(128), whitespaceInExpression),
						entry(closedOpen(129, 133), IN_EXPRESSION_BLOCK),
						entry(singleton(133), whitespaceInExpression),
						// Part of expression block (issue #105)
						entry(closed(134, 150), parsingState(IN_LINE_COMMENT, expressionParent)),
						entry(closed(151, 152), IN_EXPRESSION_BLOCK));
	}

	@Test
	void testLineTerminatorInTag() {
		String text = "<blah\r\n"
				+ "text='text' \r\n"
				+ "></blah>";
		BEXString bexString = new BEXString(text, BEXParsingLanguage.JSP);

		Indexed<ParsingState> tagParent = index(0, IN_TAG);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(
						entry(closedOpen(0, 5), IN_TAG),
						entry(closed(5, 6), parsingState(LINE_TERMINATOR, tagParent)),
						entry(closedOpen(7, 12), IN_TAG),
						entry(closed(12, 17), parsingState(IN_SECONDARY_STRING_LITERAL, tagParent)),
						entry(singleton(18), parsingState(WHITESPACE, tagParent)),
						entry(closed(19, 20), parsingState(LINE_TERMINATOR, tagParent)),
						entry(singleton(21), IN_TAG),
						entry(closed(22, 28), IN_TAG));
	}
}
