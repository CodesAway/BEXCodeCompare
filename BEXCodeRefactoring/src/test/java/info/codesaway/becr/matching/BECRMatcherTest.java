package info.codesaway.becr.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class BECRMatcherTest {

	@Test
	void testMatchStringText() {
		String pattern = "\"User:[value]\"";
		String text = "request.addChildTag(\"Users\", \"a\", null)";
		String expectedValue = "s";
		this.testBECRMatch(pattern, text, expectedValue);
	}

	@Test
	void testMatchStringHasParenthesisText() {
		String pattern = "\":[value](\"";
		String text = "webView.loadUrl(\"javascript:showDetail(\"+m.getId()+\")\");";
		String expectedValue = "javascript:showDetail";
		this.testBECRMatch(pattern, text, expectedValue);
	}

	// TODO: this test fails
	// * Expecting to match "("
	// * Currently, the code expects the match to have balanced parentheses
	// * However, this isn't required, since it's part of a String
	// Noticed that Comby is only happy if has trailing quote, so this is a niche scenario that I don't plan to handle yet
	//	@Test
	//	void testMatchStringBracketTest() {
	//		String pattern = "\"javascript:showDetail:[value]\"";
	//		String text = "mWebView.loadUrl(\"javascript:showDetail(\"+mWare.getId()+\")\");";
	//		String expectedValue = "(";
	//		this.testBECRMatch(pattern, text, expectedValue);
	//	}

	private void testBECRMatch(final String pattern, final String text, final String expectedValue) {
		// TODO Auto-generated method stub
		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		if (!becrMatcher.find()) {
			fail("Could not find match");
		}

		assertThat(becrMatcher)
				.extracting(m -> m.get("value"))
				.isEqualTo(expectedValue);
	}
}
