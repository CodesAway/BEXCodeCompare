package info.codesaway.becr.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BECRStringTest {

	@Test
	void testMatchWithStringLiteral() {
		String pattern = "\":[value]\"";
		String text = "Line 1\n"
				+ "\"text(\\\"blah\"\n"
				+ "Line 3";

		BECRString becrString = new BECRString(text);

		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(becrString);

		assertTrue(becrMatcher.find(), "Could not find match");

		assertThat(becrMatcher)
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
		BECRString becrString = new BECRString(text).substring(index, text.length());

		assertThat(becrString.getText()).isEqualTo(text.substring(index));

		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(becrString);

		assertThat(becrMatcher.text()).isEqualTo(text.substring(index));

		assertTrue(becrMatcher.find(), "Could not find match");

		assertThat(becrMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo("text(\\\"blah");
	}
}
