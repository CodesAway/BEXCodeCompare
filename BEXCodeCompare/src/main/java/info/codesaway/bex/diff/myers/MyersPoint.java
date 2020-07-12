package info.codesaway.bex.diff.myers;

public final class MyersPoint {
	private final int x;
	private final int y;

	public MyersPoint(final int x, final int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	@Override
	public String toString() {
		return "(" + this.getX() + ", " + this.getY() + ")";
	}
}
