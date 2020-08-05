package info.codesaway.bex.matching;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class BEXString implements CharSequence {
	private final String text;

	/**
	 * Map from range start to BEXMatchingTextState (contains range and text state)
	 */
	private final NavigableMap<Integer, BEXMatchingTextState> textStateMap;

	/**
	 * The offset, so can resolve indexes in text to indexes in textStateMap (such as if use BEXString.substring)
	 */
	private final int offset;

	/**
	 * Creates a BEXString from the specified text using the {@link BEXMatchingUtilities#extractJavaTextStates(CharSequence)}
	 * @param text the Java source code
	 */
	public BEXString(final String text) {
		this(text, BEXMatchingLanguage.JAVA);
	}

	public BEXString(final String text, final BEXMatchingLanguage language) {
		this(text, language.extract(text), 0);
	}

	/**
	 * Creates a BEXString from the specified text and text state map
	 * @param text the text
	 * @param textStateMap the text state map (key is start index for range)
	 * @param isUnmodifiable indicates whether the textStateMap is unmodifiable; if <code>false</code> a copy will be created and made unmodifiable
	 */
	public BEXString(final String text, final NavigableMap<Integer, BEXMatchingTextState> textStateMap,
			final boolean isUnmodifiable) {
		this(text,
				isUnmodifiable
						? textStateMap
						: Collections.unmodifiableNavigableMap(new TreeMap<>(textStateMap)),
				0);
	}

	private BEXString(final String text, final NavigableMap<Integer, BEXMatchingTextState> textStateMap,
			final int offset) {
		this.text = text;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	public String getText() {
		return this.text;
	}

	public NavigableMap<Integer, BEXMatchingTextState> getTextStateMap() {
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

	public BEXString substring(final int start, final int end) {
		return new BEXString(this.text.substring(start, end), this.textStateMap, start);
	}

	@Override
	public BEXString subSequence(final int start, final int end) {
		return this.substring(start, end);
	}
}
