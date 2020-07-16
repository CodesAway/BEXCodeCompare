package info.codesaway.bex.diff;

import java.util.Objects;

public final class DiffNormalizedText {
	private final String left;
	private final String right;

	public DiffNormalizedText(final String left, final String right) {
		this.left = left;
		this.right = right;
	}

	public String getLeft() {
		return this.left;
	}

	public String getRight() {
		return this.right;
	}

	/**
	 * Indicates if the two text values in this normalized text are equal
	 *
	 * @return
	 */
	public boolean hasEqualText() {
		return Objects.equals(this.getLeft(), this.getRight());
	}
}
