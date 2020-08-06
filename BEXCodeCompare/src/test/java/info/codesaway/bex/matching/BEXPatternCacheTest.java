package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BEXPatternCacheTest {
	@Test
	void testPatternCache() {
		String pattern = "something";
		BEXPattern p1 = BEXPattern.compile(pattern);
		BEXPattern p2 = BEXPattern.compile(pattern);

		assertThat(p1).isSameAs(p2);
	}
}
