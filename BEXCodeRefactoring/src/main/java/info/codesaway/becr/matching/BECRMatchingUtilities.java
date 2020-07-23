package info.codesaway.becr.matching;

class BECRMatchingUtilities {
	static String stringChar(final String text, final int index) {
		return text.substring(index, index + 1);
	}

	static char prevChar(final CharSequence text, final int index) {
		if (index > 0) {
			return text.charAt(index - 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	static char nextChar(final CharSequence text, final int index) {
		if (index < text.length() - 1) {
			return text.charAt(index + 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	static char lastChar(final CharSequence text) {
		if (text.length() != 0) {
			return text.charAt(text.length() - 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	static boolean isWordCharacter(final char c) {
		return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
	}

	static boolean hasText(final CharSequence text, final int startIndex, final String search) {
		int index = startIndex;

		if (search.length() > text.length() - startIndex) {
			return false;
		}

		for (int i = 0; i < search.length(); i++) {
			char c = text.charAt(index++);
			if (c != search.charAt(i)) {
				return false;
			}
		}

		return true;
	}
}
