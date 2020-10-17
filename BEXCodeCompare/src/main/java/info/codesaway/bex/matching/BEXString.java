package info.codesaway.bex.matching;

import info.codesaway.bex.ImmutableIntRangeMap;
import info.codesaway.bex.IntPair;
import info.codesaway.bex.IntRange;

public final class BEXString implements CharSequence {
	private final String text;

	private final MatchingLanguage language;

	/**
	 * Map from range to text state
	 */
	private final ImmutableIntRangeMap<MatchingStateOption> textStateMap;

	/**
	 * The offset, so can resolve indexes in text to indexes in textStateMap (such as if use BEXString.substring)
	 */
	private final int offset;

	/**
	 * Creates a BEXString from the specified text using the {@link BEXMatchingUtilities#parseJavaTextStates(CharSequence)}
	 * @param text the Java source code
	 */
	public BEXString(final String text) {
		this(text, BEXMatchingLanguage.JAVA);
	}

	public BEXString(final String text, final MatchingLanguage language) {
		this(text, language, language.parse(text), 0);
	}

	private BEXString(final String text, final MatchingLanguage language,
			final ImmutableIntRangeMap<MatchingStateOption> textStateMap,
			final int offset) {
		this.text = text;
		this.language = language;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	public String getText() {
		return this.text;
	}

	public MatchingLanguage getLanguage() {
		return this.language;
	}

	public ImmutableIntRangeMap<MatchingStateOption> getTextStateMap() {
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

	/**
	 *
	 * @param startEnd the start (left value) and end (right value)
	 * @return
	 * @since 0.10
	 */
	public BEXString substring(final IntPair startEnd) {
		return this.substring(startEnd.getLeft(), startEnd.getRight());
	}

	/**
	 *
	 * @param range
	 * @return
	 * @since 0.11
	 */
	public BEXString substring(final IntRange range) {
		// Logic from IntRange.canonical
		int start = range.hasInclusiveStart() ? range.getStart() : range.getStart() + 1;
		int end = range.hasInclusiveEnd() ? range.getEnd() + 1 : range.getEnd();

		return this.substring(start, end);
	}

	public BEXString substring(final int start, final int end) {
		return new BEXString(this.text.substring(start, end), this.language, this.textStateMap, start + this.offset);
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
