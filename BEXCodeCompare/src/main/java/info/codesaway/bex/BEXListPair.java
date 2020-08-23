package info.codesaway.bex;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * BEXPair containing a left and right list
 *
 * <p>This class has some helper methods to operate on both sides of the list together</p>
 *
 * @param <E> the type of elements in the lists
 * @since 0.3
 */
public final class BEXListPair<E> implements BEXPair<List<E>> {
	private final BEXPair<List<E>> pair;

	/**
	 * Creates a new BEXListPair using the results of applying the specified function for both {@link BEXSide#LEFT} and {@link BEXSide#RIGHT}
	 * @param supplier the supplier used to initialize both the left and right lists (typically a constructor reference such as <code>ArrayList::new</code>)
	 */
	public BEXListPair(final Supplier<? extends List<E>> supplier) {
		this(supplier.get(), supplier.get());
	}

	/**
	 * Creates a new BEXListPair using the specified left and right list
	 * @param left the left list
	 * @param right the right list
	 */
	public BEXListPair(final List<E> left, final List<E> right) {
		this(new BEXPairValue<>(left, right));
	}

	/**
	 * Creates a new BEXListPair using the specified pair of lists
	 * @param listPair the pair of lists
	 */
	public BEXListPair(final BEXPair<List<E>> listPair) {
		this.pair = listPair;
	}

	/**
	 * Creates a new BEXListPair using the results of applying the specified function for both {@link BEXSide#LEFT} and {@link BEXSide#RIGHT}
	 * @param <E> the element type
	 * @param function the function to apply
	 * @return BEXListPair
	 */
	// Implementation note: made "from" method instead of constructor due to Java compiler ambiguity
	// (doesn't show as compile error until consumer code tries to use it, then shows as compile error)
	public static <E> BEXListPair<E> from(final Function<BEXSide, List<E>> function) {
		return new BEXListPair<>(function.apply(BEXSide.LEFT), function.apply(BEXSide.RIGHT));
	}

	/**
	 * Gets the left list
	 * @return the left list
	 */
	@Override
	public List<E> getLeft() {
		return this.pair.getLeft();
	}

	/**
	 * Gets the right list
	 * @return the right list
	 */
	@Override
	public List<E> getRight() {
		return this.pair.getRight();
	}

	/**
	 * Gets the list on the specified side
	 * @param side the side
	 * @return the list on the specified side
	 */
	@Override
	public List<E> get(final BEXSide side) {
		return this.pair.get(side);
	}

	/**
	 * Adds the left value to the left list and the right value to the right list
	 * @param pair the pair of values to add
	 */
	public void add(final BEXPair<E> pair) {
		this.getLeft().add(pair.getLeft());
		this.getRight().add(pair.getRight());
	}

	/**
	 *
	 * @return <code>true</code> if the left list is empty
	 */
	public boolean isLeftEmpty() {
		return this.getLeft().isEmpty();
	}

	/**
	 *
	 * @return <code>true</code> if the right list is empty
	 */
	public boolean isRightEmpty() {
		return this.getRight().isEmpty();
	}

	/**
	 *
	 * @return the size of the left list
	 */
	public int leftSize() {
		return this.getLeft().size();
	}

	/**
	 *
	 * @return the size of the right list
	 */
	public int rightSize() {
		return this.getRight().size();
	}

	/**
	 *
	 * @param index the index in the list
	 * @return a pair of values getting the specified value from each the left and right list
	 * @throws IndexOutOfBoundsException if the index is out of range for either the left or right list
	 */
	public BEXPair<E> get(final int index) {
		return new BEXPairValue<>(this.getLeft().get(index), this.getRight().get(index));
	}

	/**
	 *
	 * @param index the index in the list
	 * @return a pair of values using the left / right index in the pair to get the corresponding element in each list
	 * @throws IndexOutOfBoundsException if the index is out of range for either the left or right list
	 */
	public BEXPair<E> get(final IntPair index) {
		return new BEXPairValue<>(this.getLeft().get(index.getLeft()), this.getRight().get(index.getRight()));
	}

	/**
	 * Map each value in this BEXPair using the specified function
	 *
	 * @param function the mapping function
	 * @return a BEXPair consisting of this BEXPair's values mapped using the specified function
	 * @since 0.3
	 */
	@Override
	public <R> BEXPair<R> map(final Function<List<E>, R> function) {
		return new BEXPairValue<>(function.apply(this.getLeft()), function.apply(this.getRight()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pair);
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
		BEXListPair<?> other = (BEXListPair<?>) obj;
		return Objects.equals(this.pair, other.pair);
	}
}
