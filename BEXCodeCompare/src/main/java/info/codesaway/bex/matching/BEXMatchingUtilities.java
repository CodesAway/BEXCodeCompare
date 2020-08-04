package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import info.codesaway.bex.IntBEXRange;

public class BEXMatchingUtilities {
	public static String stringChar(final String text, final int index) {
		return text.substring(index, index + 1);
	}

	/**
	 * Indicates whether there is a previous character before the specified index
	 * @param text the text
	 * @param index the index
	 * @return <code>true</code> if there is a previous character before the specified index; <code>false</code> otherwise
	 * @since 0.6
	 */
	public static boolean hasPreviousChar(final CharSequence text, final int index) {
		return index > 0;
	}

	/**
	 * Gets the previous character
	 * @param text the text
	 * @param index the index
	 * @return the previous character or \0, null character, if there is no character before the specified index
	 */
	public static char previousChar(final CharSequence text, final int index) {
		if (hasPreviousChar(text, index)) {
			return text.charAt(index - 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	/**
	 * Indicates whether there is a next character after the specified index
	 * @param text the text
	 * @param index the index
	 * @return <code>true</code> if there is a next character after the specified index; <code>false</code> otherwise
	 * @since 0.6
	 */
	public static boolean hasNextChar(final CharSequence text, final int index) {
		return index < text.length() - 1;
	}

	/**
	 * Gets the next character
	 * @param text the text
	 * @param index the index
	 * @return the next character or \0, null character, if there is no character after the specified index
	 */
	public static char nextChar(final CharSequence text, final int index) {
		if (hasNextChar(text, index)) {
			return text.charAt(index + 1);
		} else {
			// Return null character to indicate nothing found
			return '\0';
		}
	}

	/**
	 * Gets the current character
	 * @param text the text
	 * @param index the index
	 * @return the current character or \0, null character, if there is no character at the specified index
	 * @since 0.6
	 */
	public static char currentChar(final CharSequence text, final int index) {
		if (index < text.length() && index >= 0) {
			return text.charAt(index);
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
	 * Extracts <code>BEXMatchingTextState</code>s from the specified Java text
	 * @param text the Java text
	 * @return an unmodifiable map from the range start to the BEXMatchingTextState
	 */
	public static NavigableMap<Integer, BEXMatchingTextState> extractJavaTextStates(final CharSequence text) {
		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		/**
		 * Map from range start to BEXMatchingTextState (contains range and text state)
		 */
		NavigableMap<Integer, BEXMatchingTextState> textStateMap = new TreeMap<>();

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
							new BEXMatchingTextState(IntBEXRange.of(startTextInfo, i), IN_STRING_LITERAL));
				}
				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (isInLineComment) {
				if (c == '\n' || c == '\r') {
					isInLineComment = false;
					textStateMap.put(startTextInfo,
							new BEXMatchingTextState(IntBEXRange.of(startTextInfo, i), IN_LINE_COMMENT));
				}
				// Other characters don't matter?
			} else if (isInMultilineComment) {
				if (hasText(text, i, "*/")) {
					isInMultilineComment = false;
					i++;
					textStateMap.put(startTextInfo,
							new BEXMatchingTextState(IntBEXRange.of(startTextInfo, i), IN_MULTILINE_COMMENT));
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
					new BEXMatchingTextState(IntBEXRange.of(startTextInfo, text.length()), IN_LINE_COMMENT));
		} else if (isInMultilineComment) {
			textStateMap.put(startTextInfo,
					new BEXMatchingTextState(IntBEXRange.of(startTextInfo, text.length()), IN_MULTILINE_COMMENT));
		} else if (isInStringLiteral) {
			textStateMap.put(startTextInfo,
					new BEXMatchingTextState(IntBEXRange.of(startTextInfo, text.length()), IN_STRING_LITERAL));
		}

		return Collections.unmodifiableNavigableMap(textStateMap);
	}
}
