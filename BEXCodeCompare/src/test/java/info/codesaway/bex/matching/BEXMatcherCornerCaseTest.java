package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.MatcherTestHelper.testJustBEXMatch;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BEXMatcherCornerCaseTest {
	@Test
	void testMatchEndsWithMultilineComment() {
		String pattern = "method(:[1], :[2])";
		String text = "method(1/**/, 2)";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.get("1")).isEqualTo("1/**/");
		assertThat(bexMatcher.get("2")).isEqualTo("2");
	}

	@Test
	void testMatchEndsWithStringLiteral() {
		String pattern = "method(:[1], :[2])";
		String text = "method(\"1\", 2)";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.get("1")).isEqualTo("\"1\"");
		assertThat(bexMatcher.get("2")).isEqualTo("2");
	}

	@Test
	void testMatchIgnorePatternInStringLiteral() {
		String pattern = "method(:[1], :[2])";
		String text = "method(\"1,2,3\", 2)";

		BEXMatcher bexMatcher = testJustBEXMatch(pattern, text);
		assertThat(bexMatcher.get("1")).isEqualTo("\"1,2,3\"");
		assertThat(bexMatcher.get("2")).isEqualTo("2");
	}
}
