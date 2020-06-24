package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JavaRefactoringTests {
	@Test
	public void testNotEqualsWithSpecialHandling() {
		String text1 = "Book Case";
		String text2 = "BookCase";

		assertFalse(equalsWithSpecialHandling(text1, text2));
		assertFalse(equalsWithSpecialHandling(text2, text1));
	}

	@Test
	public void testEqualsWithSpecialHandling() {
		String text1 = "Book" + (char) 0 + "Case";
		String text2 = "Book Case";

		assertTrue(equalsWithSpecialHandling(text1, text2));
		assertTrue(equalsWithSpecialHandling(text2, text1));
	}
}
