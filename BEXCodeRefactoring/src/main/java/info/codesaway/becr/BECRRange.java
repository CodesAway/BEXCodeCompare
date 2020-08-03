package info.codesaway.becr;

/**
 * An IntPair representing a start and end
 */
public final class BECRRange implements IntRange {
	private final int start;
	private final int end;

	/**
	 *
	 * @param start the start
	 * @param end the end (exclusive)
	 */
	private BECRRange(final int start, final int end) {
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

	public static BECRRange of(final int start, final int end) {
		return new BECRRange(start, end);
	}

	@Override
	public String toString() {
		return "[" + this.getStart() + "," + this.getEnd() + ")";
	}
}
