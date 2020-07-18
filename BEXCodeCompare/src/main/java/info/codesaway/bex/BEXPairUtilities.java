package info.codesaway.bex;

import java.util.Map;

// Note: created separate class instead of putting into BEXPairCore interface to make easier to use static imports
// Eclipse didn't seem to support using the BEXPairCore interface as a favorite to easily use static imports
public final class BEXPairUtilities {
	private BEXPairUtilities() {
		throw new UnsupportedOperationException();
	}

	public static <K, V> BEXPair<V> mapGet(final BEXPairCore<? extends Map<K, V>> mapPair, final BEXPair<K> keyPair) {
		return mapPair.mapWithSide((m, side) -> m.get(keyPair.get(side)));
	}
}
