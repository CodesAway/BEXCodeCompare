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

	/**
	* <p>Returns a String representation of this DiffNormalizedText</p>
	*
	* @return a string representation of the object
	* @since 0.14
	*/
	@Override
	public String toString() {
		return "(" + this.getLeft() + ',' + this.getRight() + ')';
	}

	/**
	 * @since 0.14
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.left, this.right);
	}

	/**
	 * @since 0.14
	 */
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
		DiffNormalizedText other = (DiffNormalizedText) obj;
		return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
	}
}
