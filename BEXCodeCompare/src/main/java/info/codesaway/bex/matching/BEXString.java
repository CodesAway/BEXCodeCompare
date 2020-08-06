package info.codesaway.bex.matching;

import info.codesaway.bex.ImmutableIntRangeMap;

public final class BEXString implements CharSequence {
	private final String text;

	/**
	 * Map from range to text state
	 */
	private final ImmutableIntRangeMap<BEXMatchingStateOption> textStateMap;

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
	 * @param textStateMap the text state map
	 */
	public BEXString(final String text, final ImmutableIntRangeMap<BEXMatchingStateOption> textStateMap) {
		this(text, textStateMap, 0);
	}

	private BEXString(final String text, final ImmutableIntRangeMap<BEXMatchingStateOption> textStateMap,
			final int offset) {
		this.text = text;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	public String getText() {
		return this.text;
	}

	public ImmutableIntRangeMap<BEXMatchingStateOption> getTextStateMap() {
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

	@Override
	public String toString() {
		return this.getText();
	}
}
