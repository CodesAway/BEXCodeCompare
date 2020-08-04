package info.codesaway.bex.matching;

import static info.codesaway.bex.util.BEXUtilities.getSubstring;

import info.codesaway.bex.IntPair;

public interface BEXMatchResult {
	public BEXPattern pattern();

	public String text();

	public IntPair startEndPair();

	public default int start() {
		return this.startEndPair().getLeft();
	}

	public default int end() {
		return this.startEndPair().getRight();
	}

	public default String group() {
		return getSubstring(this.text(), this.startEndPair());
	}

	public IntPair startEndPair(String group);

	public default int start(final String group) {
		return this.startEndPair(group).getLeft();
	}

	public default int end(final String group) {
		return this.startEndPair(group).getRight();
	}

	public default String group(final String group) {
		IntPair startEndPair = this.startEndPair(group);
		if (startEndPair.getLeft() == -1 || startEndPair.getRight() == -1) {
			return null;
		}

		return getSubstring(this.text(), startEndPair);
	}

	/**
	 * Gets the value for the specified group
	 *
	 * <p>If there are multiple values, the first non-null is returned (or <code>null</code> if they are all null)</p>
	 * @param group the group name
	 * @return the value for the specified group (may be <code>null</code>, such as for regex capture groups)
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the group is not specified in the pattern
	 */
	public default String get(final String group) {
		return this.group(group);
	}
}
