package info.codesaway.becr.matching;

import info.codesaway.becr.StartEndIntPair;

class BECRTextInfo {
	private final StartEndIntPair range;
	private final BECRStateOption stateOption;

	public BECRTextInfo(final StartEndIntPair range, final BECRStateOption stateOption) {
		this.range = range;
		this.stateOption = stateOption;
	}

	public StartEndIntPair getRange() {
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
