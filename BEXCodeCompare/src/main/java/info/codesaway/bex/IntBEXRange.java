package info.codesaway.bex;

import java.util.Objects;

/**
 * An IntPair representing a start and end
 */
public final class IntBEXRange implements IntRange {
	private final int start;
	private final int end;
	private final boolean hasInclusiveEnd;

	/**
	 *
	 * @param start the start
	 * @param end the end (exclusive)
	 */
	private IntBEXRange(final int start, final int end, final boolean hasInclusiveEnd) {
		this.start = start;
		this.end = end;
		this.hasInclusiveEnd = hasInclusiveEnd;
	}

	/**
	 * Gets the start
	 * @return the start
	 */
	@Override
	public int getStart() {
		return this.start;
	}

	/**
	 * Gets the end
	 * @return the end
	 */
	@Override
	public int getEnd() {
		return this.end;
	}

	public static IntBEXRange of(final int start, final int end) {
		return new IntBEXRange(start, end, false);
	}

	/**
	 *
	 * @param start
	 * @param end
	 * @return
	 * @since 0.7
	 */
	public static IntBEXRange closed(final int start, final int end) {
		return new IntBEXRange(start, end, true);
	}

	@Override
	public boolean hasInclusiveStart() {
		return true;
	}

	@Override
	public boolean hasInclusiveEnd() {
		return this.hasInclusiveEnd;
	}

	@Override
	public String toString() {
		return "[" + this.getStart() + "," + this.getEnd()
				+ (this.hasInclusiveEnd ? "]" : ")");
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.end, this.hasInclusiveEnd, this.start);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		IntBEXRange other = (IntBEXRange) obj;
		return this.end == other.end && this.hasInclusiveEnd == other.hasInclusiveEnd && this.start == other.start;
	}

}
