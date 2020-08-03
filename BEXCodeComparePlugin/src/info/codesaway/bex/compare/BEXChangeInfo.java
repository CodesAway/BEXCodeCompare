package info.codesaway.bex.compare;

public final class BEXChangeInfo {
	private final boolean isImportantChange;
	private final String info;

	public BEXChangeInfo(final boolean isImportantChange, final String info) {
		this.isImportantChange = isImportantChange;
		this.info = info;
	}

	public boolean isImportantChange() {
		return this.isImportantChange;
	}

	@Override
	public String toString() {
		return this.info + (this.isImportantChange ? " (IMPORTANT)" : "");
	}
}
