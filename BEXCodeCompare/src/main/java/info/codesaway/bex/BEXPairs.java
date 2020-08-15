package info.codesaway.bex;

import java.util.Map;

// Note: created separate class instead of putting into BEXPair interface to make easier to use static imports
// Eclipse didn't seem to support using the BEXPair interface as a favorite to easily use static imports
public final class BEXPairs {
	private BEXPairs() {
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 * @param <T> the type
	 * @param left the left value
	 * @param right the right value
	 * @return a BEXPair with the specified left / right values
	 * @since 0.10
	 */
	public static <T> BEXPair<T> bexPair(final T left, final T right) {
		return new BEXPairValue<>(left, right);
	}

	public static <K, V> BEXPair<V> mapGet(final BEXPair<? extends Map<K, V>> mapPair,
			final BEXPair<K> keyPair) {
		return mapPair.mapWithSide((m, side) -> m.get(keyPair.get(side)));
	}
}
