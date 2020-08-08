package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_SECONDARY_STRING_LITERAL;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;

import java.util.ArrayDeque;

import info.codesaway.bex.ImmutableIntRangeMap;
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
	public static ImmutableIntRangeMap<BEXMatchingStateOption> extractJavaTextStates(final CharSequence text) {
		if (text.length() == 0) {
			return ImmutableIntRangeMap.of();
		}

		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		ImmutableIntRangeMap.Builder<BEXMatchingStateOption> builder = ImmutableIntRangeMap.builder();
		ArrayDeque<BEXMatchingStateOption> stateStack = new ArrayDeque<>();
		ArrayDeque<Integer> startTextInfoStack = new ArrayDeque<>();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (stateStack.peek() == IN_STRING_LITERAL) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						break;
					}

					i++;
				} else if (c == '"') {
					// End of String literal
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (stateStack.peek() == IN_SECONDARY_STRING_LITERAL) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						break;
					}

					i++;
				} else if (c == '\'') {
					// End of String literal
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
				// Other characters don't matter??
			} else if (stateStack.peek() == IN_LINE_COMMENT) {
				if (c == '\n' || c == '\r') {
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.of(startTextInfo, i), stateStack.pop());
				}
				// Other characters don't matter?
			} else if (stateStack.peek() == IN_MULTILINE_COMMENT) {
				if (hasText(text, i, "*/")) {
					i++;
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
			} else if (c == '/' && nextChar(text, i) == '/') {
				stateStack.push(IN_LINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '/' && nextChar(text, i) == '*') {
				stateStack.push(IN_MULTILINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '"') {
				stateStack.push(IN_STRING_LITERAL);
				startTextInfoStack.push(i);
			} else if (c == '\'') {
				stateStack.push(IN_SECONDARY_STRING_LITERAL);
				startTextInfoStack.push(i);
			}
		}

		if (!stateStack.isEmpty()) {
			// TODO: what if there are multiple entries?
			// (this would suggest improperly formatted code)
			int startTextInfo = startTextInfoStack.pop();
			builder.put(IntBEXRange.of(startTextInfo, text.length()), stateStack.pop());
		}

		return builder.build();
	}

	/**
	 * Extracts <code>BEXMatchingTextState</code>s from the specified JSP text
	 * @param text the JSP text
	 * @return an unmodifiable map from the range start to the BEXMatchingTextState
	 */
	public static ImmutableIntRangeMap<BEXMatchingStateOption> extractJSPTextStates(final CharSequence text) {
		// TODO: used Java as a basic and need to enhance
		// For example, to handle JSP Expression
		// https://www.tutorialspoint.com/jsp/jsp_syntax.htm

		// TODO: need to make RangeMap class and correctly and nested ranges
		// Currently, doesn't work as expected
		// "stuff <%= expression%> more stuff"
		// "More stuff" after the expression should be seen as part of the String literal,
		// but isn't since it gets the last range, which is the expression, which is over
		// Think can fix by end the state when go into a inner state
		// Then, when leave inner state, start a new state based on the outer state

		// TODO: make RangeMap class to handle this
		// When adding a new record, check for overlap using the below logic
		// + An overlap occurs if and only if
		// a) The added range's start in part of an existing range
		// * Can check by finding existing range in map and seeing if the added range's start is in the middle
		// * BEXUtilities.getEntryInRanges
		// b) An existing range's start is contained in the new range
		// * Can do a subRange check on the existing NavigableMap and see if there are any entries
		// If there's an overlap, handle by breaking apart ranges in pieces

		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		ImmutableIntRangeMap.Builder<BEXMatchingStateOption> builder = ImmutableIntRangeMap.builder();
		ArrayDeque<BEXMatchingStateOption> stateStack = new ArrayDeque<>();
		ArrayDeque<Integer> startTextInfoStack = new ArrayDeque<>();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (stateStack.peek() == IN_STRING_LITERAL) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						break;
					}

					i++;
				} else if (c == '"') {
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				} else if (hasText(text, i, "<%=")) {
					// Going into second level, so end current level
					int startTextInfo = startTextInfoStack.pop();
					if (startTextInfo != i) {
						// Only add if not empty range
						// Would be empty for example if ended one expression then immediately started next one
						builder.put(IntBEXRange.of(startTextInfo, i), stateStack.peek());
					}

					stateStack.push(IN_EXPRESSION_BLOCK);
					startTextInfoStack.push(i);
					i += 2;
				}

				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (stateStack.peek() == IN_SECONDARY_STRING_LITERAL) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						break;
					}

					i++;
				} else if (c == '\'') {
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				} else if (hasText(text, i, "<%=")) {
					// Going into second level, so end current level
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.of(startTextInfo, i), stateStack.peek());

					stateStack.push(IN_EXPRESSION_BLOCK);
					startTextInfoStack.push(i);
					i += 2;
				}

				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (stateStack.peek() == IN_LINE_COMMENT) {
				if (c == '\n' || c == '\r') {
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.of(startTextInfo, i), stateStack.pop());
				}
				// Other characters don't matter?
			} else if (stateStack.peek() == IN_MULTILINE_COMMENT) {
				if (hasText(text, i, "*/")) {
					i++;
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
			} else if (stateStack.peek() == IN_EXPRESSION_BLOCK) {
				if (hasText(text, i, "%>")) {
					i++;
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());

					if (!stateStack.isEmpty()) {
						// Inside a first level, so add startTextInfo for after expression blocks ends
						startTextInfoStack.push(i + 1);
					}
				}
			} else if (c == '/' && nextChar(text, i) == '/') {
				stateStack.push(IN_LINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '/' && nextChar(text, i) == '*') {
				stateStack.push(IN_MULTILINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '"') {
				stateStack.push(IN_STRING_LITERAL);
				startTextInfoStack.push(i);
			} else if (c == '\'') {
				stateStack.push(IN_SECONDARY_STRING_LITERAL);
				startTextInfoStack.push(i);
			} else if (hasText(text, i, "<%=")) {
				stateStack.push(IN_EXPRESSION_BLOCK);
				startTextInfoStack.push(i);
				i += 2;
			}
		}

		if (!stateStack.isEmpty()) {
			// TODO: what if there are multiple entries?
			// (this would suggest improperly formatted code)
			int startTextInfo = startTextInfoStack.pop();
			builder.put(IntBEXRange.of(startTextInfo, text.length()), stateStack.pop());
		}

		return builder.build();
	}
}
