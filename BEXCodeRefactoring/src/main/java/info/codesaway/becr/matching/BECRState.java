package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRStateOption.IN_STRING_LITERAL;
import static info.codesaway.becr.matching.BECRStateOption.MISMATCHED_BRACKETS;

import java.util.EnumSet;

final class BECRState {
	private final int position;
	private final String brackets;
	private final EnumSet<BECRStateOption> options;

	public BECRState(final int position, final String brackets, final BECRStateOption... options) {
		this.position = position;
		this.brackets = brackets;
		this.options = EnumSet.noneOf(BECRStateOption.class);

		for (BECRStateOption option : options) {
			if (option != null) {
				this.options.add(option);
			}
		}
	}

	public int getPosition() {
		return this.position;
	}

	public String getBrackets() {
		return this.brackets;
	}

	public boolean isInStringLiteral() {
		return this.options.contains(IN_STRING_LITERAL);
	}

	public boolean hasMismatchedBrackets() {
		return this.options.contains(MISMATCHED_BRACKETS);
	}

	public boolean isValid(final int expectedPosition) {
		//		System.out.printf("Is valid?? %d ?= %d\t%s\t%s%n", this.position, expectedPosition, this.brackets,
		//				this.options);

		return (this.position == expectedPosition || expectedPosition == -1)
				&& this.brackets.length() == 0
				&& this.options.isEmpty();
	}

	@Override
	public String toString() {
		return String.format("BECRState[%s, %s, %s", this.position, this.brackets, this.options);
	}
}
