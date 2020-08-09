package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BEXStringTest {

	@Test
	void testMatchWithStringLiteral() {
		String pattern = "\":[value]\"";
		String text = "Line 1\n"
				+ "\"text(\\\"blah\"\n"
				+ "Line 3";

		BEXString bexString = new BEXString(text);

		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(bexString);

		assertTrue(bexMatcher.find(), "Could not find match");

		assertThat(bexMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo("text(\\\"blah");
	}

	@Test
	void testSubstringMatchWithStringLiteral() {
		String pattern = "\":[value]\"";
		String text = "Line 1\n"
				+ "\"text(\\\"blah\"\n"
				+ "Line 3";

		// Get substring starting with line 2
		int index = text.indexOf('"');
		BEXString bexString = new BEXString(text).substring(index, text.length());

		assertThat(bexString.getText()).isEqualTo(text.substring(index));

		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(bexString);

		assertThat(bexMatcher.text()).isEqualTo(text.substring(index));

		assertTrue(bexMatcher.find(), "Could not find match");

		assertThat(bexMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo("text(\\\"blah");
	}

	@Test
	void testExpressionEndsThenAnotherImmediatelyStarts() {
		String text = "<a href=\"<%=value1 %><%= value2 %>\"><%=value3 %></a>";
		BEXString bexString = new BEXString(text, BEXMatchingLanguage.JSP);

		bexString.getTextStateMap().asMapOfRanges();

		assertThat(bexString.getTextStateMap().asMapOfRanges())
				.containsExactly(entry(closedOpen(8, 9), IN_STRING_LITERAL),
						entry(closed(9, 20), IN_EXPRESSION_BLOCK),
						entry(closed(21, 33), IN_EXPRESSION_BLOCK),
						entry(closed(34, 34), IN_STRING_LITERAL),
						entry(closed(36, 47), IN_EXPRESSION_BLOCK));
	}
}
