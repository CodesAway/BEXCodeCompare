package info.codesaway.bex;

import static info.codesaway.bex.BEXPairs.bexPair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class BEXPairCompare {
	@Test
	void testStringCompare() {
		String left1 = "abc";
		String right1 = "def";

		String left2 = "def";
		String right2 = "abc";

		assertThat(bexPair(left1, right1).compareTo(bexPair(left2, right2))).isNegative();
		assertThat(bexPair(left2, right2).compareTo(bexPair(left1, right1))).isPositive();
	}

	@Test
	void testObjectCompare() {
		Object left1 = new Object();
		Object right1 = new Object();

		Object left2 = new Object();
		Object right2 = new Object();

		assertThatThrownBy(() -> bexPair(left1, right1).compareTo(bexPair(left2, right2)))
				.isInstanceOf(ClassCastException.class)
				.hasMessage("java.lang.Object cannot be cast to java.lang.Comparable");
	}

	@Test
	void testStringObjectCompare() {
		Object left1 = "abc";
		Object right1 = "def";

		Object left2 = new Object();
		Object right2 = new Object();

		assertThatThrownBy(() -> bexPair(left1, right1).compareTo(bexPair(left2, right2)))
				.isInstanceOf(ClassCastException.class)
				.hasMessage("java.lang.Object cannot be cast to java.lang.String");
	}

	@Test
	void testStringFirstEqual() {
		Object left1 = "abc";
		Object right1 = "def";

		String left2 = "abc";
		String right2 = "z";

		assertThat(bexPair(left1, right1).compareTo(bexPair(left2, right2))).isNegative();
		assertThat(bexPair((Object) left2, right2).compareTo(bexPair(left1, right1))).isPositive();
	}

	@Test
	void testStringBothEqual() {
		Object left1 = "abc";
		Object right1 = "def";

		String left2 = "abc";
		String right2 = "def";

		assertThat(bexPair(left1, right1).compareTo(bexPair(left2, right2))).isZero();
		assertThat(bexPair((Object) left2, right2).compareTo(bexPair(left1, right1))).isZero();
	}

	@Test
	void testNotMutuallyComparable() {
		String left1 = "abc";
		String right1 = "def";

		Integer left2 = 1;
		Integer right2 = 2;

		assertThatThrownBy(() -> bexPair((Object) left1, right1).compareTo(bexPair(left2, right2)))
				.isInstanceOf(ClassCastException.class)
				.hasMessage("java.lang.Integer cannot be cast to java.lang.String");
	}
}
