package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRGroupMatchSetting.STOP_WHEN_VALID;
import static info.codesaway.becr.matching.BECRMatchingUtilities.lastChar;
import static info.codesaway.becr.matching.BECRMatchingUtilities.nextChar;
import static info.codesaway.becr.matching.BECRStateOption.IN_STRING_LITERAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import info.codesaway.bex.IntBEXPair;
import info.codesaway.bex.IntPair;
import info.codesaway.bex.MutableIntBEXPair;
import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

public final class BECRMatcher {
	private static final boolean DEBUG = false;

	// TODO: likely these won't be final since likely want to add similar functionality as in Pattern / Matcher regex classes
	private final BECRPattern parentPattern;
	private final CharSequence text;

	private final MutableIntBEXPair matchStartEnd = new MutableIntBEXPair(-1, 0);

	/**
	 * Most groups will have only one value, so stored here
	 */
	private final Map<String, IntPair> singleValues = new HashMap<>();

	/**
	 * Groups with multiple values are stored here
	 */
	private final Map<String, List<IntPair>> multipleValues = new HashMap<>();

	BECRMatcher(final BECRPattern parent, final CharSequence text) {
		this.parentPattern = parent;
		this.text = text;
	}

	public BECRPattern pattern() {
		return this.parentPattern;
	}

	public String text() {
		return this.text.toString();
	}

	public boolean find() {
		// Logic from regex Matcher.find
		int nextSearchStart = this.end();
		if (nextSearchStart == this.start()) {
			nextSearchStart++;
		}
		return this.search(nextSearchStart);
	}

	private boolean search(final int from) {
		this.singleValues.clear();
		this.multipleValues.clear();

		boolean foundMatch = this.match(from);
		if (!foundMatch) {
			this.matchStartEnd.setLeft(-1);
		}

		return foundMatch;
	}

