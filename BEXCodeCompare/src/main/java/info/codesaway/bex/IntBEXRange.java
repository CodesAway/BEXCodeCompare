package info.codesaway.bex;

import java.util.Objects;

/**
 * An IntPair representing a start and end
 */
public final class IntBEXRange implements IntRange {
	private final int start;
	private final int end;

	/**
	 *
	 * @param start the start
	 * @param end the end (exclusive)
	 */
	private IntBEXRange(final int start, final int end) {
		this.start = start;
		this.end = end;
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
		return new IntBEXRange(start, end);
	}

	@Override
	public String toString() {
		return "[" + this.getStart() + "," + this.getEnd() + ")";
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.end, this.start);
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
		return this.end == other.end && this.start == other.start;
	}

}
