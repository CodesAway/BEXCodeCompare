package info.codesaway.bex.matching;

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
}
