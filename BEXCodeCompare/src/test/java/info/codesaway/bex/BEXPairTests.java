package info.codesaway.bex;

import static info.codesaway.bex.BEXPairs.bexPair;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class BEXPairTests {
	@Test
	void testHasEqualValues() {
		BigInteger value1 = new BigInteger("1");
		BigInteger value2 = new BigInteger("1");

		assertTrue(bexPair(value1, value2).hasEqualValues());
	}
}
