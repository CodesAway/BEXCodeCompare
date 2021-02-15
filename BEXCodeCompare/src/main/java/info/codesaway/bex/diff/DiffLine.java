package info.codesaway.bex.diff;

import info.codesaway.bex.Indexed;

public final class DiffLine implements Indexed<String> {
	private final int number;
	private final String text;

	public DiffLine(final int number, final String text) {
		this.number = number;
		this.text = text;
	}

	public int getNumber() {
		return this.number;
	}

	public String getText() {
		return this.text;
	}

	@Override
	public String toString() {
		return this.getNumber() + ": " + this.getText();
	}

	/**
	 * @since 0.14
	 */
	@Override
	public int getIndex() {
		return this.getNumber();
	}

	/**
	 * @since 0.14
	 */
	@Override
	public String getValue() {
		return this.getText();
	}

	// Intentionally don't create equals method
	// For example, if two lines have the same line number and text, they could still be different
	// Such as one is the left line and the other is the right line
	// Therefore, each DiffLine should be seen as unique
}
