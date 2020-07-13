package info.codesaway.bex.util;

/**
 * Function which throws a Throwable
 * @since 0.3
 */
@FunctionalInterface
public interface FunctionThrows<T, R, X extends Throwable> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 * @throws X the Throwable type to throw
	 */
	R apply(T t) throws X;
}
