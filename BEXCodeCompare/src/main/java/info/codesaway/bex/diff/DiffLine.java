package info.codesaway.bex.diff;

public final class DiffLine {
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

	// Intentionally don't create equals method
	// For example, if two lines have the same line number and text, they could still be different
	// Such as one is the left line and the other is the right line
	// Therefore, each DiffLine should be seen as unique
}
