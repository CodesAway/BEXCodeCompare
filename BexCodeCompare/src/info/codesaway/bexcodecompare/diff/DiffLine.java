package info.codesaway.bexcodecompare.diff;

public class DiffLine {
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
}
