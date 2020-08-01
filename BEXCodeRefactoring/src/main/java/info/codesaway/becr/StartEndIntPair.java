package info.codesaway.becr;

import info.codesaway.bex.IntPair;

/**
 * An IntPair representing a start and end
 */
public class StartEndIntPair implements IntPair {
	private final int start;
	private final int end;

	/**
	 *
	 * @param start the start
	 * @param end the end (exclusive)
	 */
	private StartEndIntPair(final int start, final int end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Gets the start
	 * @return the start
	 */
	public int getStart() {
		return this.start;
	}

	/**
	 * Gets the end
	 * @return the end
	 */
	public int getEnd() {
		return this.end;
	}

	@Override
	public int getLeft() {
		return this.getStart();
	}

	@Override
	public int getRight() {
		return this.getEnd();
	}

	public static StartEndIntPair of(final int start, final int end) {
		return new StartEndIntPair(start, end);
	}

	public boolean contains(final int value) {
		return value >= this.start && value < this.end;
	}

	@Override
	public String toString() {
		return "[" + this.getStart() + "," + this.getEnd() + ")";
	}
}
