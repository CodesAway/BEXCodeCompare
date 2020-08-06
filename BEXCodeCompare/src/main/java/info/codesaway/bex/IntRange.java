package info.codesaway.bex;

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

	public boolean hasInclusiveStart();

	public boolean hasInclusiveEnd();

	public default boolean contains(final int value) {
		return value > this.getStart() && value < this.getEnd()
				|| this.hasInclusiveStart() && value == this.getStart()
				|| this.hasInclusiveEnd() && value == this.getEnd();
	}

	public default boolean isEmpty() {
		return this.getStart() == this.getEnd()
				&& !(this.hasInclusiveStart() && this.hasInclusiveEnd());
	}

	public default IntRange canonical() {
		if (this.hasInclusiveStart() && !this.hasInclusiveEnd()) {
			return this;
		}

		int start = this.hasInclusiveStart() ? this.getStart() : this.getStart() + 1;
		int end = this.hasInclusiveEnd() ? this.getEnd() + 1 : this.getEnd();
		return IntBEXRange.of(start, end);
	}

	/**
	 * Returns an IntBEXRange (immutable) for this IntRange
	 * @return an IntBEXRange (immutable) for this IntRange
	 * @since 0.8
	 */
	public default IntBEXRange toIntBEXRange() {
		if (this instanceof IntBEXRange) {
			return (IntBEXRange) this;
		}

		return new IntBEXRange(this.getStart(), this.hasInclusiveStart(), this.getEnd(), this.hasInclusiveEnd());
	}
}
