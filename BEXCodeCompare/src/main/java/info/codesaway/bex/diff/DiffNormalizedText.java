package info.codesaway.bex.diff;

import java.util.Objects;

public final class DiffNormalizedText {
	private final String leftText;
	private final String rightText;

	public DiffNormalizedText(final String leftText, final String rightText) {
		this.leftText = leftText;
		this.rightText = rightText;
	}

	public String getLeftText() {
		return this.leftText;
	}

	public String getRightText() {
		return this.rightText;
	}

	/**
	 * Indicates if the two text values in this normalized text are equal
	 *
	 * @return
	 */
	public boolean hasEqualText() {
		return Objects.equals(this.leftText, this.rightText);
	}
}
