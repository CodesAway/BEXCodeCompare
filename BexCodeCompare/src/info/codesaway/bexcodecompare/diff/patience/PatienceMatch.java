package info.codesaway.bexcodecompare.diff.patience;

/**
 * Class to represent the matched lines Patience diff algorithm
 *
 * <p>Objects of this class track matching lines and track the previous / next match, allowing to combine the chain of matches</p>
 *
 * <p>This class is mutable and will change state as we sort the matches using patience sort</p>
 *
 * <p>Referenced: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/</p>
 * @author TRSHCO
 *
 */
public class PatienceMatch {
	private final int lineNumber1;
	private final int lineNumber2;

	private PatienceMatch previous;
	private PatienceMatch next;

	public PatienceMatch(final int lineNumber1, final int lineNumber2) {
		this.lineNumber1 = lineNumber1;
		this.lineNumber2 = lineNumber2;
	}

	public int getLineNumber1() {
		return this.lineNumber1;
	}

	public int getLineNumber2() {
		return this.lineNumber2;
	}

	public PatienceMatch getPrevious() {
		return this.previous;
	}

	public boolean hasPrevious() {
		return this.getPrevious() != null;
	}

	public PatienceMatch getNext() {
		return this.next;
	}

	public void setPrevious(final PatienceMatch previous) {
		this.previous = previous;
	}

	public void setNext(final PatienceMatch next) {
		this.next = next;
	}

	@Override
	public String toString() {
		return "PatienceMatch(" + this.getLineNumber1() + " -> " + this.getLineNumber2() + ")";
	}

	/**
	 * Indicates if this match is immediately after the specified match
	 *
	 * <p>This means that this match has values for {@link #getLineNumber1()} and {@link #getLineNumber2()} which are one after the specified match. Some optimizations can be done for these consecutive matches and this method is a helper method to identify them.</p>
	 *
	 * @param match
	 * @return
	 */
	public boolean isImmediatelyAfter(final PatienceMatch match) {
		return this.getLineNumber1() == match.getLineNumber1() + 1
				&& this.getLineNumber2() == match.getLineNumber2() + 1;
	}
}
