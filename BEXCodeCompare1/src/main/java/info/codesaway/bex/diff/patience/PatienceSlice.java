package info.codesaway.bex.diff.patience;

import info.codesaway.bex.diff.IntPair;
import info.codesaway.bex.diff.MutableIntLeftRightPair;

public class PatienceSlice {
	private final MutableIntLeftRightPair start;
	private final MutableIntLeftRightPair end;

	/**
	 * Creates a slice from start to end (exclusive)
	 *
	 * @param leftStart
	 * @param leftEnd
	 * @param rightStart
	 * @param rightEnd
	 */
	public PatienceSlice(final IntPair start, final IntPair end) {
		this.start = new MutableIntLeftRightPair(start);
		this.end = new MutableIntLeftRightPair(end);
	}

	public PatienceSlice(final int leftStart, final int leftEnd, final int rightStart, final int rightEnd) {
		this.start = new MutableIntLeftRightPair(leftStart, rightStart);
		this.end = new MutableIntLeftRightPair(leftEnd, rightEnd);
	}

	public IntPair getStart() {
		return this.start;
	}

	public IntPair getEnd() {
		return this.end;
	}

	public int getLeftStart() {
		return this.start.getLeft();
	}

	public int getLeftEnd() {
		return this.end.getLeft();
	}

	public int getRightStart() {
		return this.start.getRight();
	}

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
