package info.codesaway.bex.diff.substitution.java;

import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

public final class ImportSameClassnameDiffType implements SubstitutionDiffType {
	private final String className;
	private final String leftImport;
	private final String rightImport;
	private final boolean isMove;

	public ImportSameClassnameDiffType(final String classname, final String leftImport, final String rightImport) {
		this(classname, leftImport, rightImport, false);
	}

	public ImportSameClassnameDiffType(final String classname, final String leftImport, final String rightImport,
			final boolean isMove) {
		this.className = classname;
		this.leftImport = leftImport;
		this.rightImport = rightImport;
		this.isMove = isMove;
	}

	@Override
	public char getSymbol() {
		return 'i';
	}

	@Override
	public boolean isMove() {
		return this.isMove;
	}

	@Override
	public String toString() {
		return String.format("import %s (%s, %s)%s", this.className, this.leftImport, this.rightImport,
				this.isMove ? " (MOVE)" : "");
	}
}
