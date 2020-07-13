package info.codesaway.bex;

public interface IntPair {
	public int getLeft();

	public int getRight();

	public default int get(final BEXSide side) {
		return side == BEXSide.LEFT ? this.getLeft() : this.getRight();
	}
}
