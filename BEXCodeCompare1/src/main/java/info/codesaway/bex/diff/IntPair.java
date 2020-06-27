package info.codesaway.bex.diff;

public interface IntPair {
	public int getLeft();

	public int getRight();

	public default int get(final DiffSide diffSide) {
		return diffSide == DiffSide.LEFT ? this.getLeft() : this.getRight();
	}
}
