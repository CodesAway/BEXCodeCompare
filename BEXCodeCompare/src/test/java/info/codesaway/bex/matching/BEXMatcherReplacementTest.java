package info.codesaway.bex.matching;

import static info.codesaway.bex.util.BEXUtilities.contentEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BEXMatcherReplacementTest {
	@Test
	void testAppendReplacementJavadocExample() {
		BEXPattern p = BEXPattern.compile("cat");
		BEXMatcher m = p.matcher("one cat two cats in the yard");
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "dog");
		}
		m.appendTail(sb);
		assertTrue(contentEquals(sb, "one dog two dogs in the yard"));
	}

	@Test
	void testReplaceAllJavadocExample() {
		BEXPattern p = BEXPattern.compile("cat");
		BEXMatcher m = p.matcher("one cat two cats in the yard");

		assertTrue(contentEquals(m.replaceAll("dog"), "one dog two dogs in the yard"));
	}

	@Test
	void testReplaceAllUsingFunctionJavadocExample() {
		BEXPattern p = BEXPattern.compile("cat");
		BEXMatcher m = p.matcher("one cat two cats in the yard");

		assertTrue(contentEquals(m.replaceAll(x -> "dog"), "one dog two dogs in the yard"));
	}

	@Test
	void testReplaceAllDigitsWithReversal() {
		BEXPattern pattern = BEXPattern.compile(":[num:d]");
		BEXMatcher matcher = pattern.matcher("123 cats 456 dogs and 789 birds");

		assertTrue(contentEquals(
				matcher.replaceAll(m -> new StringBuilder(m.group("num")).reverse().toString()),
				"321 cats 654 dogs and 987 birds"));
	}

}