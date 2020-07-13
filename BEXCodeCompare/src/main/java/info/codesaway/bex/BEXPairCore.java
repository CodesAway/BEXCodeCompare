package info.codesaway.bex;

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
 * @param <T> the type of pair
 */
public interface BEXPairCore<T> {
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
		return side == BEXSide.LEFT ? this.getLeft() : this.getRight();
	}

	/**
	 * Map each value in this BEXPairCore using the specified function
	 *
	 * @param function the mapping function
	 * @return a BEXPair consisting of this BEXPairCore's values mapped using the specified function
	 * @since 0.3
	 */
	public default <R> BEXPair<R> map(final Function<T, R> function) {
		return new BEXPair<>(function.apply(this.getLeft()), function.apply(this.getRight()));
	}

	/**
	 *
	 * @param function the mapping function
	 * @return a BEXPair consisting of this BEXPairCore's values mapped using the specified function
	 * @throws X the Throwable type
	 */
	public default <R, X extends Throwable> BEXPair<R> mapThrows(final FunctionThrows<T, R, X> function) throws X {
		return new BEXPair<>(function.apply(this.getLeft()), function.apply(this.getRight()));
	}

	/**
	 * Map each value in this BEXPairCore using the specified int function
	 *
	 * @param function the mapping function
	 * @return an IntBEXPair consisting of this BEXPairCore's values mapped to an int using the specified function
	 * @since 0.3
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
	 * @since 0.3
	 */
	public default int applyAsInt(final ToIntBiFunction<T, T> function) {
		return function.applyAsInt(this.getLeft(), this.getRight());
	}

	/**
	 * Evaluates the specified BiPredicate, passing {@link #getLeft()} and {@link #getRight()} as arguments
	
	 * @param predicate the BiPredicate to apply
	 * @return <code>true</code> if the predicate matches when applying the arugments; otherwise, <code>false</code>
	 * @since 0.3
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
	 * @since 0.3
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
	 * @since 0.3
	 */
	public default boolean testOrBoth(final Predicate<T> predicate) {
		return predicate.test(this.getLeft()) || predicate.test(this.getRight());
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
		consumer.accept(this.getLeft(), BEXSide.LEFT);
		consumer.accept(this.getRight(), BEXSide.RIGHT);
	}

	/**
	* <p>Formats this BEXPairCore using the given format.</p>
	*
	* <p>This method uses {@link String#format} to perform the formatting, passing the {@link #getLeft()} and {@link #getRight()} values as parameters.</p>
	*
	* @param format the format string
	* @return the formatted string
	* @since 0.3
	*/
	public default String toString(final String format) {
		return String.format(format, this.getLeft(), this.getRight());
	}
}
