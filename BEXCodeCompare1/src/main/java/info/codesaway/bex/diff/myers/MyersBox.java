package info.codesaway.bex.diff.myers;

public class MyersBox {
	// Box class from https://blog.jcoglan.com/2017/04/25/myers-diff-in-linear-space-implementation/
	private final MyersPoint topLeft;
	private final MyersPoint bottomRight;

	public MyersBox(final MyersPoint topLeft, final MyersPoint bottomRight) {
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public MyersPoint getTopLeft() {
		return this.topLeft;
	}

	public MyersPoint getBottomRight() {
		return this.bottomRight;
	}

	public int getLeft() {
		return this.getTopLeft().getX();
	}

	public int getTop() {
		return this.getTopLeft().getY();
	}

	public int getRight() {
		return this.getBottomRight().getX();
	}

	public int getBottom() {
		return this.getBottomRight().getY();
	}

	public int getWidth() {
		return this.getRight() - this.getLeft();
	}

	public int getHeight() {
		return this.getBottom() - this.getTop();
	}

	public int size() {
		return this.getWidth() + this.getHeight();
	}

	public int delta() {
		return this.getWidth() - this.getHeight();
	}
}
