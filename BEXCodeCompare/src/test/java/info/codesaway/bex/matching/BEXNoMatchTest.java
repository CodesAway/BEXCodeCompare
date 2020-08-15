package info.codesaway.bex.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BEXNoMatchTest {
	private static final BEXPattern PATTERN = BEXPattern.compile("fun");
	private static final String TEXT = "nothing enjoyable";
	private static final String TEXT_NO_FIND = "fun";

	private final BEXMatcher bexMatcher = PATTERN.matcher();
	private final BEXMatcher bexMatcherNoFind = PATTERN.matcher();
	private BEXMatchResult bexMatchResult;
	private BEXMatchResult bexMatchResultNoFind;

	@BeforeEach
	void testSetup() {
		this.bexMatcher.reset(TEXT);
		assertFalse(this.bexMatcher.find(), "Should not find match");

		this.bexMatcherNoFind.reset(TEXT_NO_FIND);

		this.bexMatchResult = this.bexMatcher.toMatchResult();
		this.bexMatchResultNoFind = this.bexMatcherNoFind.toMatchResult();
	}

	@Test
	void testNoMatchStart() {
		this.testNoMatch(this.bexMatcher::start);
		this.testNoMatch(this.bexMatcherNoFind::start);

		this.testNoMatch(this.bexMatchResult::start);
		this.testNoMatch(this.bexMatchResultNoFind::start);
	}

	@Test
	void testNoMatchEnd() {
		this.testNoMatch(this.bexMatcher::end);
		this.testNoMatch(this.bexMatcherNoFind::end);

		this.testNoMatch(this.bexMatchResult::end);
		this.testNoMatch(this.bexMatchResultNoFind::end);
	}

	@Test
	void testNoMatchGroup() {
		this.testNoMatch(this.bexMatcher::group);
		this.testNoMatch(this.bexMatcherNoFind::group);

		this.testNoMatch(this.bexMatchResult::group);
		this.testNoMatch(this.bexMatchResultNoFind::group);
	}

	@Test
	void testNoMatchRange() {
		this.testNoMatch(this.bexMatcher::range);
		this.testNoMatch(this.bexMatcherNoFind::range);

		this.testNoMatch(this.bexMatchResult::range);
		this.testNoMatch(this.bexMatchResultNoFind::range);
	}

	@Test
	void testNoMatchEntrySet() {
		this.testNoMatch(this.bexMatcher::entrySet);
		this.testNoMatch(this.bexMatcherNoFind::entrySet);

		this.testNoMatch(this.bexMatchResult::entrySet);
		this.testNoMatch(this.bexMatchResultNoFind::entrySet);
	}

	void testNoMatchStartWithGroup() {
		this.testNoMatch(() -> this.bexMatcher.start("_"));
		this.testNoMatch(() -> this.bexMatcherNoFind.start("_"));

		this.testNoMatch(() -> this.bexMatchResult.start("_"));
		this.testNoMatch(() -> this.bexMatchResultNoFind.start("_"));
	}

	@Test
	void testNoMatchEndWithGroup() {
		this.testNoMatch(() -> this.bexMatcher.end("_"));
		this.testNoMatch(() -> this.bexMatcherNoFind.end("_"));

		this.testNoMatch(() -> this.bexMatchResult.end("_"));
		this.testNoMatch(() -> this.bexMatchResultNoFind.end("_"));
	}

	@Test
	void testNoMatchGroupWithGroup() {
		this.testNoMatch(() -> this.bexMatcher.group("_"));
		this.testNoMatch(() -> this.bexMatcherNoFind.group("_"));

		this.testNoMatch(() -> this.bexMatchResult.group("_"));
		this.testNoMatch(() -> this.bexMatchResultNoFind.group("_"));
	}

	@Test
	void testNoMatchRangeWithGroup() {
		this.testNoMatch(() -> this.bexMatcher.range("_"));
		this.testNoMatch(() -> this.bexMatcherNoFind.range("_"));

		this.testNoMatch(() -> this.bexMatchResult.range("_"));
		this.testNoMatch(() -> this.bexMatchResultNoFind.range("_"));
	}

	@Test
	void testNoMatchPattern() {
		assertThat(this.bexMatcher.pattern()).isEqualTo(PATTERN);
		assertThat(this.bexMatcherNoFind.pattern()).isEqualTo(PATTERN);

		assertThat(this.bexMatchResult.pattern()).isEqualTo(PATTERN);
		assertThat(this.bexMatchResultNoFind.pattern()).isEqualTo(PATTERN);
	}

	@Test
	void testNoMatchText() {
		assertThat(this.bexMatcher.text()).isEqualTo(TEXT);
		assertThat(this.bexMatcherNoFind.text()).isEqualTo(TEXT_NO_FIND);

		assertThat(this.bexMatchResult.text()).isEqualTo(TEXT);
		assertThat(this.bexMatchResultNoFind.text()).isEqualTo(TEXT_NO_FIND);
	}

	private void testNoMatch(final ThrowingCallable shouldRaiseThrowable) {
		assertThatThrownBy(shouldRaiseThrowable)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("No match available");
	}
}
