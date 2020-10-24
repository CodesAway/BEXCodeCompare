package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import info.codesaway.bex.IntBEXRange;

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
	void testIsCommentedOut() {
		String text = "/*  + \" AND (something \"\r\n" +
				"+ \" OR \"\r\n" +
				"+ \" something else \" *//*block comment*///comment";

		BEXString bexString = new BEXString(text);

		IntBEXRange range = IntBEXRange.of(0, text.length());

		assertTrue(bexString.isComment(range));
	}

	@Test
	void testIsCommentedOutIgnoreWhitespace() {
		String text = "/*  + \" AND (something \"\r\n" +
				"+ \" OR \"\r\n" +
				"+ \" something else \" */ /*block comment*/     //comment\r\n";

		BEXString bexString = new BEXString(text);

		IntBEXRange range = IntBEXRange.of(0, text.length());

		assertTrue(bexString.isComment(range));
	}

	@Test
	void testNotCommentedOutDueToWhitespace() {
		String text = "/*  + \" AND (something \"\r\n" +
				"+ \" OR \"\r\n" +
				"+ \" something else \" */ /*block comment*/     //comment\r\n";

		BEXString bexString = new BEXString(text);

		IntBEXRange range = IntBEXRange.of(0, text.length());

		assertFalse(bexString.isComment(range, false));
	}
}
