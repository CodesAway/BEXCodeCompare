package info.codesaway.bex.diff.substitution;

import java.util.Objects;

import info.codesaway.bex.BEXSide;

// TODO: rename class to something better
public final class RefactoringDiffTypeValue implements RefactoringDiffType {
	private final char symbol;
	private final boolean isMove;

	private final BEXSide side;
	private final String category;
	private final String info;
	private final boolean shouldTreatAsNormalizedEqual;

	/**
	 *
	 * @param symbol
	 * @param side the BEXSide (may be <code>null</code>)
	 * @param category the category (may be <code>null</code>)
	 * @param info the info (may be <code>null</code>)
	 */
	public RefactoringDiffTypeValue(final char symbol, final BEXSide side, final String category, final String info) {
		this(symbol, side, category, info, false);
	}

	/**
	 *
	 * @param symbol
	 * @param side the BEXSide (may be <code>null</code>)
	 * @param category the category (may be <code>null</code>)
	 * @param info the info (may be <code>null</code>)
	 * @param shouldTreatAsNormalizedEqual
	 */
	public RefactoringDiffTypeValue(final char symbol, final BEXSide side, final String category, final String info,
			final boolean shouldTreatAsNormalizedEqual) {
		this(symbol, side, category, info, shouldTreatAsNormalizedEqual, false);
	}

	/**
	 *
	 * @param symbol
	 * @param side the BEXSide (may be <code>null</code>)
	 * @param category the category (may be <code>null</code>)
	 * @param info the info (may be <code>null</code>)
	 * @param isMove
	 */
	public RefactoringDiffTypeValue(final char symbol, final BEXSide side, final String category, final String info,
			final boolean shouldTreatAsNormalizedEqual, final boolean isMove) {
		this.symbol = symbol;
		this.shouldTreatAsNormalizedEqual = shouldTreatAsNormalizedEqual;
		this.isMove = isMove;

		this.side = side;
		this.category = category;
		this.info = info;
	}

	@Override
	public char getSymbol() {
		return this.symbol;
	}

	@Override
	public boolean isMove() {
		return this.isMove;
	}

	public BEXSide getSide() {
		return this.side;
	}

	public String getCategory() {
		return this.category;
	}

	public String getInfo() {
		return this.info;
	}

	@Override
	public boolean shouldTreatAsNormalizedEqual() {
		return this.shouldTreatAsNormalizedEqual;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("REFACTOR");

		if (this.side != null) {
			result.append(' ').append(this.side);
		}

		if (this.category != null) {
			result.append(" (").append(this.category).append(')');
		}

		if (this.info != null) {
			result.append(" \"").append(this.info).append('"');
		}

		return result.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.category, this.side, this.info, this.isMove, this.shouldTreatAsNormalizedEqual,
				this.symbol);
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
		RefactoringDiffTypeValue other = (RefactoringDiffTypeValue) obj;
		return Objects.equals(this.category, other.category) && this.side == other.side
				&& Objects.equals(this.info, other.info) && this.isMove == other.isMove
				&& this.shouldTreatAsNormalizedEqual == other.shouldTreatAsNormalizedEqual && this.symbol == other.symbol;
	}
}
