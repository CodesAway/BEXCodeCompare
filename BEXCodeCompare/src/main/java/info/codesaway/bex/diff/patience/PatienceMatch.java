package info.codesaway.bex.diff.patience;

import java.util.Comparator;

import info.codesaway.bex.IntBEXPair;

/**
 * Class to represent the matched lines Patience diff algorithm
 *
 * <p>Objects of this class track matching lines and track the previous / next match, allowing to combine the chain of matches</p>
 *
 * <p>This class is mutable and will change state as we sort the matches using patience sort</p>
 *
 * <p>Referenced: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/</p>
 */
public class PatienceMatch {
	private final int leftLineNumber;
	private final int rightLineNumber;

	private PatienceMatch previous;
	private PatienceMatch next;

	public static final Comparator<PatienceMatch> LEFT_LINE_NUMBER_COMPARATOR = Comparator
			.comparingInt(PatienceMatch::getLeftLineNumber);

	public static final Comparator<PatienceMatch> RIGHT_LINE_NUMBER_COMPARATOR = Comparator
			.comparingInt(PatienceMatch::getRightLineNumber);

	public PatienceMatch(final int leftLineNumber, final int rightLineNumber) {
		this.leftLineNumber = leftLineNumber;
		this.rightLineNumber = rightLineNumber;
	}

	public IntBEXPair getLineNumber() {
		return IntBEXPair.of(this.leftLineNumber, this.rightLineNumber);
	}

	public int getLeftLineNumber() {
		return this.leftLineNumber;
	}

	public int getRightLineNumber() {
		return this.rightLineNumber;
	}

	public PatienceMatch getPrevious() {
		return this.previous;
	}

	public boolean hasPrevious() {
		return this.getPrevious() != null;
	}

	public void setPrevious(final PatienceMatch previous) {
		this.previous = previous;
	}

	public PatienceMatch getNext() {
		return this.next;
	}

	public void setNext(final PatienceMatch next) {
		this.next = next;
	}

	@Override
	public String toString() {
		return "PatienceMatch(" + this.getLeftLineNumber() + " -> " + this.getRightLineNumber() + ")";
	}

	/**
	 * Indicates if this match is immediately after the specified match
	 *
	 * <p>This means that this match has values for {@link #getLeftLineNumber()} and {@link #getRightLineNumber()} which are one after the specified match. Some optimizations can be done for these consecutive matches and this method is a helper method to identify them.</p>
	 *
	 * @param match
	 * @return
	 */
	public boolean isImmediatelyAfter(final PatienceMatch match) {
		return this.getLeftLineNumber() == match.getLeftLineNumber() + 1
				&& this.getRightLineNumber() == match.getRightLineNumber() + 1;
	}
}
