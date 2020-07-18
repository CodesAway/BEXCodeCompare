package info.codesaway.bex;

import java.util.Map;

// Note: created separate class instead of putting into BEXPair interface to make easier to use static imports
// Eclipse didn't seem to support using the BEXPair interface as a favorite to easily use static imports
public final class BEXPairs {
	private BEXPairs() {
		throw new UnsupportedOperationException();
	}

	public static <K, V> BEXPair<V> mapGet(final BEXPair<? extends Map<K, V>> mapPair,
			final BEXPair<K> keyPair) {
		return mapPair.mapWithSide((m, side) -> m.get(keyPair.get(side)));
	}
}
