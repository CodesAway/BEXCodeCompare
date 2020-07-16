package info.codesaway.bex.diff.substitution.java;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

public final class ImportSameClassnameDiffType implements SubstitutionDiffType {
	private final String className;
	private final BEXPair<String> importPackage;
	private final boolean isMove;

	public ImportSameClassnameDiffType(final String classname, final BEXPair<String> importPackage,
			final boolean isMove) {
		this.className = classname;
		this.importPackage = importPackage;
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
		return String.format("import %s %s%s",
				this.className,
				this.importPackage.toString("(%s, %s)"),
				this.isMove ? " (MOVE)" : "");
	}
}
