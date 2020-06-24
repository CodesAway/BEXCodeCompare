package info.codesaway.bex.diff.substitution.java;

import info.codesaway.bex.diff.substitution.SubstitutionType;

public interface JavaSubstitution extends SubstitutionType {
	public static String enhanceRegexWhitespace(final String regex) {
		return regex.replace(" ", "\\s*+");
	}

	// Added to help prevent SpotBugs warning when intentionally compare String identity
	public static boolean identityEquals(final Object object1, final Object object2) {
		return object1 == object2;
	}

	public static boolean equalsWithSpecialHandling(final String text1, final String text2) {
		// Checks if the text is equal
		// Has special handling for null character (char 0) so can handle differences due to refactoring
		int index1 = 0;
		int index2 = 0;

		boolean allowSpace1 = false;
		boolean allowSpace2 = false;

		while (index1 < text1.length() && index2 < text2.length()) {
			char c1 = text1.charAt(index1);
			char c2 = text2.charAt(index2);

			if (c1 == c2) {
				index1++;
				index2++;
				allowSpace1 = false;
				allowSpace2 = false;
			} else if (allowSpace1 && c1 == ' ') {
				index1++;
				allowSpace1 = false;
			} else if (allowSpace2 && c2 == ' ') {
				index2++;
				allowSpace2 = false;
			}
			// Special handling for char 0
			else if (c1 == 0) {
				index1++;
				allowSpace2 = true;
			} else if (c2 == 0) {
				index2++;
				allowSpace1 = true;
			} else {
				return false;
			}
		}

		// Ignore char 0 if at end of text
		while (index1 < text1.length() && text1.charAt(index1) == 0) {
			index1++;
		}

		while (index2 < text2.length() && text2.charAt(index2) == 0) {
			index2++;
		}

		return index1 == text1.length() && index2 == text2.length();
	}
}
