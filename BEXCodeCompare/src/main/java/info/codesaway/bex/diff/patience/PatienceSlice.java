package info.codesaway.bex.diff.patience;

import info.codesaway.bex.IntPair;
import info.codesaway.bex.MutableIntBEXPair;

public class PatienceSlice {
	private final MutableIntBEXPair start;
	private final MutableIntBEXPair end;

	/**
	 * Creates a slice from start to end (exclusive)
	 *
	 * @param start
	 * @param end
	 */
	public PatienceSlice(final IntPair start, final IntPair end) {
		this.start = new MutableIntBEXPair(start);
		this.end = new MutableIntBEXPair(end);
	}

	/**
	 * Creates a slice from start to end (exclusive)
	 * @param leftStart
	 * @param leftEnd
	 * @param rightStart
	 * @param rightEnd
	 */
	public PatienceSlice(final int leftStart, final int leftEnd, final int rightStart, final int rightEnd) {
		this.start = new MutableIntBEXPair(leftStart, rightStart);
		this.end = new MutableIntBEXPair(leftEnd, rightEnd);
	}

	/**
	 * Gets the start (as a left/right IntPair)
	 * @return the start
	 */
	public IntPair getStart() {
		return this.start;
	}

	/**
	 * Gets the end (as a left/right IntPair)
	 * @return the end
	 */
	public IntPair getEnd() {
		return this.end;
	}

	/**
	 * Gets the left start
	 * @return the left start
	 */
	public int getLeftStart() {
		return this.start.getLeft();
	}

	/**
	 * Gets the left end
	 * @return the left end
	 */
	public int getLeftEnd() {
		return this.end.getLeft();
	}

	/**
	 * Gets the right start
	 * @return the right start
	 */
	public int getRightStart() {
		return this.start.getRight();
	}

	/**
	 * Gets the right end
	 * @return the right end
	 */
	public int getRightEnd() {
		return this.end.getRight();
	}

	/**
	 * Indicates if this PatienceSlice is empty
	 */
	// The original source has not_empty? property
	// Instead, I flipped this to be a positive, isEmpty, mimicking the method name in Collection
	// Source: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
	public boolean isEmpty() {
		return this.isLeftEmpty() || this.isRightEmpty();
	}

	/**
	 * Indicates if this PatienceSlice is empty for the left text
	 */
	public boolean isLeftEmpty() {
		return this.getLeftStart() >= this.getLeftEnd();
	}

	/**
	 * Indicates if this PatienceSlice is empty for the right text
	 */
	public boolean isRightEmpty() {
		return this.getRightStart() >= this.getRightEnd();
	}

	/**
	 * Increment both starts by 1
	 */
	public void incrementStarts() {
		this.start.increment();
	}

	/**
	 * Decrement both ends by 1
	 */
	public void decrementEnds() {
		this.end.decrement();
	}
}
