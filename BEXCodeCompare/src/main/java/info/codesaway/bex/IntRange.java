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

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default int getInclusiveStart() {
		return this.hasInclusiveStart() ? this.getStart() : this.getStart() + 1;
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default int getInclusiveEnd() {
		return this.hasInclusiveEnd() ? this.getEnd() : this.getEnd() - 1;
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default int getCanonicalEnd() {
		return this.hasInclusiveEnd() ? this.getEnd() + 1 : this.getEnd();
	}

	public default boolean contains(final int value) {
		return value > this.getStart() && value < this.getEnd()
				|| this.hasInclusiveStart() && value == this.getStart()
				|| this.hasInclusiveEnd() && value == this.getEnd();
	}

	public default boolean isEmpty() {
		return this.getStart() == this.getEnd()
				&& !(this.hasInclusiveStart() && this.hasInclusiveEnd());
	}

	/**
	 *
	 * @return
	 * @since 0.13
	 */
	public default boolean isSingleValue() {
		return this.getStart() == this.getEnd()
				&& this.hasInclusiveStart() && this.hasInclusiveEnd();
	}

	/**
	 *
	 * @return the length of the range
	 * @since 0.10
	 */
	public default int length() {
		return this.getCanonicalEnd() - this.getInclusiveStart();
	}

	public default IntRange canonical() {
		if (this.hasInclusiveStart() && !this.hasInclusiveEnd()) {
			return this;
		}

		return IntBEXRange.of(this.getInclusiveStart(), this.getCanonicalEnd());
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
