package info.codesaway.bex;

import info.codesaway.bex.IntPair;

public interface IntRange extends IntPair {
	/**
	 * Gets the start
	 * @return the start
	 */
	public int getStart();

	/**
	 * Gets the end
	 * @return the end
	 */
	public int getEnd();

	@Override
	default int getLeft() {
		return this.getStart();
	}

	@Override
	default int getRight() {
		return this.getEnd();
	}

	public default boolean hasInclusiveStart() {
		return true;
	}

	public default boolean hasInclusiveEnd() {
		return false;
	}

	public default boolean contains(final int value) {
		return value > this.getStart() && value < this.getEnd()
				|| this.hasInclusiveStart() && value == this.getStart()
				|| this.hasInclusiveEnd() && value == this.getEnd();
	}
}
