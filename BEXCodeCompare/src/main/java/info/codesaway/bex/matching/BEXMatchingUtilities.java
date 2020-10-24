package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_EXPRESSION_BLOCK;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_SECONDARY_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_SECONDARY_STRING_LITERAL;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_TAG;
import static info.codesaway.bex.matching.BEXMatchingStateOption.LINE_TERMINATOR;
import static info.codesaway.bex.matching.BEXMatchingStateOption.WHITESPACE;

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

	/**
	 *
	 * @param text
	 * @param startIndex
	 * @param search
	 * @param isCaseInsensitive
	 * @return
	 * @since 0.11
	 */
	public static boolean hasText(final CharSequence text, final int startIndex, final String search,
			final boolean isCaseInsensitive) {
		return isCaseInsensitive
				? hasCaseInsensitiveText(text, startIndex, search)
				: hasText(text, startIndex, search);
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
	 *
	 * @param text
	 * @param startIndex
	 * @param search
	 * @return
	 * @since 0.11
	 */
	public static boolean hasCaseInsensitiveText(final CharSequence text, final int startIndex, final String search) {
		int index = startIndex;

		if (search.length() > text.length() - startIndex) {
			return false;
		}

		for (int i = 0; i < search.length(); i++) {
			// Case-insensitive compare
			// Based on String.CaseInsensitiveComparator.compare(String, String)
			char c1 = text.charAt(index++);
			char c2 = search.charAt(i);
			if (c1 != c2) {
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
				if (c1 != c2) {
					c1 = Character.toLowerCase(c1);
					c2 = Character.toLowerCase(c2);
					if (c1 != c2) {
						return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * Parses the specified Java text and determines the <code>BEXMatchingTextState</code>s
	 * @param text the Java text
	 * @return an unmodifiable map from the range to the BEXMatchingTextState
	 */
	public static ImmutableIntRangeMap<MatchingStateOption> parseJavaTextStates(final CharSequence text) {
		if (text.length() == 0) {
			return ImmutableIntRangeMap.of();
		}

		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		ImmutableIntRangeMap.Builder<MatchingStateOption> builder = ImmutableIntRangeMap.builder();
		ArrayDeque<MatchingStateOption> stateStack = new ArrayDeque<>();
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

					if (c == '\r' && nextChar(text, i) == '\n') {
						builder.put(IntBEXRange.closed(i, i + 1), LINE_TERMINATOR);
						i++;
					} else {
						builder.put(IntBEXRange.singleton(i), LINE_TERMINATOR);
					}
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
			} else if (c == '\n' || c == '\r') {
				if (c == '\r' && nextChar(text, i) == '\n') {
					builder.put(IntBEXRange.closed(i, i + 1), LINE_TERMINATOR);
					i++;
				} else {
					builder.put(IntBEXRange.singleton(i), LINE_TERMINATOR);
				}
			} else if (Character.isWhitespace(c)) {
				char nextChar = nextChar(text, i);
				if (hasNextChar(text, i) && Character.isWhitespace(nextChar)) {
					// Multiple whitespace
					int start = i;

					do {
						if (nextChar == '\n' || nextChar == '\r') {
							break;
						}

						i++;
						nextChar = nextChar(text, i);
					} while (hasNextChar(text, i) && Character.isWhitespace(nextChar));

					builder.put(IntBEXRange.closed(start, i), WHITESPACE);
				} else {
					// Single whitespace
					builder.put(IntBEXRange.singleton(i), WHITESPACE);
				}
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
	 * Parses the specified JSP text and determines the <code>BEXMatchingTextState</code>s
	 * @param text the JSP text
	 * @return an unmodifiable map from the range to the BEXMatchingTextState
	 */
	// TODO: need to fix and then update BEX
	public static ImmutableIntRangeMap<MatchingStateOption> parseJSPTextStates(final CharSequence text) {
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

		// Reference: https://www.tutorialspoint.com/jsp/jsp_syntax.htm

		ImmutableIntRangeMap.Builder<MatchingStateOption> builder = ImmutableIntRangeMap.builder();
		ArrayDeque<MatchingStateOption> stateStack = new ArrayDeque<>();
		ArrayDeque<Integer> startTextInfoStack = new ArrayDeque<>();

		boolean isJava = false;
		// HTML tag
		boolean isTag = false;
		// TODO: should I refactor and use this? how would I use it?
		//		String expectedEnd = "";

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
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
				} else if (hasText(text, i, "<%=")) {
					pushNextLevelMatchingStateOption(IN_EXPRESSION_BLOCK, i, builder, stateStack, startTextInfoStack);
					i += 2;

					isJava = true;
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
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
				} else if (hasText(text, i, "<%=")) {
					pushNextLevelMatchingStateOption(IN_EXPRESSION_BLOCK, i, builder, stateStack, startTextInfoStack);
					i += 2;
				}

				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal

				// TODO: Java comments only valid in <% code block %>
			} else if (isJava && hasText(text, i, "%>")) {
				isJava = false;

				if (stateStack.peek() != IN_EXPRESSION_BLOCK) {
					// End the current state on the prior character
					popMatchingStateOption(i - 1, builder, stateStack, startTextInfoStack);
				}

				i++;
				popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
			} else if (isJava && stateStack.peek() == IN_LINE_COMMENT) {
				if (c == '\n' || c == '\r') {
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
					//					int startTextInfo = startTextInfoStack.pop();
					//					builder.put(IntBEXRange.of(startTextInfo, i), stateStack.pop());
				}
				// Other characters don't matter?
			} else if (isJava && stateStack.peek() == IN_MULTILINE_COMMENT) {
				if (hasText(text, i, "*/")) {
					i++;
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);//					int startTextInfo = startTextInfoStack.pop();
					//					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
			} else if (stateStack.peek() == IN_MULTILINE_COMMENT) {
				if (hasText(text, i, "--%>")) {
					i += 3;
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
				}
			} else if (stateStack.peek() == IN_SECONDARY_MULTILINE_COMMENT) {
				if (hasText(text, i, "-->")) {
					i += 2;
					popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
				}
			} else if (isJava && c == '/' && nextChar(text, i) == '/') {
				pushNextLevelMatchingStateOption(IN_LINE_COMMENT, i, builder, stateStack, startTextInfoStack);
				//				stateStack.push(IN_LINE_COMMENT);
				//				startTextInfoStack.push(i);
				i++;
			} else if (isJava && c == '/' && nextChar(text, i) == '*') {
				pushNextLevelMatchingStateOption(IN_MULTILINE_COMMENT, i, builder, stateStack, startTextInfoStack);
				//				stateStack.push(IN_MULTILINE_COMMENT);
				//				startTextInfoStack.push(i);
				i++;
			} else if (c == '"' && isTag) {
				pushNextLevelMatchingStateOption(IN_STRING_LITERAL, i, builder, stateStack, startTextInfoStack);
			} else if (c == '\'' && isTag) {
				pushNextLevelMatchingStateOption(IN_SECONDARY_STRING_LITERAL, i, builder, stateStack,
						startTextInfoStack);
			} else if (c == '"' && isJava) {
				stateStack.push(IN_STRING_LITERAL);
				startTextInfoStack.push(i);
			} else if (c == '\'' && isJava) {
				stateStack.push(IN_SECONDARY_STRING_LITERAL);
				startTextInfoStack.push(i);
			} else if (hasText(text, i, "<%--")) {
				stateStack.push(IN_MULTILINE_COMMENT);
				startTextInfoStack.push(i);
				i += 3;
			} else if (hasText(text, i, "<!--")) {
				stateStack.push(IN_SECONDARY_MULTILINE_COMMENT);
				startTextInfoStack.push(i);
				i += 3;
			} else if (hasText(text, i, "<%=")) {
				stateStack.push(IN_EXPRESSION_BLOCK);
				startTextInfoStack.push(i);
				i += 2;
				isJava = true;
			} else if (hasText(text, i, "<%")) {
				// TODO: in Java scriptlet
				stateStack.push(IN_EXPRESSION_BLOCK);
				startTextInfoStack.push(i);
				i++;
				isJava = true;
			} else if (c == '<' && !isJava && !isTag) {
				stateStack.push(IN_TAG);
				startTextInfoStack.push(i);
				isTag = true;
			} else if (c == '>' && isTag) {
				isTag = false;
				popMatchingStateOption(i, builder, stateStack, startTextInfoStack);
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
	 * Parses the specified SQL text and determines the <code>BEXMatchingTextState</code>s
	 * @param text the SQL text
	 * @return an unmodifiable map from the range to the BEXMatchingTextState
	 */
	public static ImmutableIntRangeMap<MatchingStateOption> parseSQLTextStates(final CharSequence text) {
		if (text.length() == 0) {
			return ImmutableIntRangeMap.of();
		}

		// Parse text to get states
		// * Block comment
		// * Line comment
		// * In String literal
		// * Other stuff?

		ImmutableIntRangeMap.Builder<MatchingStateOption> builder = ImmutableIntRangeMap.builder();
		ArrayDeque<MatchingStateOption> stateStack = new ArrayDeque<>();
		ArrayDeque<Integer> startTextInfoStack = new ArrayDeque<>();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (stateStack.peek() == IN_STRING_LITERAL) {
				// TODO: how to implement escaping, since cannot escape single quote with '\'
				if (c == '\'' && nextChar(text, i) == '\'') {
					// 2 single quotes represents 1 single quote in text
					i++;
				} else if (c == '\'') {
					// End of String literal
					int startTextInfo = startTextInfoStack.pop();
					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				}
				//				} else if (c == '\\') {
				//				// Escape next character
				//				if (nextChar(text, i) == '\0') {
				//					break;
				//				}
				//
				//				i++;
				//			}
				// Other characters don't matter??
				// TODO: does SQL allow double quotes and what is their meaning?
				//			} else if (stateStack.peek() == IN_SECONDARY_STRING_LITERAL) {
				//				if (c == '\\') {
				//					// Escape next character
				//					if (nextChar(text, i) == '\0') {
				//						break;
				//					}
				//
				//					i++;
				//				} else if (c == '\'') {
				//					// End of String literal
				//					int startTextInfo = startTextInfoStack.pop();
				//					builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());
				//				}
				//				// Other characters don't matter??
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

					if (!stateStack.isEmpty()) {
						// Inside a first level, so add startTextInfo for after expression blocks ends
						startTextInfoStack.push(i + 1);
					}
				} else if (hasText(text, i, "/*")) {
					// SQL supports nested block comments

					// Going into second level, so end current level
					int startTextInfo = startTextInfoStack.pop();
					if (startTextInfo != i) {
						// Only add if not empty range
						// Would be empty for example if ended one expression then immediately started next one
						builder.put(IntBEXRange.of(startTextInfo, i), stateStack.peek());
					}

					stateStack.push(IN_MULTILINE_COMMENT);
					startTextInfoStack.push(i);
					i++;
				}
			} else if (c == '-' && nextChar(text, i) == '-') {
				stateStack.push(IN_LINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '/' && nextChar(text, i) == '*') {
				stateStack.push(IN_MULTILINE_COMMENT);
				startTextInfoStack.push(i);
				i++;
			} else if (c == '\'') {
				stateStack.push(IN_STRING_LITERAL);
				startTextInfoStack.push(i);
				//			} else if (c == '"') {
				//				stateStack.push(IN_SECONDARY_STRING_LITERAL);
				//				startTextInfoStack.push(i);
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

	private static void pushNextLevelMatchingStateOption(final MatchingStateOption matchingStateOption, final int i,
			final ImmutableIntRangeMap.Builder<MatchingStateOption> builder,
			final ArrayDeque<MatchingStateOption> stateStack,
			final ArrayDeque<Integer> startTextInfoStack) {
		// Going into second level, so end current level
		int startTextInfo = startTextInfoStack.pop();
		if (startTextInfo != i) {
			// Only add if not empty range
			// Would be empty for example if ended one expression then immediately started next one
			builder.put(IntBEXRange.of(startTextInfo, i), stateStack.peek());
		}

		stateStack.push(matchingStateOption);
		startTextInfoStack.push(i);
	}

	private static void popMatchingStateOption(final int i,
			final ImmutableIntRangeMap.Builder<MatchingStateOption> builder,
			final ArrayDeque<MatchingStateOption> stateStack,
			final ArrayDeque<Integer> startTextInfoStack) {
		int startTextInfo = startTextInfoStack.pop();
		builder.put(IntBEXRange.closed(startTextInfo, i), stateStack.pop());

		if (!stateStack.isEmpty()) {
			// Inside a first level, so add startTextInfo for after expression blocks ends
			startTextInfoStack.push(i + 1);
		}
	}
}
