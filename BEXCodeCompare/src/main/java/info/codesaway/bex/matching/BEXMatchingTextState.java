package info.codesaway.bex.matching;

import info.codesaway.bex.IntRange;

public class BEXMatchingTextState {
	private final IntRange range;
	private final BEXMatchingStateOption stateOption;

	public BEXMatchingTextState(final IntRange range, final BEXMatchingStateOption stateOption) {
		this.range = range;
		this.stateOption = stateOption;
	}

	public IntRange getRange() {
		return this.range;
	}

	public BEXMatchingStateOption getStateOption() {
		return this.stateOption;
	}

	@Override
	public String toString() {
		return this.range + " " + this.stateOption;
	}
}
