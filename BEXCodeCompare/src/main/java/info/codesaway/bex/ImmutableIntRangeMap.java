package info.codesaway.bex;

import static info.codesaway.bex.util.BEXUtilities.checkArgument;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

/**
 *
 * @param <V> type of value
 */
public class ImmutableIntRangeMap<V> {
	// Referenced Guava's ImmutableRangeMap
	// Based on usage in BEXMatchingUtilities
	private final List<IntRange> ranges;
	private final List<V> values;

	private static final ImmutableIntRangeMap<Object> EMPTY = new ImmutableIntRangeMap<>(
			Collections.emptyList(), Collections.emptyList());

	/** Returns an empty immutable range map. */
	@SuppressWarnings("unchecked")
	public static <V> ImmutableIntRangeMap<V> of() {
		return (ImmutableIntRangeMap<V>) EMPTY;
	}

	private ImmutableIntRangeMap(final List<IntRange> ranges, final List<V> values) {
		this.ranges = ranges;
		this.values = values;
	}

	/** Returns a new builder for an immutable range map. */
	public static <V> Builder<V> builder() {
		return new Builder<>();
	}

	/**
	 * A builder for immutable range maps. Overlapping ranges are prohibited.
	 */
	public static final class Builder<V> {
		private final List<Entry<IntRange, V>> entries;

		public Builder() {
			this.entries = new ArrayList<>();
		}

		/**
		 * Associates the specified range with the specified value.
		 *
		 * @throws IllegalArgumentException if {@code range} is empty
		 */
		public Builder<V> put(final IntRange range, final V value) {
			Objects.requireNonNull(range);
			Objects.requireNonNull(value);
			checkArgument(!range.isEmpty(), "Range must not be empty, but was " + range);
			this.entries.add(new SimpleImmutableEntry<>(range.toIntBEXRange(), value));

			return this;
		}

		/**
		 * Returns an {@code ImmutableRangeMap} containing the associations previously added to this
		 * builder.
		 *
		 * @throws IllegalArgumentException if any two ranges inserted into this builder overlap
		 */
		public ImmutableIntRangeMap<V> build() {
			Collections.sort(this.entries, Comparator.comparingInt(e -> e.getKey().getStart()));
			List<IntRange> ranges = new ArrayList<>(this.entries.size());
			List<V> values = new ArrayList<>(this.entries.size());
			for (int i = 0; i < this.entries.size(); i++) {
				IntRange range = this.entries.get(i).getKey();
				if (i > 0) {
					IntRange prevRange = this.entries.get(i - 1).getKey();
					//					if (range.isConnected(prevRange) && !range.intersection(prevRange).isEmpty()) {

					// TODO: will this correctly determine if there's overlap?
					// TODO: what if start isn't inclusive?
					if (prevRange.contains(range.getStart()) || range.contains(prevRange.getStart())) {
						throw new IllegalArgumentException(
								"Overlapping ranges: range " + prevRange + " overlaps with entry " + range);
					}
				}

				ranges.add(range);
				values.add(this.entries.get(i).getValue());
			}

			ranges = Collections.unmodifiableList(ranges);
			values = Collections.unmodifiableList(values);

			return new ImmutableIntRangeMap<>(ranges, values);
		}
	}

	public V get(final int key) {
		int index = this.getIndex(key);
		if (index == -1) {
			return null;
		} else {
			IntRange range = this.ranges.get(index);
			return range.contains(key) ? this.values.get(index) : null;
		}
	}

	public Entry<IntRange, V> getEntry(final int key) {
		int index = this.getIndex(key);
		if (index == -1) {
			return null;
		} else {
			IntRange range = this.ranges.get(index);
			return range.contains(key) ? new SimpleImmutableEntry<>(range, this.values.get(index)) : null;
		}
	}

	private int getIndex(final int key) {
		int index = this.binarySearch(key);

		if (index >= 0) {
			return index;
		}

		// So, we take -index - 1 to find the insertion point
		// Then, we get the prior entry, since this would be the range which contained the value
		// If the key is before all the ranges, -1 would be returned, indicating it's not found
		return (-index - 1) - 1;
	}

	/**
	 * Searches the ranges for the specified value using the
	 * binary search algorithm.
	 *
	 * @param key the value to be searched for
	 * @return index of the search key, if it is contained in the array;
	 *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	 *         <i>insertion point</i> is defined as the point at which the
	 *         key would be inserted into the array: the index of the first
	 *         element greater than the key, or <tt>a.length</tt> if all
	 *         elements in the array are less than the specified key.  Note
	 *         that this guarantees that the return value will be &gt;= 0 if
	 *         and only if the key is found.
	 */
	private int binarySearch(final int key) {
		// Based on Collections.indexedBinarySearch
		int low = 0;
		int high = this.ranges.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = this.ranges.get(mid).getStart();
			int cmp = Integer.compare(midVal, key);

			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}
		return -(low + 1); // key not found
	}

	@Override
	public String toString() {
		StringJoiner result = new StringJoiner(", ", "{", "}");

		for (int i = 0; i < this.ranges.size(); i++) {
			result.add(this.ranges.get(i) + "=" + this.values.get(i));
		}

		return result.toString();
	}
}
