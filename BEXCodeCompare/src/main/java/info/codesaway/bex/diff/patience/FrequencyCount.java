package info.codesaway.bex.diff.patience;

import info.codesaway.bex.BEXSide;

/**
 * Instances of this class track the number of occurrences and first occurrence of the line in the current slice
 *
 * <p>This class is mutable and will change state as new occurrences of the line of text are found.</p>
 *
 * <p>Instances of this class are not safe for use by multiple concurrent threads.</p>
 */
public final class FrequencyCount {
	// Referenced: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/

	/**
	 * Indicates that the line has not been found in the slice
	 */
	private static int NOT_FOUND = -1;

	/**
	 * The number of times the line appears in the left text (within the current slice)
	 */
	private int leftCount;

	/**
	 * The number of times the line appears in the right text (within the current slice)
	 */
	private int rightCount;

	/**
	 * The line number of the first occurrence of the line in the left text (or -1 if no occurrence is found)
	 */
	private int leftLineNumber = NOT_FOUND;

	/**
	 * The line number of the first occurrence of the line in the right text (or -1 if no occurrence is found)
	 */
	private int rightLineNumber = NOT_FOUND;

	public int getLeftLineNumber() {
		return this.leftLineNumber;
	}

	public int getRightLineNumber() {
		return this.rightLineNumber;
	}

	/**
	 * Records that the line was found in the slice
	 * <ul>
	 * <li>Increments count
	 * <li>Sets the line number if this is the first occurrence of the text</li>
	 * </ul>
	 *
	 * @param side {@link BEXSide#LEFT} or {@link BEXSide#RIGHT}
	 * @param lineNumber the line number in the line text
	 * @return <code>this</code>
	 */
	public FrequencyCount recordFoundInSlice(final BEXSide side, final int lineNumber) {
		if (side == BEXSide.LEFT) {
			this.leftCount++;
			if (this.leftLineNumber == NOT_FOUND) {
				this.leftLineNumber = lineNumber;
			}
		} else {
			this.rightCount++;
			if (this.rightLineNumber == NOT_FOUND) {
				this.rightLineNumber = lineNumber;
			}
		}

		return this;
	}

	/**
	 * Indicates if the line is unique within both documents
	 *
	 * @return <code>true</code> if the line is unique within both documents
	 */
	public boolean isLineUnique() {
		return this.leftCount == 1 && this.rightCount == 1;
	}

	/**
	 *
	 * @param frequencyCount a FrequencyCount (possibly <code>null</code>)
	 * @return the specified value if not <code>null</code>; otherwise, creates a new FrequencyCount
	 */
	public static FrequencyCount emptyIfNull(final FrequencyCount frequencyCount) {
		return frequencyCount != null ? frequencyCount : new FrequencyCount();
	}
}
