package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXGroupMatchSetting.DEFAULT;
import static info.codesaway.bex.matching.BEXGroupMatchSetting.STOP_WHEN_VALID;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.matching.BEXMatchingUtilities.extractJavaTextStates;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasNextChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasText;
import static info.codesaway.bex.matching.BEXMatchingUtilities.isWordCharacter;
import static info.codesaway.bex.matching.BEXMatchingUtilities.lastChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.nextChar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.function.Function;

import info.codesaway.bex.IntBEXRange;
import info.codesaway.bex.MutableIntBEXPair;
import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

/**
 *
 * @since 0.5
 */
public final class BEXMatcher implements BEXMatchResult {
	static final boolean DEBUG = false;

	private final BEXPattern parentPattern;

	/**
	 * The index of the last position appended in a substitution.
	 * @since 0.6
	 */
	private int lastAppendPosition = 0;

	private CharSequence text;

	/**
	 * Map from range start to BEXMatchingTextState (contains range and text state)
	 */
	private NavigableMap<Integer, BEXMatchingTextState> textStateMap;

	/**
	 * Offset used when resoulving indexes in textStateMap (allows sharing textStateMap such as in BEXString)
	 */
	private int offset;

	private final MutableIntBEXPair matchStartEnd = new MutableIntBEXPair(-1, 0);

	/**
	 * Most groups will have only one value, so stored here
	 */
	private final Map<String, IntBEXRange> singleValues = new HashMap<>();

	/**
	 * Groups with multiple values are stored here
	 */
	private final Map<String, List<IntBEXRange>> multipleValues = new HashMap<>();

	BEXMatcher(final BEXPattern parent, final CharSequence text) {
		this(parent, text, extractJavaTextStates(text), 0);
	}

