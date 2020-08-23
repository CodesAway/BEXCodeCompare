package info.codesaway.bex;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Pair of left and right values, which are of the same type
 *
 * <p>Instances of this class are immutable if generic class type is also immutable</p>
 * @param <T> the type of pair
 */
// TODO: think of better name?
public final class BEXPairValue<T> implements BEXPair<T> {
	private final T left;
	private final T right;

	/**
	 * Creates a new BEXPair using the specified values
	 * @param left the left value
	 * @param right the right value
	 */
	public BEXPairValue(final T left, final T right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Creates a new BEXPair using the specified supplier (same supplier used for left and right values)
	 * @param supplier the supplier
	 * @since 0.3
	 */
	public BEXPairValue(final Supplier<T> supplier) {
		this(supplier.get(), supplier.get());
	}

	/**
	 * Creates a new BEXPair using the results of applying the specified function for both {@link BEXSide#LEFT} and {@link BEXSide#RIGHT}.
	 *
	 * @param <T> the generic type
	 * @param function the function to apply
	 * @return the BEXPairValue
	 * @since 0.4
	 */
	// Changed from constructor to static method due to ambiguity between this and constructor accepting a Supplier
	public static <T> BEXPairValue<T> from(final Function<BEXSide, T> function) {
		return new BEXPairValue<>(function.apply(BEXSide.LEFT), function.apply(BEXSide.RIGHT));
	}

	/**
	 * Creates a new BEXPair with both values set to <code>both</code>
	 * @param <T> the type of value
	 * @param both the value used for both sides
	 * @return new BEXPair with both values set to <code>both</code>
	 */
	public static <T> BEXPairValue<T> of(final T both) {
		return new BEXPairValue<>(both, both);
	}

	/**
	 * Gets the left value in the pair
	 * @return the left value
	 */
	@Override
	public T getLeft() {
		return this.left;
	}

	/**
	 * Gets the right value in the pair
	 * @return the right value
	 */
	@Override
	public T getRight() {
		return this.right;
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
		BEXPairValue other = (BEXPairValue) obj;
		return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
	}

	/**
	* <p>Returns a String representation of this BEXPair using the format {@code ($left,$right)}.</p>
	*
	* @return a string representation of the object
	* @since 0.3
	*/
	@Override
	public String toString() {
		return "(" + this.getLeft() + ',' + this.getRight() + ')';
	}
}