	private boolean match(final int from) {
		List<Pattern> patterns = this.parentPattern.getPatterns();

		if (patterns.isEmpty()) {
			return false;
		}

		// TODO: cache matchers?
		Matcher currentMatcher = patterns.get(0).matcher(this.text);
		if (!currentMatcher.find(from)) {
			if (DEBUG) {
				System.out.println("Couldn't find match 0: " + from + "\t" + this.text());
				System.out.println("Pattern 0: @" + currentMatcher.pattern() + "@");
			}
			return false;
		}

		this.putCaptureGroups(currentMatcher);
		int regionStart = currentMatcher.end();

		for (int i = 0; i < patterns.size() - 1; i++) {
			Pattern nextPattern = patterns.get(i + 1);

			Matcher nextMatcher = nextPattern.matcher(this.text);
			nextMatcher.region(regionStart, this.text.length());

			if (!nextMatcher.find()) {
				if (DEBUG) {
					System.out.printf("Didn't match next matcher %d%n", i + 1);
				}
				return false;
			}

			int start = regionStart;
			int end = nextMatcher.start();

			BECRGroupMatchSetting groupMatchSetting = this.parentPattern.getGroupMatchSettings().get(i);

			BECRState state = search(this.text, start, end, groupMatchSetting);

			if (!state.isValid(end)) {
				// TODO: if has mismatched brackets, start over and try to find after this?
				// This way, if one line in a file isn't valid, could still handle other lines (versus never matching ever)
				if (state.hasMismatchedBrackets()) {
					if (DEBUG) {
						System.out.println(state);
					}
					return false;
				}

				// TODO: handle what if not valid (in this case, expand group)
				if (DEBUG) {
					System.out.printf("Not valid group value: @%s@%n", this.text.subSequence(start, end));
				}

				BECRState validState = search(this.text, state.getPosition(), this.text.length(),
						groupMatchSetting.turnOn(STOP_WHEN_VALID), state);

				if (!validState.isValid(-1)) {
					// Still not valid
					// TODO: should keep trying until valid?
					if (DEBUG) {
						System.out.println("Still not valid");
					}
					return false;
				}

				int position = validState.getPosition();

				if (DEBUG) {
					System.out.println("Valid state at position " + position + "\t"
							+ this.text.subSequence(start, position));
				}

				nextMatcher.region(position, this.text.length());

				if (!nextMatcher.find()) {
					// State is valid and cannot find another match
					// In this case, skip and try again from beginning at later point?
					if (DEBUG) {
						System.out.println("Cannot find next match: " + (i + 1));
					}
					return false;
				}

				if (nextMatcher.start() != position) {
					// TODO: there may be extra stuff between the valid position and the next start
					// (if this is also valid, it would be
					if (DEBUG) {
						System.out.printf("New scenario %d: %d\t%d\t%s%n", i + 1, nextMatcher.start(), position,
								this.text.subSequence(position, nextMatcher.start()));

						System.out.printf("Position does not match next matcher start: %d != %d", position,
								nextMatcher.start());
					}
					return false;
				}

				end = nextMatcher.start();
			}

			String group = this.parentPattern.getGroups().get(i);
			//			String value = this.text.subSequence(start, end).toString();

			// If group is already specified, the values must match
			// (unless it's an unnamed group)
			if (!group.equals("_")) {
				// TODO: what if group was matched as part of regex, does normal group have to match?
				IntPair startEnd = this.getInternal(group);

				if (startEnd != null && startEnd != NOT_FOUND) {
					// Verify the content equals; otherwise, don't match
					// TODO: should go to next match or something... need to implement
					int oldLength = startEnd.getRight() - startEnd.getLeft();
					int newLength = end - start;

					// Fast check, based on length
					if (newLength != oldLength) {
						return false;
					}

					// Compare character by character
					int index1 = startEnd.getLeft();
					int index2 = start;

					for (int m = 0; m < oldLength; m++) {
						char c1 = this.text.charAt(index1++);
						char c2 = this.text.charAt(index2++);

						if (c1 != c2) {
							return false;
						}
					}
				}
			}

			this.put(group, IntBEXPair.of(start, end));
			this.putCaptureGroups(nextMatcher);

			//			System.out.printf("%s: @%s@%n", group, value);
			regionStart = nextMatcher.end();
		}

		int matchStart = currentMatcher.start();
		int matchEnd = regionStart;

		this.matchStartEnd.set(matchStart, matchEnd);

		if (DEBUG) {
			System.out.println("Found match: " + this.matchStartEnd);
		}

		return true;
	}

	private static BECRState search(final CharSequence text, final int start, final int end,
			final BECRGroupMatchSetting groupMatchSetting) {
		return search(text, start, end, groupMatchSetting, null);
	}

