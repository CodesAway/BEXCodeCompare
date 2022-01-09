package info.codesaway.bex.diff;

import static info.codesaway.bex.util.BEXUtilities.immutableCopyOf;
import static java.util.stream.Collectors.toList;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class representing a change (such as a refactoring), which affects multiple DiffUnit (such as DiffEdit or DiffBlock)
 * which may or may not be consecutive lines
 */
public final class DiffChange<T> implements DiffUnit {
	private final DiffType diffType;
	private final List<DiffUnit> changes;
	private final T info;

	/**
	 * @param changes list of changes
	 * @param info data related to the change (such as change notes)
	 */
	public DiffChange(final DiffType diffType, final List<DiffUnit> changes, final T info) {
		this.diffType = diffType;
		this.changes = immutableCopyOf(changes);
		//		this.changes = ImmutableList.copyOf(changes);
		this.info = info;
	}

	public boolean hasChanges() {
		return !this.changes.isEmpty();
	}

	@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Immutable copy in constructor")
	public List<DiffUnit> getChanges() {
		return this.changes;
	}

	public T getInfo() {
		return this.info;
	}

	@Override
	public List<DiffEdit> getEdits() {
		return this.changes.stream()
				.flatMap(DiffUnit::stream)
				.collect(toList());
	}

	@Override
	public DiffType getType() {
		return this.diffType;
	}

	@Override
	public String toString() {
		return String.format("%s - %s", this.getType(), this.getInfo());
	}
}