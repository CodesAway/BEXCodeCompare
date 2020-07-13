package info.codesaway.bex.diff;

import java.util.Optional;

import info.codesaway.bex.BEXSide;

public final class DiffWithIndex {
	private final DiffEdit diffEdit;
	private final int index;

	public DiffWithIndex(final DiffEdit diffEdit, final int index) {
		this.diffEdit = diffEdit;
		this.index = index;
	}

	public DiffEdit getDiffEdit() {
		return this.diffEdit;
	}

	public int getIndex() {
		return this.index;
	}

	public BEXSide getFirstSide() {
		return this.diffEdit.getFirstSide();
	}

	public Optional<DiffLine> getLine(final BEXSide side) {
		return this.diffEdit.getLine(side);
	}

	public boolean hasLeftLine() {
		return this.diffEdit.hasLeftLine();
	}

	public Optional<DiffLine> getLeftLine() {
		return this.diffEdit.getLeftLine();
	}

	public boolean hasRightLine() {
		return this.diffEdit.hasRightLine();
	}

	public Optional<DiffLine> getRightLine() {
		return this.diffEdit.getRightLine();
	}

	public boolean isInsertOrDelete() {
		return this.diffEdit.isInsertOrDelete();
	}

	@Override
	public String toString() {
		return "(" + this.getIndex() + ", " + this.getDiffEdit() + ")";
	}
}