	private static BECRState search(final CharSequence text, final int start, final int end,
			final BECRGroupMatchSetting groupMatchSetting, final BECRState state) {
		// Verify parentheses / brackets are balanced
		// TODO: handle string (what if group is in String??)
		// TODO: handle comments (what if group is in comments??)

		boolean shouldStopWhenValid = groupMatchSetting.shouldStopWhenValid();

		// By default, don't include angled brackets <> as part of balancing (unless specified)
		String bracketStarts;
		String bracketEnds;

		if (groupMatchSetting.shouldMatchAngleBrackets()) {
			bracketStarts = "([{<";
			bracketEnds = ")]}>";
		} else {
			bracketStarts = "([{";
			bracketEnds = ")]}";
		}

		StringBuilder brackets = new StringBuilder();
		boolean inStringLiteral = false;

		if (state != null) {
			brackets.append(state.getBrackets());
			inStringLiteral = state.isInStringLiteral();
		}

		for (int i = start; i < end; i++) {
			char c = text.charAt(i);

			if (inStringLiteral) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						// If there is no next character, return false since not valid
						return new BECRState(i, brackets.toString(), IN_STRING_LITERAL);
					}

					i++;
				} else if (c == '"') {
					// End of String literal
					inStringLiteral = false;

					if (shouldStopWhenValid && brackets.length() == 0) {
						return new BECRState(i + 1, "");
					}
				}
				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (bracketStarts.indexOf(c) != -1) {
				brackets.append(c);
			} else if (c == '"') {
				// String literal
				inStringLiteral = true;
			} else {
				int bracketEndIndex = bracketEnds.indexOf(c);

				if (bracketEndIndex != -1) {
					char bracketStart = bracketStarts.charAt(bracketEndIndex);
					if (lastChar(brackets) != bracketStart) {
						return new BECRState(i, brackets.toString());
					} else {
						// Remove last character
						brackets.setLength(brackets.length() - 1);

						if (shouldStopWhenValid && brackets.length() == 0) {
							return new BECRState(i + 1, "");
						}
					}
				}
			}
		}

		//		System.out.println("inStringLiteral? " + inStringLiteral + "\t" + brackets);

		return new BECRState(end, brackets.toString(), inStringLiteral ? IN_STRING_LITERAL : null);
	}

	private void putCaptureGroups(final Matcher matcher) {
		// Add any capture groups from the Pattern
		// TODO: (only care about named capture groups?)
		for (int i = 1; i <= matcher.groupCount(); i++) {
			String groupName = matcher.getGroupName(i);

			if (groupName == null) {
				continue;
			}

			// If group name is something like name[1], just care about "name"
			int occurrenceStart = groupName.indexOf('[');
			if (occurrenceStart != -1) {
				groupName = groupName.substring(0, occurrenceStart);
			}

			this.put(groupName, IntBEXPair.of(matcher.start(i), matcher.end(i)));
		}
	}

	// TODO: instead of putting text value, just put int pair (start / end)
	// Can use (-1, -1) to indicate not found
	// This way, only convert to String value when requested (in case text passed isn't a String but for example a StringBuilder)
	private void put(final String group, final IntPair value) {
		List<IntPair> existingValues = this.multipleValues.get(group);

		if (existingValues != null) {
			// Already has existing values, so add to the collection
			existingValues.add(value);
			return;
		}

		IntPair existingValue = this.singleValues.get(group);

		if (existingValue == null) {
			// First time seeing the specified group
			this.singleValues.put(group, value);
		} else {
			// Put existing value and new value into a collection in multipleValues
			this.singleValues.remove(group);
			List<IntPair> newValues = new ArrayList<>();
			newValues.add(existingValue);
			newValues.add(value);
			this.multipleValues.put(group, newValues);
		}
	}

	public IntPair startEndPair() {
		return this.matchStartEnd.toIntBEXPair();
	}

	public int start() {
		return this.matchStartEnd.getLeft();
	}

	public int end() {
		return this.matchStartEnd.getRight();
	}

	public String group() {
		return this.getSubstring(this.matchStartEnd);
	}

	// Added to correspond to Regex Matcher.group
	public String group(final String group) {
		return this.get(group);
	}

	/**
	 *
	 * Gets the value for the specified group
	 *
	 * <p>If there are multiple values, the first non-null is returned (or <code>null</code> if they are all null)</p>
	 * @param group
	 * @return the value for the specified group (may be <code>null</code>, such as for regex capture groups)
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the group is not specified in the pattern
	 */
	public String get(final String group) {
		IntPair startEnd = this.getInternal(group);

		// Intentionally using identity equals
		if (startEnd == NOT_FOUND) {
			throw new IllegalArgumentException("The specified group is not in the pattern: " + group);
		}

		if (startEnd == null) {
			return null;
		}

		return this.getSubstring(startEnd);
	}

	private static IntPair NOT_FOUND = IntBEXPair.of(Integer.MIN_VALUE, Integer.MIN_VALUE);

	private IntPair getInternal(final String group) {
		List<IntPair> existingValues = this.multipleValues.get(group);

		if (existingValues != null) {
			// Return first non-null value or return null if all values are null
			return existingValues.stream()
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
		}

		return this.singleValues.getOrDefault(group, NOT_FOUND);
	}

	private String getSubstring(final IntPair startEnd) {
		return this.getSubSequence(startEnd).toString();
	}

	private CharSequence getSubSequence(final IntPair startEnd) {
		return this.text.subSequence(startEnd.getLeft(), startEnd.getRight());
	}
}
