package info.codesaway.bex.diff;

/**
 * Immutable pair of left / right int values
 */
public final class IntLeftRightPair implements IntPair {
	private final int left;
	private final int right;

	public static final IntLeftRightPair ZERO = new IntLeftRightPair(0, 0);

	private IntLeftRightPair(final int left, final int right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int getLeft() {
		return this.left;
	}

	@Override
	public int getRight() {
		return this.right;
	}

	public static IntLeftRightPair of(final int left, final int right) {
		return new IntLeftRightPair(left, right);
	}

	@Override
	public String toString() {
		return "(" + this.getLeft() + ", " + this.getRight() + ")";
	}
}
