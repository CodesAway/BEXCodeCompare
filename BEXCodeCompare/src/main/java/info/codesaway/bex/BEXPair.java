package info.codesaway.bex;

import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import info.codesaway.bex.util.FunctionThrows;

/**
 * Pair of left and right values, which are of the same type
 *
 * <p>Instances of this class are immutable if generic class type is also immutable</p>
 *
 * <p>Instances of this class are Comparable if generic class type is also Comparable</p>
 * @param <T> the type of pair
 * @since 0.3
 */
public interface BEXPair<T> extends Comparable<BEXPair<T>> {
	/**
	 * Gets the left value in the pair
	 * @return the left value
	 */
	public T getLeft();

	/**
	 * Gets the right value in the pair
	 * @return the right value
	 */
	public T getRight();

	/**
	 * Gets the value on the specified side
	 * @param side the side (either {@link BEXSide#LEFT} or {@link BEXSide#RIGHT})
	 * @return the value on the specified side
	 */
	public default T get(final BEXSide side) {
		return side == LEFT ? this.getLeft() : this.getRight();
	}

	/**
	 * Map each value in this BEXPair using the specified function
	 *
	 * @param function the mapping function
	 * @return a BEXPair consisting of this BEXPair's values mapped using the specified function
	 */
	public default <R> BEXPair<R> map(final Function<T, R> function) {
		return new BEXPairValue<>(function.apply(this.getLeft()), function.apply(this.getRight()));
	}

	/**
	 * Map each value in this BEXPair using the specified BiFunction, passing the side as the second argument
	 *
	 * @param function the mapping BiFunction
	 * @return a BEXPair consisting of this BEXPair's values mapped using the specified function
	 * @since 0.4
	 */
	public default <R> BEXPair<R> mapWithSide(final BiFunction<T, BEXSide, R> function) {
		return new BEXPairValue<>(function.apply(this.getLeft(), LEFT), function.apply(this.getRight(), RIGHT));
	}

	/**
	 *
	 * @param function the mapping function
	 * @return a BEXPair consisting of this BEXPair's values mapped using the specified function
	 * @throws X the Throwable type
	 */
	public default <R, X extends Throwable> BEXPair<R> mapThrows(final FunctionThrows<T, R, X> function) throws X {
		return new BEXPairValue<>(function.apply(this.getLeft()), function.apply(this.getRight()));
	}

	/**
	 * Returns a BEXPair with left and right values swapped
	 *
	 * <pre>new BEXPair&lt;&gt;({@link #getRight()}, {@link #getLeft()})</pre>
	 * @return BEXPair with left and right values swapped
	 * @since 0.4
	 */
	public default BEXPair<T> mirror() {
		return new BEXPairValue<>(this.getRight(), this.getLeft());
	}

	/**
	 * Map each value in this BEXPair using the specified int function
	 *
	 * @param function the mapping function
	 * @return an IntBEXPair consisting of this BEXPair's values mapped to an int using the specified function
	 */
	public default IntBEXPair mapToInt(final ToIntFunction<T> function) {
		return IntBEXPair.of(function.applyAsInt(this.getLeft()), function.applyAsInt(this.getRight()));
	}

	/**
	 * Applies the specified BiFunction, passing {@link #getLeft()} and {@link #getRight()} as arguments
	
	 * @param function the BiFunction to apply
	 * @return the result of applying the BiFunction
	 */
	public default <R> R apply(final BiFunction<T, T, R> function) {
		return function.apply(this.getLeft(), this.getRight());
	}

	/**
	 * Applies the specified ToIntBiFunction, passing {@link #getLeft()} and {@link #getRight()} as arguments
	
	 * @param function the ToIntBiFunction to apply
	 * @return the result of applying the ToIntBiFunction
	 */
	public default int applyAsInt(final ToIntBiFunction<T, T> function) {
		return function.applyAsInt(this.getLeft(), this.getRight());
	}

	/**
	 * Evaluates the specified BiPredicate, passing {@link #getLeft()} and {@link #getRight()} as arguments
	
	 * @param predicate the BiPredicate to apply
	 * @return <code>true</code> if the predicate matches when applying the arugments; otherwise, <code>false</code>
	 */
	public default boolean test(final BiPredicate<T, T> predicate) {
		return predicate.test(this.getLeft(), this.getRight());
	}

	/**
	 * Evaluates the specified Predicate against both {@link #getLeft()} and {@link #getRight()} and <b>AND</b>s the result
	 *
	 * <p>This is logically the same as</p>
	 * <pre>predicate.test(this.getLeft()) &amp;&amp; predicate.test(this.getRight())</pre>
	 *
	 * @param predicate the Predicate to apply
	 * @return <code>true</code> if the predicate matches {@link #getLeft()} <b>AND</b> {@link #getRight()}
	 */
	public default boolean testAndBoth(final Predicate<T> predicate) {
		return predicate.test(this.getLeft()) && predicate.test(this.getRight());
	}

