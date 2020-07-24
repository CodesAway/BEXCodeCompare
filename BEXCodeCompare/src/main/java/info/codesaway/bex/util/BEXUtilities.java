package info.codesaway.bex.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class BEXUtilities {
	private BEXUtilities() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Indicates whether the specified <code>key</code> exists in the passed <code>elements</code>.
	 *
	 * <p>
	 * This method is null safe. If <code>key</code> is <code>null</code> and one of the entries in
	 * <code>elements</code> is <code>null</code>, this method returns <code>true</code>.
	 * </p>
	 *
	 * @param key
	 *        the key to check
	 * @param elements
	 *        entries to check for the key's existence
	 * @return <code>true</code> if the specified <code>key</code> exists in the passed <code>elements</code>
	 * @throws NullPointerException
	 *         if <code>elements</code> is <code>null</code>
	 */
	public static boolean in(final Object key, final Object... elements) {
		return Arrays.stream(elements).anyMatch(e -> Objects.equals(e, key));
	}

	public static <T> List<T> immutableCopyOf(final List<T> list) {
		return Collections.unmodifiableList(new ArrayList<>(list));
	}

	public static <T> T firstNonNull(final T first, final T second) {
		return first != null ? first : second;
	}

	public static void checkArgument(final boolean condition, final String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Returns a predicate that is the negation of the
	 * supplied predicate.
	 * @param <T>     the type of arguments to the
	 *                specified predicate
	 * @param target  predicate to negate
	 *
	 * @return a predicate that negates the results
	 *         of the supplied predicate
	 *
	 * @since 0.3
	 */
	// Added Java 11 "not" helper method
	// https://bugs.openjdk.java.net/browse/JDK-8203428
	public static <T> Predicate<T> not(final Predicate<? super T> target) {
		return (Predicate<T>) target.negate();
	}

	/**
	 * Checks if the contents of the <code>CharSequence</code>s are equal
	 *
	 * @param s1 the first CharSequence
	 * @param s2 the second CharSequence
	 * @return <code>true</code> if the contents of the <code>CharSequence</code>s are equal
	 * @since 0.4
	 */
	public static boolean contentEquals(final CharSequence s1, final CharSequence s2) {
		// Check length as fast check
		if (s1.length() != s2.length()) {
			return false;
		}

		// Compare contents
		for (int i = 0; i < s2.length(); i++) {
			if (s1.charAt(i) != s2.charAt(i)) {
				return false;
			}
		}

		return true;
	}
}
