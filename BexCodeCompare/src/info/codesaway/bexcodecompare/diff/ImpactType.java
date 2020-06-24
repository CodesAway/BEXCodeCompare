package info.codesaway.bexcodecompare.diff;

public enum ImpactType {
	NONE("None"), LOW("Low"), MEDIUM("Medium"), HIGH("High"), UNKNOWN("");

	private final String displayValue;

	private ImpactType(final String displayValue) {
		this.displayValue = displayValue;
	}

	public String getDisplayValue() {
		return this.displayValue;
	}

	@Override
	public String toString() {
		return this.getDisplayValue();
	}

	public int getImpact() {
		return this.ordinal();
	}
}
