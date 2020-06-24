package info.codesaway.bexcodecompare.diff.patience;

/**
 * Class to represent the hashed element in the Patience diff algorithm
 *
 * <p>Objects of this class track the number of occurrences and first occurrence of the line in the current slice</p>
 *
 * <p>This class is mutable and will change state as new occurrences of the line of text are found</p>
 *
 * <p>Referenced: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/</p>
 */
public class FrequencyCount {
	/**
	 * The number of times the line appears in the first file (within the current slice)
	 */
	private int count1;

	/**
	 * The number of times the line appears in the second file (within the current slice)
	 */
	private int count2;

	/**
	 * The line number of the first occurrence of the line in the first s(or -1 if no occurrence is found)
	 */
	private int lineNumber1;

	/**
	 * The line number of the first occurrence of the line in the first file (or -1 if no occurrence is found)
	 */
	private int lineNumber2;

	/**
	 * Indicates that the line has not been found in the slice
	 */
	private static int NOT_FOUND = -1;

	public FrequencyCount() {
		// Counts are defaulted to 0 by Java already, which is the correct initial value

		// Initialize line number to -1 to indicate that the line hasn't been found yet
		this.lineNumber1 = NOT_FOUND;
		this.lineNumber2 = NOT_FOUND;
	}

	public int getCount1() {
		return this.count1;
	}

	public int getCount2() {
		return this.count2;
	}

	private void setCount1(final int count1) {
		this.count1 = count1;
	}

	private void setCount2(final int count2) {
		this.count2 = count2;
	}

	private void incrementCount1() {
		this.setCount1(this.getCount1() + 1);
	}

	private void incrementCount2() {
		this.setCount2(this.getCount2() + 1);
	}

	public int getLineNumber1() {
		return this.lineNumber1;
	}

	public int getLineNumber2() {
		return this.lineNumber2;
	}

	private void setLineNumber1(final int lineNumber1) {
		this.lineNumber1 = lineNumber1;
	}

	private void setLineNumber2(final int lineNumber2) {
		this.lineNumber2 = lineNumber2;
	}

	private void setLineNumber1IfNotFoundYet(final int lineNumber1) {
		if (this.getLineNumber1() == NOT_FOUND) {
			this.setLineNumber1(lineNumber1);
		}
	}

	private void setLineNumber2IfNotFoundYet(final int lineNumber2) {
		if (this.getLineNumber2() == NOT_FOUND) {
			this.setLineNumber2(lineNumber2);
		}
	}

	/**
	 * Records that the line was found in the slice for the first file
	 * <ul>
	 * <li>Increments count
	 * <li>Sets the line number if this is the first occurrence of the text</li>
	 * </ul>
	 *
	 * @param lineNumber1 the line number in the first file
	 * @return <code>this</code>
	 */
	public FrequencyCount recordFoundInSlice1(final int lineNumber1) {
		this.incrementCount1();
		this.setLineNumber1IfNotFoundYet(lineNumber1);
		return this;
	}

	/**
	 * Records that the line was found in the slice for the second file
	 * <ul>
	 * <li>Increments count
	 * <li>Sets the line number if this is the first occurrence of the text</li>
	 * </ul>
	 *
	 * @param lineNumber2 the line number in the second file
	 * @return <code>this</code>
	 */
	public FrequencyCount recordFoundInSlice2(final int lineNumber2) {
		this.incrementCount2();
		this.setLineNumber2IfNotFoundYet(lineNumber2);
		return this;
	}

	/**
	 * Indicates if the line is unique within both documents
	 *
	 * <p>This occurs when <code>{@link #getCount1()} = 1 and {@link #getCount2()} = 1</code></p>
	 *
	 * @return
	 */
	public boolean isLineUnique() {
		return this.getCount1() == 1 && this.getCount2() == 1;
	}
}
