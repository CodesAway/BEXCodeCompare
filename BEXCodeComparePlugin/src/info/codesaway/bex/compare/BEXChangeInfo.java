package info.codesaway.bex.compare;

public final class BEXChangeInfo {
	private final boolean isImportantChange;
	private final String info;
	private final int number;

	public BEXChangeInfo(final boolean isImportantChange, final String info) {
		this.isImportantChange = isImportantChange;
		this.info = info;
		this.number = -1;
	}

	public BEXChangeInfo(final boolean isImportantChange, final int number) {
		this.isImportantChange = isImportantChange;
		this.info = "Change " + number;
		this.number = number;
	}

	public boolean isImportantChange() {
		return this.isImportantChange;
	}

	/**
	 *
	 * @return
	 * @since 0.14
	 */
	public String getInfo() {
		return this.info;
	}

	public int getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		return this.info + (this.isImportantChange ? " (IMPORTANT)" : "");
	}
}
