package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRMatchingUtilities.hasText;
import static info.codesaway.becr.matching.BECRMatchingUtilities.nextChar;
import static info.codesaway.becr.matching.BECRStateOption.IN_LINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_STRING_LITERAL;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import info.codesaway.becr.BECRRange;

public final class BECRString implements CharSequence {
	private final String text;

	/**
	 * Map from range start to BECRTextState (contains range and text state)
	 */
	private final NavigableMap<Integer, BECRTextState> textStateMap;

	/**
	 * The offset, so can resolve indexes in text to indexes in textStateMap (such as if use BECRString.substring)
	 */
	private final int offset;

	/**
	 * Creates a BECRString from the specified text using the {@link #extractJavaTextStates(CharSequence)}
	 * @param text the Java source code
	 */
	public BECRString(final String text) {
		this(text, extractJavaTextStates(text), 0);
	}

	/**
	 * Creates a BECRString from the specified text and text state map
	 * @param text the text
	 * @param textStateMap the text state map (key is start index for range)
	 * @param isUnmodifiable indicates whether the textStateMap is unmodifiable; if <code>false</code> a copy will be created and made unmodifiable
	 */
	public BECRString(final String text, final NavigableMap<Integer, BECRTextState> textStateMap,
			final boolean isUnmodifiable) {
		this(text,
				isUnmodifiable
						? textStateMap
						: Collections.unmodifiableNavigableMap(new TreeMap<>(textStateMap)),
				0);
	}

	private BECRString(final String text, final NavigableMap<Integer, BECRTextState> textStateMap, final int offset) {
		this.text = text;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	public String getText() {
		return this.text;
	}

	public NavigableMap<Integer, BECRTextState> getTextStateMap() {
		return this.textStateMap;
	}

	public int getOffset() {
		return this.offset;
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

	@Override
	public int length() {
		return this.text.length();
	}

	@Override
	public char charAt(final int index) {
		return this.text.charAt(index);
	}

	public BECRString substring(final int start, final int end) {
		return new BECRString(this.text.substring(start, end), this.textStateMap, start);
	}

	@Override
	public BECRString subSequence(final int start, final int end) {
		return this.substring(start, end);
	}
}
