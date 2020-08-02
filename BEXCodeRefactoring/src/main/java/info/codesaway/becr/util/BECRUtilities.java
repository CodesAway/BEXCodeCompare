package info.codesaway.becr.util;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;

import info.codesaway.becr.IntRange;

public final class BECRUtilities {
	private BECRUtilities() {
		throw new UnsupportedOperationException();
	}

	public static <T extends IntRange> Optional<Entry<Integer, T>> getEntryInRanges(final int start,
			final NavigableMap<Integer, T> ranges) {
		Entry<Integer, T> entry = ranges.floorEntry(start);

		if (entry != null && entry.getValue().contains(start)) {
			return Optional.of(entry);
		} else {
			return Optional.empty();
		}
	}

	public static <T extends IntRange> boolean hasEntryInRanges(final int start,
			final NavigableMap<Integer, T> ranges) {
		return getEntryInRanges(start, ranges).isPresent();
	}
}
