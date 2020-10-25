package info.codesaway.bex.matching;

import static info.codesaway.bex.parsing.BEXParsingUtilities.hasText;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BEXMatchingUtilitiesTest {

	@Test
	void testHasTextCornerCase() {
		String text = "12345";
		int index = 1;

		assertTrue(hasText(text, index, "2345"));
	}

	@Test
	void testHasTextSearchTooLong() {
		String text = "12345";
		int index = 1;

		assertFalse(hasText(text, index, "23456"));
	}
}
