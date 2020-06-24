package info.codesaway.bexcodecompare.diff;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class DiffBlock implements DiffUnit {
	private final DiffType type;
	private final List<DiffEdit> edits;

	public DiffBlock(final DiffType type, final List<DiffEdit> edits) {
		this.type = type;

		// Create defensive copy
		this.edits = ImmutableList.copyOf(edits);
	}

	@Override
	public DiffType getType() {
		return this.type;
	}

	@Override
	public List<DiffEdit> getEdits() {
		return this.edits;
	}
}
