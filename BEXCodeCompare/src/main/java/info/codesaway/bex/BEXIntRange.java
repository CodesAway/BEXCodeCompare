package info.codesaway.bex;

/**
 * An IntPair representing a start and end
 */
public final class BEXIntRange implements IntRange {
	private final int start;
	private final int end;

	/**
	 *
	 * @param start the start
	 * @param end the end (exclusive)
	 */
	private BEXIntRange(final int start, final int end) {
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

	public static BEXIntRange of(final int start, final int end) {
		return new BEXIntRange(start, end);
	}

	@Override
	public String toString() {
		return "[" + this.getStart() + "," + this.getEnd() + ")";
	}
}
