package info.codesaway.bex.diff;

import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.util.BEXUtilities.checkArgument;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.IntBEXPair;

// TODO: see if can simplify this code by using BEXPair
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

	public DiffEdit(final DiffType type, final BEXPair<Optional<DiffLine>> line) {
		this(type, line.getLeft(), line.getRight());
	}

	public DiffEdit(final BEXSide side, final DiffType type, final DiffLine line) {
		this(type,
				side == LEFT ? line : null,
				side == RIGHT ? line : null);
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

	@Override
	public List<DiffEdit> getEdits() {
		return Arrays.asList(this);
	}

	@Override
	public Stream<DiffEdit> stream() {
		return Stream.of(this);
	}

	/**
	 * Gets the first side which has data (left or right)
	 */
	public BEXSide getFirstSide() {
		return this.hasLeftLine() ? LEFT : RIGHT;
	}

	public Optional<DiffLine> getLine(final BEXSide side) {
		return side == LEFT ? this.getLeftLine() : this.getRightLine();
	}

	public Optional<DiffLine> getLeftLine() {
		return this.leftLine;
	}

	public Optional<DiffLine> getRightLine() {
		return this.rightLine;
	}

	/**
	 * Indicates if has line on the specified side
	 * @param side the side
	 * @return <code>true</code> if has line on the specified side
	 * @since 0.4
	 */
	public boolean hasLine(final BEXSide side) {
		return side == LEFT ? this.hasLeftLine() : this.hasRightLine();
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

	public String getText(final BEXSide side) {
		return this.getLine(side).orElse(ABSENT_LINE).getText();
	}

	/**
	 * Gets the text for the left line
	 */
	public String getLeftText() {
		return this.getText(LEFT);
	}

	/**
	 * Gets the text for the right line
	 */
	public String getRightText() {
		return this.getText(RIGHT);
	}

	/**
	 * Gets the line number for the each side
	 *
	 * @return the line number (or -1 if there is no line for the specified side)
	 */
	public IntBEXPair getLineNumber() {
		return IntBEXPair.of(this.getLeftLineNumber(), this.getRightLineNumber());
	}

	/**
	 * Gets the line number for the line on the specified side
	 * @param side the side
	 *
	 * @return the line number (or -1 if there is no line on the specified side)
	 * @since 0.4
	 */
	public int getLineNumber(final BEXSide side) {
		return side == LEFT ? this.getLeftLineNumber() : this.getRightLineNumber();
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
	public String getLineNumberString(final BEXSide side) {
		return this.getLine(side)
				.map(l -> Integer.toString(l.getNumber()))
				.orElse("");
	}

	@Override
	public String toString() {
		return this.toString(this.getType().getSymbol());
	}

	public String toString(final char symbol) {
		return this.toString(symbol, false);
	}

	/**
	 *
	 *
	 * @param shouldHandleSubstitutionSpecial
	 * @return
	 */
	public String toString(final boolean shouldHandleSubstitutionSpecial) {
		return this.toString(this.getType().getSymbol(), shouldHandleSubstitutionSpecial);
	}

	/**
	 * @param symbol
	 * @param shouldHandleSubstitutionSpecial
	 * @return
	 */
	public String toString(final char symbol, final boolean shouldHandleSubstitutionSpecial) {
		String leftLineNumber = this.getLineNumberString(LEFT);
		String rightLineNumber = this.getLineNumberString(RIGHT);

		if (shouldHandleSubstitutionSpecial && this.isSubstitution()) {
			// Format with line numbers
			return String.format("%s%6s%6s    %s%n"
					+ "%s%6s%6s    %s"
			// Left
					, symbol, leftLineNumber, "", this.getLeftText()
					// Right
					, symbol, "", rightLineNumber, this.getRightText());
		}

		// Format with line numbers
		return String.format("%s%6s%6s    %s", symbol, leftLineNumber, rightLineNumber, this.getText());
	}

	public String toString(final BEXSide side) {
		char symbol = this.getSymbol();
		String lineNumber = this.getLineNumberString(side);

		String leftLineNumber = side == LEFT ? lineNumber : "";
		String rightLineNumber = side == RIGHT ? lineNumber : "";

		// Format with line numbers
		return String.format("%s%6s%6s    %s", symbol, leftLineNumber, rightLineNumber, this.getText(side));
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
