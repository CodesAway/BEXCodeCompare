package info.codesaway.bex.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import info.codesaway.bex.IntPair;

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

	/**
	 * Indicates if the specified <code>number</code> is between the specified <code>start</code> and <code>end</code> (inclusive)
	 *
	 * @param number
	 * @param start
	 * @param end
	 * @return
	 * @since 0.5
	 */
	public static boolean isBetween(final int number, final int start, final int end) {
		return number >= start && number <= end;
	}

	public static String getSubstring(final CharSequence text, final IntPair startEnd) {
		return getSubSequence(text, startEnd).toString();
	}

	public static CharSequence getSubSequence(final CharSequence text, final IntPair startEnd) {
		return text.subSequence(startEnd.getLeft(), startEnd.getRight());
	}

	/**
	* Returns an immutable {@link Entry} containing the given key and value.
	* The {@code Entry} instances created by this method have the following characteristics:
	*
	* <ul>
	* <li>They disallow {@code null} keys and values. Attempts to create them using a {@code null}
	* key or value result in {@code NullPointerException}.
	* <li>They are immutable. Calls to {@link Entry#setValue Entry.setValue()}
	* on a returned {@code Entry} result in {@code UnsupportedOperationException}.
	* <li>They are not serializable.
	* <li>They are <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/doc-files/ValueBased.html">value-based</a>.
	* Callers should make no assumptions about the identity of the returned instances.
	* This method is free to create new instances or reuse existing ones. Therefore,
	* identity-sensitive operations on these instances (reference equality ({@code ==}),
	* identity hash code, and synchronization) are unreliable and should be avoided.
	* </ul>
	*
	* @param <K> the key's type
	* @param <V> the value's type
	* @param k the key
	* @param v the value
	* @return an {@code Entry} containing the specified key and value
	* @throws NullPointerException if the key or value is {@code null}
	*
	* @since 0.9
	*/
	// Similar to Java 9 Map.entry helper method
	public static <K, V> Entry<K, V> entry(final K k, final V v) {
		Objects.requireNonNull(k);
		Objects.requireNonNull(v);
		return new AbstractMap.SimpleImmutableEntry<>(k, v);
	}

	//	public static <T extends IntRange> Optional<Entry<Integer, T>> getEntryInRanges(final int index,
	//			final NavigableMap<Integer, T> ranges) {
	//		Entry<Integer, T> entry = ranges.floorEntry(index);
	//
	//		if (entry != null && entry.getValue().contains(index)) {
	//			return Optional.of(entry);
	//		} else {
	//			return Optional.empty();
	//		}
	//	}
	//
	//	public static <T extends IntRange> boolean hasEntryInRanges(final int start,
	//			final NavigableMap<Integer, T> ranges) {
	//		return getEntryInRanges(start, ranges).isPresent();
	//	}
}
