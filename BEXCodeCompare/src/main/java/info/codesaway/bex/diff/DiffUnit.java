package info.codesaway.bex.diff;

import java.util.List;
import java.util.stream.Stream;

/**
 * Common interface for DiffEdit and DiffBlock
 *
 */
// Class created on 1/6/2019
public interface DiffUnit {
	public List<DiffEdit> getEdits();

	public DiffType getType();

	public default Stream<DiffEdit> stream() {
		return this.getEdits().stream();
	}

	public default char getSymbol() {
		return this.getType().getSymbol();
	}

	public default boolean isMove() {
		return this.getType().isMove();
	}

	public default boolean shouldTreatAsNormalizedEqual() {
		return this.getType().shouldTreatAsNormalizedEqual();
	}

	public default boolean isSubstitution() {
		return this.getType().isSubstitution();
	}

	public default boolean shouldIgnore() {
		return this.getType().shouldIgnore();
	}

	public default boolean isInsertOrDelete() {
		return this.getType() == BasicDiffType.INSERT || this.getType() == BasicDiffType.DELETE;
	}
}
