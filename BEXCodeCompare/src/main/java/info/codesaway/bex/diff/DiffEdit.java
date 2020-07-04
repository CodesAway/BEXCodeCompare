package info.codesaway.bex.diff;

import static info.codesaway.bex.util.Utilities.checkArgument;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DiffEdit implements DiffUnit {
	// Reference: https://blog.jcoglan.com/2017/02/17/the-myers-diff-algorithm-part-3/
	private final DiffType type;

	private final Optional<DiffLine> leftLine;
	private final Optional<DiffLine> rightLine;

	/**
	 * Value to use with optional if there is no line (that is, it's absent)
	 *
	 * <p>This is used to help the readability of the code</p>
	 */
	private static final DiffLine ABSENT_LINE = new DiffLine(-1, "");

	public static final Comparator<DiffEdit> LEFT_LINE_NUMBER_COMPARATOR = Comparator
			.comparingInt(DiffEdit::getLeftLineNumber);

	public static final Comparator<DiffEdit> RIGHT_LINE_NUMBER_COMPARATOR = Comparator
			.comparingInt(DiffEdit::getRightLineNumber);

	/**
	 *
	 * @param type
	 * @param leftLine left line (may be <code>null</code>)
	 * @param rightLine right line (may be <code>null</code>
	 */
	public DiffEdit(final DiffType type, final DiffLine leftLine, final DiffLine rightLine) {
		this(type, Optional.ofNullable(leftLine), Optional.ofNullable(rightLine));
	}

	public DiffEdit(final DiffType type, final Optional<DiffLine> leftLine, final Optional<DiffLine> rightLine) {
		checkArgument(leftLine.isPresent() || rightLine.isPresent(), "Either left line or right line is required.");

		this.type = type;
		this.leftLine = leftLine;
		this.rightLine = rightLine;
	}

	@Override
	public DiffType getType() {
		return this.type;
	}

	public boolean isInsertOrDelete() {
		return this.getType() == BasicDiffType.INSERT || this.getType() == BasicDiffType.DELETE;
	}

	@Override
	public List<DiffEdit> getEdits() {
		return Arrays.asList(this);
	}

	/**
	 * Gets the first side which has data (left or right)
	 */
	public DiffSide getFirstSide() {
		return this.hasLeftLine() ? DiffSide.LEFT : DiffSide.RIGHT;
	}

	public Optional<DiffLine> getLine(final DiffSide diffSide) {
		return diffSide == DiffSide.LEFT ? this.getLeftLine() : this.getRightLine();
	}

	public Optional<DiffLine> getLeftLine() {
		return this.leftLine;
	}

	public Optional<DiffLine> getRightLine() {
		return this.rightLine;
	}

	/**
	 * Indicates if has left line
	 */
	public boolean hasLeftLine() {
		return this.getLeftLine().isPresent();
	}

	/**
	 * Indicates if has right line
	 */
	public boolean hasRightLine() {
		return this.getRightLine().isPresent();
	}

	public String getText() {
		return this.getText(this.getFirstSide());
	}

	public String getText(final DiffSide diffSide) {
		return this.getLine(diffSide).orElse(ABSENT_LINE).getText();
	}

	/**
	 * Gets the text for the left line
	 */
	public String getLeftText() {
		return this.getText(DiffSide.LEFT);
	}

	/**
	 * Gets the text for the right line
	 */
	public String getRightText() {
		return this.getText(DiffSide.RIGHT);
	}

	/**
	 * Gets the line number for the each side
	 *
	 * @return the line number (or -1 if there is no line for the specified side)
	 */
	public IntLeftRightPair getLineNumber() {
		return IntLeftRightPair.of(this.getLeftLineNumber(), this.getRightLineNumber());
	}

	/**
	 * Gets the line number for the left line
	 *
	 * @return the line number of the left line (or -1 if there is no left line)
	 */
	public int getLeftLineNumber() {
		return this.leftLine.orElse(ABSENT_LINE).getNumber();
	}

	/**
	 * Gets the line number for the right line
	 *
	 * @return the line number of the right line (or -1 if there is no right line, such as for a deleted line)
	 */
	public int getRightLineNumber() {
		return this.rightLine.orElse(ABSENT_LINE).getNumber();
	}

	/**
	 * Gets the line number (as a String) for the specified side
	 *
	 * @return the line number (or the empty string if there is no line for the specified side)
	 */
	public String getLineNumberString(final DiffSide diffSide) {
		return this.getLine(diffSide)
				.map(l -> Integer.toString(l.getNumber()))
				.orElse("");
	}

	@Override
	public String toString() {
		return this.toString(this.getType().getTag());
	}

	public String toString(final char tag) {
		return this.toString(tag, false);
	}

	/**
	 *
	 *
	 * @param shouldHandleSubstitutionSpecial
	 * @return
	 */
	public String toString(final boolean shouldHandleSubstitutionSpecial) {
		return this.toString(this.getType().getTag(), shouldHandleSubstitutionSpecial);
	}

	/**
	 * @param tag
	 * @param shouldHandleSubstitutionSpecial
	 * @return
	 */
	public String toString(final char tag, final boolean shouldHandleSubstitutionSpecial) {
		String leftLineNumber = this.getLineNumberString(DiffSide.LEFT);
		String rightLineNumber = this.getLineNumberString(DiffSide.RIGHT);

		if (shouldHandleSubstitutionSpecial && this.getType().isSubstitution()) {
			// Format with line numbers
			return String.format("%s%6s%6s    %s%n"
					+ "%s%6s%6s    %s"
			// Left
					, tag, leftLineNumber, "", this.getLeftText()
					// Right
					, tag, "", rightLineNumber, this.getRightText());
		}

		// Format with line numbers
		return String.format("%s%6s%6s    %s", tag, leftLineNumber, rightLineNumber, this.getText());
	}

	public String toString(final DiffSide diffSide) {
		char tag = this.getType().getTag();
		String lineNumber = this.getLineNumberString(diffSide);

		String leftLineNumber = diffSide == DiffSide.LEFT ? lineNumber : "";
		String rightLineNumber = diffSide == DiffSide.RIGHT ? lineNumber : "";

		// Format with line numbers
		return String.format("%s%6s%6s    %s", tag, leftLineNumber, rightLineNumber, this.getText(diffSide));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.leftLine, this.rightLine, this.type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		DiffEdit other = (DiffEdit) obj;
		return Objects.equals(this.leftLine, other.leftLine) && Objects.equals(this.rightLine, other.rightLine)
				&& Objects.equals(this.type, other.type);
	}
}
