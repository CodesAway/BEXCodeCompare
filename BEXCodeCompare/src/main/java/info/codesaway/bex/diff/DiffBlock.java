package info.codesaway.bex.diff;

import static info.codesaway.bex.util.Utilities.immutableCopyOf;

import java.util.List;
import java.util.Objects;

/**
 * Class representing consecutive <code>DiffEdit</code>s with a common DiffType
 */
public class DiffBlock implements DiffUnit {
	private final DiffType type;
	private final List<DiffEdit> edits;

	public DiffBlock(final DiffType type, final List<DiffEdit> edits) {
		this.type = type;

		// Create defensive copy
		this.edits = immutableCopyOf(edits);
		//		this.edits = ImmutableList.copyOf(edits);
	}

	@Override
	public DiffType getType() {
		return this.type;
	}

	@Override
	public List<DiffEdit> getEdits() {
		return this.edits;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.edits, this.type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		DiffBlock other = (DiffBlock) obj;
		return Objects.equals(this.edits, other.edits) && Objects.equals(this.type, other.type);
	}
}
