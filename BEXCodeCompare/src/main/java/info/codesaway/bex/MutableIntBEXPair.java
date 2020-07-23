package info.codesaway.bex;

public final class MutableIntBEXPair implements IntPair {
	private int left;
	private int right;

	/**
	 * Creates a new MutableIntBEXPair with both values 0
	 */
	public MutableIntBEXPair() {
		this(0, 0);
	}

	public MutableIntBEXPair(final IntPair intPair) {
		this(intPair.getLeft(), intPair.getRight());
	}

	public MutableIntBEXPair(final int left, final int right) {
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
		this.set(intPair.getLeft(), intPair.getRight());
	}

	public void set(final BEXSide side, final int newValue) {
		if (side == BEXSide.LEFT) {
			this.setLeft(newValue);
		} else {
			this.setRight(newValue);
		}
	}

	public void set(final int left, final int right) {
		this.setLeft(left);
		this.setRight(right);
	}

	/**
	 * @param left
	 * @since 0.5
	 */
	public void setLeft(final int left) {
		this.left = left;
	}

	/**
	 * @param right
	 * @since 0.5
	 */
	public void setRight(final int right) {
		this.right = right;
	}

	/**
	 * Increment both the left and right int value by 1
	 */
	public void increment() {
		this.left++;
		this.right++;
	}

	/**
	 * Decrement both the left and right int value by 1
	 */
	public void decrement() {
		this.left--;
		this.right--;
	}

	public int incrementAndGet(final BEXSide side) {
		int newValue = this.get(side) + 1;
		this.set(side, newValue);
		return newValue;
	}

	public int getAndIncrement(final BEXSide side) {
		int oldValue = this.get(side);
		this.set(side, oldValue + 1);
		return oldValue;
	}

	/**
	 * Sets the value to the specified update value if the current value equals the expected value
	 *
	 * @param side
	 * @param expect
	 * @param update
	 * @return <b>true</b> if successful
	 */
	public boolean compareAndSet(final BEXSide side, final int expect, final int update) {
		if (this.get(side) == expect) {
			this.set(side, update);
			return true;
		}

		return false;
	}

	/**
	 * Creates a new MutableIntBEXPair with both values initialized to <code>both</code>
	 * @return new MutableIntBEXPair with both values initialized to <code>both</code>
	 */
	public static MutableIntBEXPair of(final int both) {
		return new MutableIntBEXPair(both, both);
	}

	@Override
	public String toString() {
		return "(" + this.getLeft() + ", " + this.getRight() + ")";
	}
}
