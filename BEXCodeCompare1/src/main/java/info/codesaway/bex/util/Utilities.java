package info.codesaway.bex.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Utilities {
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
}
