package info.codesaway.becr.matching;

import info.codesaway.becr.IntRange;

public class BECRTextState {
	private final IntRange range;
	private final BECRStateOption stateOption;

	public BECRTextState(final IntRange range, final BECRStateOption stateOption) {
		this.range = range;
		this.stateOption = stateOption;
	}

	public IntRange getRange() {
		return this.range;
	}

	public BECRStateOption getStateOption() {
		return this.stateOption;
	}

	@Override
	public String toString() {
		return this.range + " " + this.stateOption;
	}
}
