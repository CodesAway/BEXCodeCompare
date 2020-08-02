package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRStateOption.IN_LINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_STRING_LITERAL;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import info.codesaway.becr.BECRRange;

public class BECRMatchingUtilities {
	public static String stringChar(final String text, final int index) {
		return text.substring(index, index + 1);
	}

	public static char prevChar(final CharSequence text, final int index) {
		if (index > 0) {
			return text.charAt(index - 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	public static char nextChar(final CharSequence text, final int index) {
		if (index < text.length() - 1) {
			return text.charAt(index + 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	public static char lastChar(final CharSequence text) {
		if (text.length() != 0) {
			return text.charAt(text.length() - 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	public static boolean isWordCharacter(final char c) {
		return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
	}

	public static boolean hasText(final CharSequence text, final int startIndex, final String search) {
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

	/**
	 * Extracts <code>BECRTextState</code>s from the specified Java text
	 * @param text the Java text
	 * @return an unmodifiable map from the range start to the BECRTextState
	 */
	public static NavigableMap<Integer, BECRTextState> extractJavaTextStates(final CharSequence text) {
		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		/**
		 * Map from range start to BECRTextState (contains range and text state)
		 */
		NavigableMap<Integer, BECRTextState> textStateMap = new TreeMap<>();

		boolean isInStringLiteral = false;
		boolean isInLineComment = false;
		boolean isInMultilineComment = false;

		int startTextInfo = -1;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (isInStringLiteral) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						break;
					}

					i++;
				} else if (c == '"') {
					// End of String literal
					isInStringLiteral = false;

					textStateMap.put(startTextInfo,
							new BECRTextState(BECRRange.of(startTextInfo, i), IN_STRING_LITERAL));
				}
				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (isInLineComment) {
				if (c == '\n' || c == '\r') {
					isInLineComment = false;
					textStateMap.put(startTextInfo,
							new BECRTextState(BECRRange.of(startTextInfo, i), IN_LINE_COMMENT));
				}
				// Other characters don't matter?
			} else if (isInMultilineComment) {
				if (hasText(text, i, "*/")) {
					isInMultilineComment = false;
					i++;
					textStateMap.put(startTextInfo,
							new BECRTextState(BECRRange.of(startTextInfo, i), IN_MULTILINE_COMMENT));
				}
			} else if (c == '/' && nextChar(text, i) == '/') {
				isInLineComment = true;
				startTextInfo = i;
				i++;
			} else if (c == '/' && nextChar(text, i) == '*') {
				isInMultilineComment = true;
				startTextInfo = i;
				i++;
			} else if (c == '"') {
				// String literal
				isInStringLiteral = true;
				startTextInfo = i;
			}
		}

		if (isInLineComment) {
			textStateMap.put(startTextInfo,
					new BECRTextState(BECRRange.of(startTextInfo, text.length()), IN_LINE_COMMENT));
		}

		return Collections.unmodifiableNavigableMap(textStateMap);
	}
}
