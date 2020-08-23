package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BEXParseJSPTest {
	@Test
	void testExpressionEndsThenAnotherImmediatelyStarts() {
		String text = "<a href=\"<%=value1 %><%= value2 %>\"><%=value3 %></a>";
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.JSP);

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closedOpen(8, 9), IN_STRING_LITERAL),
						entry(closed(9, 20), IN_EXPRESSION_BLOCK),
						entry(closed(21, 33), IN_EXPRESSION_BLOCK),
						entry(closed(34, 34), IN_STRING_LITERAL),
						entry(closed(36, 47), IN_EXPRESSION_BLOCK));
	}
}
