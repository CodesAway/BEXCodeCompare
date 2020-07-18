package info.codesaway.bex;

public interface IntPair {
	public int getLeft();

	public int getRight();

	public default int get(final BEXSide side) {
		return side == BEXSide.LEFT ? this.getLeft() : this.getRight();
	}

	/**
	 * Returns a BEXPair&lt;Integer&gt; representing this IntPair
	 * @return a BEXPair&lt;Integer&ght; representing this IntPair
	 * @since 0.4
	 */
	public default BEXPair<Integer> toBEXPair() {
		return new BEXPair<>(this.getLeft(), this.getRight());
	}

}
