package info.codesaway.bex;

/**
 * Immutable pair of left / right int values
 */
public final class IntBEXPair implements IntPair {
	private final int left;
	private final int right;

	public static final IntBEXPair ZERO = new IntBEXPair(0, 0);

	private IntBEXPair(final int left, final int right) {
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

	public static IntBEXPair of(final int left, final int right) {
		return new IntBEXPair(left, right);
	}

	@Override
	public String toString() {
		return "(" + this.getLeft() + "," + this.getRight() + ")";
	}
}