	BEXMatcher(final BEXPattern parent, final CharSequence text,
			final NavigableMap<Integer, BEXMatchingTextState> textStateMap, final int offset) {
		this.parentPattern = parent;
		this.text = text;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	@Override
	public BEXPattern pattern() {
		return this.parentPattern;
	}

	@Override
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
		// TODO: specify as option? (needed to handle spaces after group)
		currentMatcher.useTransparentBounds(true);

		boolean foundMatch;
		int searchFrom = from;
		do {
			if (!currentMatcher.find(searchFrom)) {
				if (DEBUG) {
					System.out.println("Couldn't find match 0: " + from + "\t" + this.text());
					System.out.println("Pattern 0: @" + currentMatcher.pattern() + "@");
				}
				return false;
			}

			int start = currentMatcher.start();
			int startWithOffset = start + this.offset;
			// TODO: refactor to use helper method in BEXUtilities
			Entry<Integer, BEXMatchingTextState> entry = this.textStateMap.floorEntry(startWithOffset);
			if (entry != null && entry.getValue().getRange().contains(startWithOffset)
					&& startWithOffset != entry.getKey()) {
				// Don't count as match, since part of string literal or comment
				// If match starts with the block, then okay to match
				// TODO: when else would it be okay to match?
				foundMatch = false;
				searchFrom = start + 1;
			} else {
				foundMatch = true;
				if (DEBUG) {
					if (start == startWithOffset) {
						System.out.printf("Found match! %d%n", start);
					} else {
						System.out.printf("Found match! %d (%d)%n", start, startWithOffset);
					}
					System.out.println(this.textStateMap);
				}
			}
		} while (!foundMatch);

		// Don't match if in string literal or comment
		// TODO: under what scenarios should it match stuff in comments?

		// TODO: need to process code before match to detect if in block comment, line comment, or String literal

		this.putCaptureGroups(currentMatcher);
		int regionStart = currentMatcher.end();
		// TODO: keep track of matchStart (such as if requires multiple passes to find next match)

		for (int i = 0; i < patterns.size() - 1; i++) {
			Pattern nextPattern = patterns.get(i + 1);

			Matcher nextMatcher = nextPattern.matcher(this.text);

			if (DEBUG) {
				System.out.println("Region start: " + regionStart);
			}

			nextMatcher.region(regionStart, this.text.length());
			nextMatcher.useTransparentBounds(true);

			if (!nextMatcher.find()) {
				if (DEBUG) {
					System.out.printf("Didn't match next matcher %d%n", i + 1);
					System.out.println("Pattern: " + nextPattern);
					System.out.println("Text: " + this.text.subSequence(regionStart, this.text.length()));
				}
				return false;
			}

			if (DEBUG) {
				System.out.printf("Matched next match %d %s\t%s%n", i + 1, nextMatcher.pattern(), nextMatcher.group());
			}

			int start = regionStart;
			int end = nextMatcher.start();

			// If match starts with " and prior character is \ then ignore
			// (trying to handle match within string, while ignoring escaped double quote)
			// TODO: is there a better way to do this?
			// Doesn't work, since regionStart is used to determine when the capture group starts
			// Instead, when building the pattern, if see double quote character, need to ensure not escaped
			//			if (hasText(this.text, end, "\"") && prevChar(this.text, end) == '\\') {
			//				// Redo the current pattern
			//				i--;
			//
			//				// Start with the character after the double quote
			//				regionStart = end + 1;
			//
			//				continue;
			//			}

			int startWithOffset = start + this.offset;
			// TODO: refactor to use helper in BEXUtilities
			Entry<Integer, BEXMatchingTextState> entry = this.textStateMap.floorEntry(startWithOffset);
			BEXMatchingState initialState;
			if (entry != null && entry.getValue().getRange().contains(startWithOffset)) {
				initialState = new BEXMatchingState(-1, "", entry.getValue().getStateOption());
			} else {
				initialState = BEXMatchingState.DEFAULT;
			}

			BEXGroupMatchSetting groupMatchSetting = this.parentPattern.getGroupMatchSettings()
					.getOrDefault(i, DEFAULT);

			BEXMatchingState state = search(this.text, start, end, groupMatchSetting, initialState);

			while (!state.isValid(end, initialState.getOptions())) {
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

				BEXMatchingState validState = search(this.text, state.getPosition(), this.text.length(),
						groupMatchSetting.turnOn(STOP_WHEN_VALID), state);

				// TODO: should I ignore the initialState options?
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
				// TODO: specify as option? (needed to handle spaces after group)
				nextMatcher.useTransparentBounds(true);

				if (!nextMatcher.find()) {
					// State is valid and cannot find another match
					// In this case, skip and try again from beginning at later point?
					if (DEBUG) {
						System.out.println("Cannot find next match: " + (i + 1));
					}
					return false;
				}

				end = nextMatcher.start();

				if (end != position) {
					// TODO: there may be extra stuff between the valid position and the next start
					// (if this is also valid, it would be okay)
					if (DEBUG) {
						System.out.printf("New scenario %d: %d\t%d\t%s%n", i + 1, nextMatcher.start(), position,
								this.text.subSequence(position, nextMatcher.start()));

						System.out.printf("Position does not match next matcher start: %d != %d%n", position,
								nextMatcher.start());
					}
					//					return false;
					// TODO: what should I pass for initial state
					state = search(this.text, position, end, groupMatchSetting);
				} else {
					break;
				}
			}

			if (start == end && !groupMatchSetting.isOptional()) {
				// TODO: check if expanding group would allow to match
				return false;
			}

			String group = this.parentPattern.getGroups().get(i);
			//			String value = this.text.subSequence(start, end).toString();

			// If group is already specified, the values must match
			// (unless it's an unnamed group)
			if (!group.equals("_")) {
				// TODO: what if group was matched as part of regex, does normal group have to match?
				IntBEXRange startEnd = this.getInternal(group);

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

			this.put(group, IntBEXRange.of(start, end));
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

	private static BEXMatchingState search(final CharSequence text, final int start, final int end,
			final BEXGroupMatchSetting groupMatchSetting) {
		return search(text, start, end, groupMatchSetting, null);
	}

	private static BEXMatchingState search(final CharSequence text, final int start, final int end,
			final BEXGroupMatchSetting groupMatchSetting, final BEXMatchingState state) {
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
		boolean isInStringLiteral = false;
		boolean isInLineComment = false;
		boolean isInMultilineComment = false;

		if (state != null) {
			brackets.append(state.getBrackets());
			isInStringLiteral = state.isInStringLiteral();
			isInLineComment = state.isInLineComment();
			isInMultilineComment = state.isInMultilineComment();
		}

		for (int i = start; i < end; i++) {
			char c = text.charAt(i);

			if (isInStringLiteral) {
				if (c == '\\') {
					// Escape next character
					if (nextChar(text, i) == '\0') {
						// If there is no next character, return false since not valid
						return new BEXMatchingState(i, brackets.toString(), IN_STRING_LITERAL);
					}

					i++;
				} else if (c == '"') {
					// End of String literal
					isInStringLiteral = false;

					if (shouldStopWhenValid && brackets.length() == 0) {
						return new BEXMatchingState(i + 1, "");
					}
				}
				// Other characters don't matter??
				// TODO: handle unicode and other escaping in String literal
			} else if (isInLineComment) {
				if (c == '\n' || c == '\r') {
					isInLineComment = false;
				}
				// Other characters don't matter?
			} else if (isInMultilineComment) {
				if (hasText(text, i, "*/")) {
					isInMultilineComment = false;
					i++;
				}
			} else if (c == '/' && nextChar(text, i) == '/') {
				isInLineComment = true;
				i++;
			} else if (c == '/' && nextChar(text, i) == '*') {
				isInMultilineComment = true;
				i++;
			} else if (bracketStarts.indexOf(c) != -1) {
				brackets.append(c);
			} else if (c == '"') {
				// String literal
				isInStringLiteral = true;
			} else {
				int bracketEndIndex = bracketEnds.indexOf(c);

				if (bracketEndIndex != -1) {
					char bracketStart = bracketStarts.charAt(bracketEndIndex);
					if (lastChar(brackets) != bracketStart) {
						return new BEXMatchingState(i, brackets.toString());
					} else {
						// Remove last character
						brackets.setLength(brackets.length() - 1);

						if (shouldStopWhenValid && brackets.length() == 0) {
							return new BEXMatchingState(i + 1, "");
						}
					}
				}
			}
		}

		//		System.out.println("inStringLiteral? " + inStringLiteral + "\t" + brackets);

		return new BEXMatchingState(end, brackets.toString(),
				isInStringLiteral ? IN_STRING_LITERAL : null,
				isInLineComment ? IN_LINE_COMMENT : null,
				isInMultilineComment ? IN_MULTILINE_COMMENT : null);
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

			this.put(groupName, IntBEXRange.of(matcher.start(i), matcher.end(i)));
		}
	}

	// TODO: instead of putting text value, just put int pair (start / end)
	// Can use (-1, -1) to indicate not found
	// This way, only convert to String value when requested (in case text passed isn't a String but for example a StringBuilder)
	private void put(final String group, final IntBEXRange value) {
		List<IntBEXRange> existingValues = this.multipleValues.get(group);

		if (existingValues != null) {
			// Already has existing values, so add to the collection
			existingValues.add(value);
			return;
		}

		IntBEXRange existingValue = this.singleValues.get(group);

		if (existingValue == null) {
			// First time seeing the specified group
			this.singleValues.put(group, value);
		} else {
			// Put existing value and new value into a collection in multipleValues
			this.singleValues.remove(group);
			List<IntBEXRange> newValues = new ArrayList<>();
			newValues.add(existingValue);
			newValues.add(value);
			this.multipleValues.put(group, newValues);
		}
	}

	@Override
	public IntBEXRange startEndPair() {
		return IntBEXRange.of(this.matchStartEnd.getLeft(), this.matchStartEnd.getRight());
	}

	@Override
	public IntBEXRange startEndPair(final String group) {
		IntBEXRange startEnd = this.getInternal(group);

		// Intentionally using identity equals
		if (startEnd == NOT_FOUND) {
			throw new IllegalArgumentException("The specified group is not in the pattern: " + group);
		}

		return startEnd;
	}

	private static IntBEXRange NOT_FOUND = IntBEXRange.of(Integer.MIN_VALUE, Integer.MIN_VALUE);
	private static IntBEXRange NULL_PAIR = IntBEXRange.of(-1, -1);

	private IntBEXRange getInternal(final String group) {
		List<IntBEXRange> existingValues = this.multipleValues.get(group);

		if (existingValues != null) {
			// Return first non-null value or return null if all values are null
			return existingValues.stream()
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(NULL_PAIR);
		}

		return this.singleValues.getOrDefault(group, NOT_FOUND);
	}

	/**
	 * Resets this matcher.
	 *
	 * <p>Resetting a matcher discards all of its explicit state information
	 * and sets its append position to zero.</p>
	 *
	 * @return  This matcher
	 * @since 0.6
	 */
	public BEXMatcher reset() {
		this.matchStartEnd.set(-1, 0);
		this.singleValues.clear();
		this.multipleValues.clear();
		this.lastAppendPosition = 0;

		// TODO: support region?
		//		from = 0;
		//		to = this.getTextLength();
		return this;
	}

	/**
	 * Resets this matcher with a new input sequence.
	 *
	 * <p>Resetting a matcher discards all of its explicit state information
	 * and sets its append position to zero.</p>
	 *
	 * @param  input
	 *         The new input character sequence
	 *
	 * @return  This matcher
	 * @since 0.6
	 */
	public BEXMatcher reset(final CharSequence input) {
		this.text = input;
		this.textStateMap = extractJavaTextStates(input);
		this.offset = 0;
		return this.reset();
	}

	/**
	 * Resets this matcher with a new input sequence.
	 *
	 * <p>Resetting a matcher discards all of its explicit state information
	 * and sets its append position to zero.</p>
	 *
	 * @param  bexString
	 *         The new input character sequence
	 *
	 * @return  This matcher
	 * @since 0.6
	 */
	public BEXMatcher reset(final BEXString bexString) {
		this.text = bexString.getText();
		this.textStateMap = bexString.getTextStateMap();
		this.offset = bexString.getOffset();
		return this.reset();
	}

	/**
	 *
	 * @param replacement the replacement string
	 * @return The string constructed by replacing each matching subsequence by
	 *         the replacement string (return from applying replacementFunction), substituting captured subsequences as
	 *         needed
	 * @since 0.6
	 */
	public String replaceAll(final String replacement) {
		this.reset();
		boolean result = this.find();
		if (result) {
			StringBuffer sb = new StringBuffer();
			do {
				this.appendReplacement(sb, replacement);
				result = this.find();
			} while (result);
			this.appendTail(sb);
			return sb.toString();
		}
		return this.text.toString();
	}

	/**
	 *
	 * @param replacementFunction function that takes the BEXMatchResult for the current match and returns the replacement (supports referencing group)
	 * @return The string constructed by replacing each matching subsequence by
	 *         the replacement string (return from applying replacementFunction), substituting captured subsequences as
	 *         needed
	 * @since 0.6
	 */
	public String replaceAll(final Function<BEXMatchResult, String> replacementFunction) {
		this.reset();
		boolean result = this.find();
		if (result) {
			StringBuffer sb = new StringBuffer();
			do {
				this.appendReplacement(sb, replacementFunction.apply(this));
				result = this.find();
			} while (result);
			this.appendTail(sb);
			return sb.toString();
		}
		return this.text.toString();
	}

	/**
	 * Replaces the first subsequence of the input sequence that matches the
	 * pattern with the given replacement string.
	 *
	 * <p>This method first resets this matcher. It then scans the input
	 * sequence looking for a match of the pattern. Characters that are not part
	 * of the match are appended directly to the result string; the match is
	 * replaced in the result by the replacement string. The replacement string
	 * may contain references to captured subsequences as in the {@link #appendReplacement appendReplacement} method.</p>
	 *
	 * <p>Given the pattern <tt>dog</tt>, the input
	 * <tt>"zzzdogzzzdogzzz"</tt>, and the replacement string <tt>"cat"</tt>, an
	 * invocation of this method on a matcher for that expression would yield
	 * the string <tt>"zzzcatzzzdogzzz"</tt>.</p>
	 *
	 * <p>Invoking this method changes this matcher's state. If the matcher
	 * is to be used in further matching operations then it should first be
	 * reset.</p>
	 *
	 * @param replacement
	 *            The replacement string
	 * @return The string constructed by replacing the first matching
	 *         subsequence by the replacement string, substituting captured
	 *         subsequences as needed
	 * @since 0.6
	 */
	public String replaceFirst(final String replacement) {
		if (replacement == null) {
			throw new NullPointerException("replacement");
		}
		this.reset();
		if (!this.find()) {
			return this.text.toString();
		}
		StringBuffer sb = new StringBuffer();
		this.appendReplacement(sb, replacement);
		this.appendTail(sb);
		return sb.toString();
	}

	/**
	 * <p>Gets the replacement string, replacing any group references with their actual value</p>
	 *
	 * @param replacement
	 *            The replacement string
	 *
	 * @return the replacement string replacing any group references with their group value from this BEXMatcher
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the replacement string refers to a capturing
	 *             group that does not exist in the pattern
	 *
	 * @since 0.6
	 */
	public String getReplacement(final String replacement) {
		int cursor = 0;
		StringBuilder result = new StringBuilder();

		while (cursor < replacement.length()) {
			char nextChar = replacement.charAt(cursor);

			if (nextChar == ':' && nextChar(replacement, cursor) == '['
					&& isWordCharacter(nextChar(replacement, cursor + 1))) {
				int originalStart = cursor;

				// Skip past :[
				cursor += 2;

				// Logic from BEXPattern
				int groupNameStart = cursor;

				char d;

				do {
					if (cursor >= replacement.length()) {
						break;
					}

					d = replacement.charAt(cursor++);
				} while (isWordCharacter(d));

				// Last character isn't part of group name
				cursor--;

				int groupNameEnd = cursor;

				if (hasText(replacement, cursor, "]")) {
					String groupName = replacement.substring(groupNameStart, groupNameEnd);
					String value = this.group(groupName);

					if (value != null) {
						result.append(value);
					}
				} else {
					// Not valid group
					// Throw error since likely intended to use group
					// (also, this way, can add more functionality over time without braking existing code)
					throw new IllegalArgumentException(
							"Invalid syntax: " + replacement.substring(originalStart, cursor + 1));
				}
			} else if (nextChar == ':' && hasText(replacement, cursor + 1, "[:]")) {
				result.append(nextChar);
				cursor += 4;
			} else if (nextChar == ':' && hasText(replacement, cursor + 1, "[*]")) {
				result.append(this.group());
				cursor += 4;
			} else if (nextChar == ':' && nextChar(replacement, cursor) == '[') {
				throw new IllegalArgumentException("Unexpected group syntax: " + replacement.substring(cursor));
			} else {
				result.append(nextChar);
				cursor++;
			}
		}

		return result.toString();

	}

	/**
	 * Implements a non-terminal append-and-replace step.
	 *
	 * <p>This method performs the following actions:</p>
	 *
	 * <ol>
	 *
	 * <li>
	 * <p>It reads characters from the input sequence, starting at the append
	 * position, and appends them to the given string buffer. It stops after
	 * reading the last character preceding the previous match, that is, the
	 * character at index {@link #start()}&nbsp;<tt>-</tt>&nbsp;<tt>1</tt>.</p>
	 * </li>
	 *
	 * <li>
	 * <p>It appends the given replacement string to the string buffer.</p>
	 * </li>
	 *
	 * <li>
	 * <p>It sets the append position of this matcher to the index of the last
	 * character matched, plus one, that is, to {@link #end()}.</p>
	 * </li>
	 *
	 * </ol>
	 *
	 * <p>The replacement string may contain references to subsequences captured
	 * during the previous match</p>
	 *
	 * <p>This method is intended to be used in a loop together with the {@link #appendTail appendTail} and
	 * {@link #find find} methods. The
	 * following code, for example, writes <tt>one dog two dogs in the
	 * yard</tt> to the standard-outputSyntax stream:</p>
	 *
	 * <blockquote>
	 *
	 * <pre>
	 * BEXPattern p = BEXPattern.compile(&quot;cat&quot;);
	 * BEXMatcher m = p.matcher(&quot;one cat two cats in the yard&quot;);
	 * StringBuffer sb = new StringBuffer();
	 * while (m.find()) {
	 * &nbsp;&nbsp;&nbsp;&nbsp;m.appendReplacement(sb, &quot;dog&quot;);
	 * }
	 * m.appendTail(sb);
	 * System.out.println(sb.toString());
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * @param sb
	 *            The target string buffer
	 *
	 * @param replacement
	 *            The replacement string
	 *
	 * @return This matcher
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the replacement string refers to a capturing
	 *             group that does not exist in the pattern
	 *
	 * @since 0.6
	 */
	public BEXMatcher appendReplacement(final StringBuffer sb, final String replacement) {
		int first = this.start();
		int last = this.end();

		// Append the intervening text
		sb.append(this.text.subSequence(this.lastAppendPosition, first));
		// Append the match substitution
		sb.append(this.getReplacement(replacement));

		this.lastAppendPosition = last;
		return this;
	}

	/**
	 * Implements a terminal append-and-replace step.
	 *
	 * <p>This method reads characters from the input sequence, starting at the
	 * append position, and appends them to the given string buffer. It is
	 * intended to be invoked after one or more invocations of the {@link #appendReplacement appendReplacement} method
	 * in order to copy the
	 * remainder of the input sequence.</p>
	 *
	 * @param sb
	 *            The target string buffer
	 *
	 * @return The target string buffer
	 * @since 0.6
	 */
	public StringBuffer appendTail(final StringBuffer sb) {
		sb.append(this.text, this.lastAppendPosition, this.getTextLength());
		return sb;
	}

	/**
	 * Returns the end index of the text.
	 *
	 * @return the index after the last character in the text
	 */
	private int getTextLength() {
		return this.text.length();
	}

	/**
	 * Returns a literal replacement <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * This method produces a <code>String</code> that will work
	 * as a literal replacement <code>s</code> in the
	 * <code>appendReplacement</code> method of the {@link BEXMatcher} class.
	 * The <code>String</code> produced will match the sequence of characters
	 * in <code>s</code> treated as a literal sequence.
	 *
	 * @param  s The string to be literalized
	 * @return  A literal string replacement
	 * @since 0.6
	 */
	public static String quoteReplacement(final String s) {
		if (!s.endsWith(":") && s.indexOf(":[") == -1) {
			return s;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ':' && (nextChar(s, i) == '[' || !hasNextChar(s, i))) {
				sb.append(":[:]");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
