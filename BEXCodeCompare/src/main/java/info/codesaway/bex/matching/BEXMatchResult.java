package info.codesaway.bex.matching;

import static info.codesaway.bex.util.BEXUtilities.getSubstring;

import java.util.Map.Entry;
import java.util.Set;

import info.codesaway.bex.IntBEXRange;

public interface BEXMatchResult {
	public BEXPattern pattern();

	public String text();

	/**
	 *
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public IntBEXRange range();

	/**
	 *
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default int start() {
		return this.range().getStart();
	}

	/**
	 *
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default int end() {
		return this.range().getEnd();
	}

	/**
	 *
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default String group() {
		return getSubstring(this.text(), this.range());
	}

	/**
	 *
	 * @param group
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public IntBEXRange range(String group);

	/**
	 *
	 * @param group
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default int start(final String group) {
		return this.range(group).getStart();
	}

	/**
	 *
	 * @param group
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default int end(final String group) {
		return this.range(group).getEnd();
	}

	/**
	 *
	 * @param group
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default String group(final String group) {
		if (group.equals("*")) {
			return this.group();
		}

		IntBEXRange range = this.range(group);
		if (range.getStart() == -1 || range.getEnd() == -1) {
			return null;
		}

		return getSubstring(this.text(), range);
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
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public default String get(final String group) {
		return this.group(group);
	}

	/**
	 *
	 * @return
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	public Set<Entry<String, String>> entrySet();
}
