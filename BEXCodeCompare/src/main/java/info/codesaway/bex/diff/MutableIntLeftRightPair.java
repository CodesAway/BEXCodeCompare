package info.codesaway.bex.diff;

public class MutableIntLeftRightPair implements IntPair {
	private int left;
	private int right;

	public MutableIntLeftRightPair(final IntPair intPair) {
		this(intPair.getLeft(), intPair.getRight());
	}

	public MutableIntLeftRightPair(final int left, final int right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int getLeft() {
		return this.left;
	}

	@Override
	public int getRight() {
		return this.right;
	}

	public void set(final IntPair intPair) {
		this.left = intPair.getLeft();
		this.right = intPair.getRight();
	}

	public void set(final DiffSide diffSide, final int newValue) {
		if (diffSide == DiffSide.LEFT) {
			this.left = newValue;
		} else {
			this.right = newValue;
		}
	}

	public void increment() {
		this.left++;
		this.right++;
	}

	public void decrement() {
		this.left--;
		this.right--;
	}

	public int incrementAndGet(final DiffSide diffSide) {
		int newValue = this.get(diffSide) + 1;
		this.set(diffSide, newValue);
		return newValue;
	}

	/**
	 * Sets the value to the specified update value if the current value equals the expected value
	 *
	 * @param diffSide
	 * @param expect
	 * @param update
	 * @return <b>true</b> if successful
	 */
	public boolean compareAndSet(final DiffSide diffSide, final int expect, final int update) {
		if (this.get(diffSide) == expect) {
			this.set(diffSide, update);
			return true;
		}

		return false;
	}

	public static MutableIntLeftRightPair of(final int both) {
		return new MutableIntLeftRightPair(both, both);
	}

	@Override
	public String toString() {
		return "(" + this.getLeft() + ", " + this.getRight() + ")";
	}
}
