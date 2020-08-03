package info.codesaway.bex;

public interface IntPair {
	public int getLeft();

	public int getRight();

	public default int get(final BEXSide side) {
		return side == BEXSide.LEFT ? this.getLeft() : this.getRight();
	}

	/**
	 * Returns a BEXPair&lt;Integer&gt; representing this IntPair
	 * @return a BEXPair&lt;Integer&gt; representing this IntPair
	 * @since 0.4
	 */
	public default BEXPair<Integer> toBEXPair() {
		return new BEXPairValue<>(this.getLeft(), this.getRight());
	}

	/**
	 * Returns an IntBEXPair (immutable) for this IntPair
	 * @return an IntBEXPair (immutable) for this IntPair
	 * @since 0.5
	 */
	public default IntBEXPair toIntBEXPair() {
		if (this instanceof IntBEXPair) {
			return (IntBEXPair) this;
		}

		return IntBEXPair.of(this.getLeft(), this.getRight());
	}
}
