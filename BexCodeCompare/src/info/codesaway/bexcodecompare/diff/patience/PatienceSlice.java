package info.codesaway.bexcodecompare.diff.patience;

public class PatienceSlice {
	private int low1;
	private int high1;

	private int low2;
	private int high2;

	/**
	 * Creates a slice from low to high (exclusive)
	 *
	 * @param low1
	 * @param high1
	 * @param low2
	 * @param high2
	 */
	public PatienceSlice(final int low1, final int high1, final int low2, final int high2) {
		this.low1 = low1;
		this.high1 = high1;

		this.low2 = low2;
		this.high2 = high2;
	}

	public int getLow1() {
		return this.low1;
	}

	private void setLow1(final int low1) {
		this.low1 = low1;
	}

	/**
	 * Increment low 1
	 *
	 * @param increment the increment (pass a negative value to decrement)
	 */
	private void incrementLow1(final int increment) {
		this.setLow1(this.getLow1() + increment);
	}

	public int getHigh1() {
		return this.high1;
	}

	private void setHigh1(final int high1) {
		this.high1 = high1;
	}

	/**
	 * Increment high 1
	 *
	 * @param increment the increment (pass a negative value to decrement)
	 */
	private void incrementHigh1(final int increment) {
		this.setHigh1(this.getHigh1() + increment);
	}

	public int getLow2() {
		return this.low2;
	}

	private void setLow2(final int low2) {
		this.low2 = low2;
	}

	/**
	 * Increment low 2
	 *
	 * @param increment the increment (pass a negative value to decrement)
	 */
	private void incrementLow2(final int increment) {
		this.setLow2(this.getLow2() + increment);
	}

	public int getHigh2() {
		return this.high2;
	}

	private void setHigh2(final int high2) {
		this.high2 = high2;
	}

	/**
	 * Increment high 2
	 *
	 * @param increment the increment (pass a negative value to decrement)
	 */
	private void incrementHigh2(final int increment) {
		this.setHigh2(this.getHigh2() + increment);
	}

	/**
	 * Indicates if this PatienceSlice is empty
	 *
	 * @return
	 */
	// The original source has not_empty? property
	// Instead, I flipped this to be a positive, isEmpty, mimicking the method name in Collection
	// Source: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
	public boolean isEmpty() {
		return this.isEmpty1() || this.isEmpty2();
	}

	/**
	 * Indicates if this PatienceSlice is empty for the first file
	 *
	 * @return
	 */
	public boolean isEmpty1() {
		return this.low1 >= this.high1;
	}

	/**
	 * Indicates if this PatienceSlice is empty for the first file
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/01/2019  Initial code
	 *</pre>***********************************************************************************
	 */
	public boolean isEmpty2() {
		return this.low2 >= this.high2;
	}

	/**
	 * Increment both lows by 1
	 *
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/30/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public void incrementLows() {
		this.incrementLow1(1);
		this.incrementLow2(1);
	}

	/**
	 * Decrement both highs by 1
	 *
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/30/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public void decrementHighs() {
		this.incrementHigh1(-1);
		this.incrementHigh2(-1);
	}
}