	/**
	 * Evaluates the specified Predicate against both {@link #getLeft()} and {@link #getRight()} and <b>OR</b>s the result
	 *
	 * <p>This is logically the same as</p>
	 * <pre>predicate.test(this.getLeft()) || predicate.test(this.getRight())</pre>
	 *
	 * @param predicate the Predicate to test
	 * @return <code>true</code> if the predicate matches {@link #getLeft()} <b>OR</b> {@link #getRight()}
	 */
	public default boolean testOrBoth(final Predicate<T> predicate) {
		return predicate.test(this.getLeft()) || predicate.test(this.getRight());
	}

	/**
	 * Evaluates the specified Predicate against {@link #getLeft()} then {@link #getRight()}
	 * to determine which side, if any, first satisfies the Predicate
	 *
	 * @param predicate the Predicate to test
	 * @return the side which first satisfier the Predicate, or <code>null</code> if neither side will satisfy the predicate
	 * @since 0.4
	 */
	public default BEXSide testLeftMirror(final Predicate<T> predicate) {
		if (predicate.test(this.getLeft())) {
			return LEFT;
		} else if (predicate.test(this.getRight())) {
			return RIGHT;
		} else {
			return null;
		}
	}

	/**
	 * Evaluates the specified Predicate against {@link #getRight()} then {@link #getLeft()}
	 * to determine which side, if any, first satisfies the Predicate
	 *
	 * @param predicate the Predicate to test
	 * @return the side which first satisfier the Predicate, or <code>null</code> if neither side will satisfy the predicate
	 * @since 0.4
	 */
	public default BEXSide testRightMirror(final Predicate<T> predicate) {
		if (predicate.test(this.getRight())) {
			return RIGHT;
		} else if (predicate.test(this.getLeft())) {
			return LEFT;
		} else {
			return null;
		}
	}

	/**
	 * Evaluates the specified BiPredicate against (left, right) then (right, left)
	 * to determine which side, if any, first satisfies the Predicate
	 *
	 * @param predicate the BiPredicate to test
	 * @return {@link BEXSide#LEFT} if (left, right) satisfies the BiPredicate,
	 * 		{@link BEXSide#RIGHT} if (right, left) satisfies the BiPredicate,
	 * 		or <code>null</code> if neither satisfy the BiPredicate
	 * @since 0.4
	 */
	public default BEXSide testLeftRightMirror(final BiPredicate<T, T> predicate) {
		if (predicate.test(this.getLeft(), this.getRight())) {
			return LEFT;
		} else if (predicate.test(this.getRight(), this.getLeft())) {
			return RIGHT;
		} else {
			return null;
		}
	}

	/**
	 * Evaluates the specified BiPredicate against (right, left) then (left, right)
	 * to determine which side, if any, first satisfies the Predicate
	 *
	 * @param predicate the BiPredicate to test
	 * @return {@link BEXSide#RIGHT} if (right, left) satisfies the BiPredicate,
	 * 		{@link BEXSide#LEFT} if (left, right) satisfies the BiPredicate,
	 * 		or <code>null</code> if neither satisfy the BiPredicate
	 * @since 0.4
	 */
	public default BEXSide testRightLeftMirror(final BiPredicate<T, T> predicate) {
		if (predicate.test(this.getRight(), this.getLeft())) {
			return RIGHT;
		} else if (predicate.test(this.getLeft(), this.getRight())) {
			return LEFT;
		} else {
			return null;
		}
	}

	/**
	 * Performs the specified operation on both {@link #getLeft()} and {@link #getRight()}
	 * @param consumer the Consumer to accept
	 */
	public default void acceptBoth(final Consumer<T> consumer) {
		consumer.accept(this.getLeft());
		consumer.accept(this.getRight());
	}

	/**
	 * Performs the specified operation on both {@link #getLeft()} and {@link #getRight()}, passing the side as the second argument
	 * @param consumer the BiConsumer to accept
	 */
	public default void acceptWithSide(final BiConsumer<T, BEXSide> consumer) {
		consumer.accept(this.getLeft(), LEFT);
		consumer.accept(this.getRight(), RIGHT);
	}

	/**
	* <p>Formats this BEXPair using the given format.</p>
	*
	* <p>This method uses {@link String#format} to perform the formatting, passing the {@link #getLeft()} and {@link #getRight()} values as parameters.</p>
	*
	* @param format the format string
	* @return the formatted string
	*/
	public default String toString(final String format) {
		return String.format(format, this.getLeft(), this.getRight());
	}

	/**
	 * Compares the BEXPair first by the left element then the right element
	 * @throws ClassCastException if the entries do not implement Comparable or are not mutually Comparable
	 * @since 0.10
	 */
	@Override
	public default int compareTo(final BEXPair<T> o) {
		@SuppressWarnings("unchecked")
		Comparable<? super T> left1 = (Comparable<? super T>) this.getLeft();

		@SuppressWarnings("unchecked")
		Comparable<? super T> right1 = (Comparable<? super T>) this.getRight();

		int compare = left1.compareTo(o.getLeft());
		if (compare != 0) {
			return compare;
		}

		return right1.compareTo(o.getRight());
	}

	/**
	 * Indicates if the left value equals the right value
	 * @return <code>true</code> if the left value equals the right value
	 * @since 0.13
	 */
	public default boolean hasEqualValues() {
		return Objects.equals(this.getLeft(), this.getRight());
	}
}
