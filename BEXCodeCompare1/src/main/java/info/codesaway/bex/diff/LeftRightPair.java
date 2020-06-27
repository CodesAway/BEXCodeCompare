package info.codesaway.bex.diff;

import java.util.Objects;

/**
 * Pair of left and right values, which
 *
 * <p>Instances of this class are immutable if generic class type is also immutable</p>
 */
public final class LeftRightPair<T> {
	private final T left;
	private final T right;

	public LeftRightPair(final T left, final T right) {
		this.left = left;
		this.right = right;
	}

	public T getLeft() {
		return this.left;
	}

	public T getRight() {
		return this.right;
	}

	public T get(final DiffSide diffSide) {
		return diffSide == DiffSide.LEFT ? this.getLeft() : this.getRight();
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
		@SuppressWarnings("rawtypes")
		LeftRightPair other = (LeftRightPair) obj;
		return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
	}
}
