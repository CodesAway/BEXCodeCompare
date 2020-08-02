package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRMatchingUtilities.extractJavaTextStates;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

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
