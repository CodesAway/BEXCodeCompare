package info.codesaway.bex;

import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(this.left, this.right);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		IntBEXPair other = (IntBEXPair) obj;
		return this.left == other.left && this.right == other.right;
	}
}
