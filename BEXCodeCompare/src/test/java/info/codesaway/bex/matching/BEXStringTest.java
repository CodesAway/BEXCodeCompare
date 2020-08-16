package info.codesaway.bex.matching;

import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
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
	void testSubstringRange() {
		String pattern = "\":[value]\"";
		String text = "Line 1\n"
				+ "\"text(\\\"blah\"\n"
				+ "Line 3";

		BEXString bexString = new BEXString(text);

		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(bexString);

		assertTrue(bexMatcher.find(), "Could not find match");

		String value = bexMatcher.group("value");

		assertThat(value).isEqualTo("text(\\\"blah");

		assertThat(bexString.substring(bexMatcher.range("value")).getText()).isEqualTo(value);
	}

	@Test
	@Disabled("Doesn't match subpattern since within String literal (which is ignored)")
	// TODO: see if there's a way to handle this intelligently, so it can match
	void testSubstringRangeMatchSubpattern() {
		String pattern = "\":[value]\"";
		String text = "Line 1\n"
				+ "\"text(\\\"blah\"\n"
				+ "Line 3";

		BEXString bexString = new BEXString(text);

		BEXPattern bexPattern = BEXPattern.compile(pattern);
		BEXMatcher bexMatcher = bexPattern.matcher(bexString);

		assertTrue(bexMatcher.find(), "Could not find match");

		String value = bexMatcher.group("value");

		assertThat(value).isEqualTo("text(\\\"blah");

		System.out.println("Subpattern!");
		BEXPattern bexSubPattern = BEXPattern.compile("blah");
		BEXMatcher bexSubMatcher = bexSubPattern.matcher(bexString.substring(bexMatcher.range("value")));

		System.out.println("Subtext: " + bexSubMatcher.text());

		assertThat(bexSubMatcher.text()).asString().isEqualTo(value);
		assertTrue(bexSubMatcher.find(), "Could not find match");

		assertThat(bexSubMatcher.get("1")).isEqualTo("lah\"");
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
