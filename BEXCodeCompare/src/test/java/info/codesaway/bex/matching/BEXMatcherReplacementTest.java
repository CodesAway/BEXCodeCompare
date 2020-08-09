package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

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
		assertThat(sb).asString().isEqualTo("one dog two dogs in the yard");
	}

	@Test
	void testReplaceAllJavadocExample() {
		BEXPattern p = BEXPattern.compile("cat");
		BEXMatcher m = p.matcher("one cat two cats in the yard");

		assertThat(m.replaceAll("dog")).isEqualTo("one dog two dogs in the yard");
	}

	@Test
	void testReplaceAllUsingFunctionJavadocExample() {
		BEXPattern p = BEXPattern.compile("cat");
		BEXMatcher m = p.matcher("one cat two cats in the yard");

		assertThat(m.replaceAll(x -> "dog")).isEqualTo("one dog two dogs in the yard");
	}

	@Test
	void testReplaceAllDigitsWithReversal() {
		BEXPattern pattern = BEXPattern.compile(":[num:d]");
		BEXMatcher matcher = pattern.matcher("123 cats 456 dogs and 789 birds");

		Function<BEXMatchResult, String> reverseMatches = m -> new StringBuilder(m.group("num")).reverse().toString();

		assertThat(matcher.replaceAll(reverseMatches)).isEqualTo("321 cats 654 dogs and 987 birds");
	}

	@Test
	void testReplaceFirstJavadocExample() {
		BEXPattern p = BEXPattern.compile("dog");
		BEXMatcher m = p.matcher("zzzdogzzzdogzzz");

		assertThat(m.replaceFirst("cat")).isEqualTo("zzzcatzzzdogzzz");
	}

	@Test
	void testReplaceFirstStar() {
		BEXPattern p = BEXPattern.compile("dog");
		BEXMatcher m = p.matcher("zzzdogzzzdogzzz");

		assertThat(m.replaceFirst("cat:[*]")).isEqualTo("zzzcatdogzzzdogzzz");
	}

	@Test
	void testReplaceWithGroup() {
		BEXPattern p = BEXPattern.compile(":[a] :[b] :[c]");
		BEXMatcher m = p.matcher("a b c");

		assertTrue(m.find(), "Could not find match");

		assertThat(m.getReplacement(":[a]/:[b]/:[c]")).isEqualTo("a/b/c");
	}

	// TODO: if didn't match, then shouldn't be able to get groups?
}