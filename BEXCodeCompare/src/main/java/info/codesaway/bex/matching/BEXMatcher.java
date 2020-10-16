package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXGroupMatchSetting.DEFAULT;
import static info.codesaway.bex.matching.BEXGroupMatchSetting.STOP_WHEN_VALID;
import static info.codesaway.bex.matching.BEXMatchingStateOption.MISMATCHED_DELIMITERS;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasNextChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.hasText;
import static info.codesaway.bex.matching.BEXMatchingUtilities.isWordCharacter;
import static info.codesaway.bex.matching.BEXMatchingUtilities.nextChar;
import static info.codesaway.bex.matching.BEXMatchingUtilities.parseJavaTextStates;
import static info.codesaway.bex.util.BEXUtilities.entry;
import static info.codesaway.bex.util.BEXUtilities.getSubstring;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import info.codesaway.bex.AbstractImmutableSet;
import info.codesaway.bex.BEXPair;
import info.codesaway.bex.ImmutableIntRangeMap;
import info.codesaway.bex.IntBEXRange;
import info.codesaway.bex.IntRange;
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

	// TODO: should I add a public getter for language?
	private MatchingLanguage language;

	/**
	 * Map from range to text state
	 */
	private ImmutableIntRangeMap<MatchingStateOption> textStateMap;

	/**
	 * Offset used when resolving indexes in textStateMap (allows sharing textStateMap such as in BEXString)
	 */
	private int offset;

	private final MutableIntBEXPair matchRange = new MutableIntBEXPair(-1, 0);

	/**
	 * Most groups will have only one value, so stored here
	 */
	// In 0.9 changed to be LinkedHashMap, so keep the group order
	private final Map<String, IntBEXRange> valuesMap = new LinkedHashMap<>();

	/**
	 * Groups with multiple values are stored here
	 */
	// Initialize with emptyMap, since most of the time won't be used
	// (reduce memory footprint of Matcher)
	// If it ends up being used, then it will be initialized with an empty map and entries will be added
	// (only used if specify pattern that contains regex which has multiple names with the same group name
	private Map<String, List<IntBEXRange>> multipleValuesMap = Collections.emptyMap();

	BEXMatcher(final BEXPattern parent, final CharSequence text) {
		this(parent, text, BEXMatchingLanguage.JAVA, parseJavaTextStates(text), 0);
	}

	BEXMatcher(final BEXPattern parent, final CharSequence text, final MatchingLanguage language,
			final ImmutableIntRangeMap<MatchingStateOption> textStateMap, final int offset) {
		this.parentPattern = parent;
		this.text = text;
		this.language = language;
		this.textStateMap = textStateMap;
		this.offset = offset;
	}

	@Override
	public BEXPattern pattern() {
		return this.parentPattern;
	}

	/**
	 * Returns the match state of this matcher as a {@link BEXMatchResult}.
	 * The result is unaffected by subsequent operations performed upon this
	 * matcher.
	 *
	 * @return  a <code>BEXMatchResult</code> with the state of this matcher
	 * @since 0.8
	 */
	public BEXMatchResult toMatchResult() {
		BEXMatcher result = new BEXMatcher(this.pattern(), this.text(), this.language, this.textStateMap, this.offset);
		result.matchRange.set(this.matchRange);
		result.valuesMap.putAll(this.valuesMap);

		if (!this.multipleValuesMap.isEmpty()) {
			result.multipleValuesMap = this.multipleValuesMap.entrySet()
					.stream()
					.collect(Collectors.toMap(Entry::getKey, e -> new ArrayList<>(e.getValue())));
		}

		return result;
	}

	@Override
	public String text() {
		return this.text.toString();
	}

	private void clearGroups() {
		this.valuesMap.clear();
		this.multipleValuesMap.clear();
	}

	/**
	 * Attempts to find the next subsequence of the input sequence that matches
	 * the pattern.
	 *
	 * <p>This method starts at the beginning of this matcher's region, or, if a
	 * previous invocation of the method was successful and the matcher has not
	 * since been reset, at the first character not matched by the previous
	 * match.</p>
	 *
	 * <p>If the match succeeds then more information can be obtained via the
	 * <code>start</code>, <code>end</code>, <code>range</code>, and <code>group</code> methods.</p>
	 *
	 * @return <code>true</code> if, and only if, a subsequence of the input
	 *         sequence matches this matcher's pattern
	 */
	public boolean find() {
		// Logic from regex Matcher.find
		int nextSearchStart = this.matchRange.getRight();
		if (nextSearchStart == this.matchRange.getLeft()) {
			nextSearchStart++;
		}
		// If next search starts beyond region then it fails
		//        if (nextSearchStart > to) {
		if (nextSearchStart > this.getTextLength()) {
			this.clearGroups();
			return false;
		}

		return this.search(nextSearchStart);
	}

	private boolean search(final int from) {
		this.clearGroups();

		boolean foundMatch = this.match(from);
		if (!foundMatch) {
			this.matchRange.setLeft(-1);
		}

		return foundMatch;
	}

	private boolean match(final int from) {
		List<Pattern> patterns = this.parentPattern.getPatterns();

		if (patterns.isEmpty()) {
			return false;
		}

		//		// TODO: cache matchers?
		//		Matcher currentMatcher = patterns.get(0).matcher(this.text);
		//		// TODO: specify as option? (needed to handle spaces after group)
		//		currentMatcher.useTransparentBounds(true);
		//
		//		boolean foundMatch;
		//		int searchFrom = from;
		//		do {
		//			if (!currentMatcher.find(searchFrom)) {
		//				if (DEBUG) {
		//					System.out.println("Couldn't find match 0: " + from + "\t" + this.text());
		//					System.out.println("Pattern 0: @" + currentMatcher.pattern() + "@");
		//				}
		//				return false;
		//			}
		//
		//			int start = currentMatcher.start();
		//			int startWithOffset = start + this.offset;
		//			Entry<IntRange, MatchingStateOption> entry = this.textStateMap.getEntry(startWithOffset);
		//
		//			// XXX: think this is what prevents test from passing that first match is in String literal
		//			// TODO: see about refactoring to use for loop to handle first match no differently than other matches
		//			// (only difference is wouldn't have an actual group, so this would just pass)
		//			if (entry != null && startWithOffset != entry.getKey().getStart()
		//					&& !entry.getValue().isCode()) {
		//				// Don't count as match, since part of string literal or comment
		//				// If match starts with the block, then okay to match
		//				// TODO: when else would it be okay to match?
		//				foundMatch = false;
		//				searchFrom = start + 1;
		//			} else {
		//				foundMatch = true;
		//				if (DEBUG) {
		//					if (start == startWithOffset) {
		//						System.out.printf("Found match! %d\t|%s|%n", start, currentMatcher.group());
		//					} else {
		//						System.out.printf("Found match! %d (%d)%n", start, startWithOffset);
		//					}
		//					System.out.println("Text states: " + this.textStateMap);
		//				}
		//			}
		//		} while (!foundMatch);
		//
		//		// Don't match if in string literal or comment
		//		// TODO: under what scenarios should it match stuff in comments?
		//
		//		// TODO: need to process code before match to detect if in block comment, line comment, or String literal
		//
		//		this.putCaptureGroups(currentMatcher);
		//		int regionStart = currentMatcher.end();

		int regionStart = from;
		// TODO: keep track of matchStart (such as if requires multiple passes to find next match)

		int firstMatchStart = -1;

		outer: for (int i = 0; i < patterns.size(); i++) {
			//		for (int i = 0; i < patterns.size() - 1; i++) {
			Pattern pattern = patterns.get(i);
			//			Pattern nextPattern = patterns.get(i + 1);
			// TODO: can we reuse the matcher and just change out the pattern?
			Matcher matcher = pattern.matcher(this.text);
			//			Matcher nextMatcher = nextPattern.matcher(this.text);

			if (DEBUG) {
				System.out.println("Trying matcher " + i);
				//				System.out.println("Trying matcher " + (i + 1));
				System.out.println("Region start: " + regionStart);
			}

			BEXGroupMatchSetting groupMatchSetting;

			if (i == 0) {
				// No group match (treat as "optional", since won't match anything)
				groupMatchSetting = DEFAULT.turnOn(BEXGroupMatchSetting.OPTIONAL);
			} else {
				groupMatchSetting = this.parentPattern.getGroupMatchSettings().getOrDefault(i - 1, DEFAULT);
				//				groupMatchSetting = this.parentPattern.getGroupMatchSettings().getOrDefault(i, DEFAULT);
			}

			// If the group isn't optional, start searching with next character
			// (since group must match something, so match would be at least 1 character)
			int matcherRegionStart = !groupMatchSetting.isOptional() && regionStart < this.getTextLength() - 1
					//			int matcherRegionStart = !groupMatchSetting.isOptional()
					? regionStart + 1
					: regionStart;

			//			if (matcherRegionStart > this.getTextLength()) {
			//				if (DEBUG) {
			//					System.out.println(
			//							"Non-optional group, cannot find starting with next character since no more characters");
			//				}
			//				return false;
			//			}

			matcher.region(matcherRegionStart, this.text.length());
			matcher.useTransparentBounds(true);
			//			nextMatcher.region(matcherRegionStart, this.text.length());
			//			nextMatcher.useTransparentBounds(true);

			if (!matcher.find()) {
				//			if (!nextMatcher.find()) {
				if (DEBUG) {
					System.out.printf("Didn't match %d%n", i);
					//					System.out.printf("Didn't match next matcher %d%n", i + 1);
					System.out.println("Pattern: " + pattern);
					//					System.out.println("Pattern: " + nextPattern);
					System.out.println("Text: " + this.text.subSequence(regionStart, this.text.length()));
				}
				return false;
			}

			if (DEBUG) {
				System.out.printf("Matched %d %s\t|%s|%n", i, matcher.pattern(), matcher.group());
				//				System.out.printf("Matched next match %d %s\t|%s|%n", i + 1, nextMatcher.pattern(),
				//						nextMatcher.group());
			}

			int start = regionStart;
			int end = matcher.start();
			//			int end = nextMatcher.start();

			// Handle first match (since has no group)
			if (i == 0) {
				// TODO: check that state is valid
				int startWithOffset = matcher.start() + this.offset;
				Entry<IntRange, MatchingStateOption> entry = this.textStateMap.getEntry(startWithOffset);

				boolean isValid;
				if (entry != null && startWithOffset != entry.getKey().getStart()
						&& !entry.getValue().isCode()) {
					// Don't count as match, since part of string literal or comment
					// If match starts with the block, then okay to match
					// TODO: when else would it be okay to match?
					isValid = false;
					if (DEBUG) {
						System.out.println("Invalid entry, try first match again: " + entry);
					}
				} else {
					isValid = true;
				}

				if (!isValid) {
					i--;
					// TODO: should this instead be matcher.end()?
					// (start + 1 would be trying the next character, just like regex would do)
					regionStart = matcher.start() + 1;
					continue;
				}

				this.putCaptureGroups(matcher);
				firstMatchStart = matcher.start();
				regionStart = matcher.end();
				continue;
			}

			int startWithOffset = start + this.offset;
			Entry<IntRange, MatchingStateOption> entry = this.textStateMap.getEntry(startWithOffset);

			BEXMatchingState initialState;
			if (entry != null && startWithOffset != entry.getKey().getStart()
					&& !entry.getValue().isCode()) {
				initialState = new BEXMatchingState(-1, Collections.emptyList(), entry.getValue());
			} else {
				initialState = BEXMatchingState.DEFAULT;
			}

			if (DEBUG) {
				System.out.println("Performing search with initialState " + initialState);
			}
			BEXMatchingState state = this.search(start, end, groupMatchSetting, initialState);

			while (!state.isValid(end, initialState.getOptions())) {
				// If has mismatched delimiters, start over and try to find after this?
				// This way, if one line in a file isn't valid, could still handle other lines (versus never matching ever)
				if (state.hasMismatchedDelimiters()) {
					if (DEBUG) {
						System.out.println("Mismatched delimiters: " + state);
					}

					// Start outer from first pattern and try again
					// TODO: when should we not start at first pattern?
					i = -1;
					this.clearGroups();

					// TODO: is this the correct place to start the next try?
					// (seems likely, since this is right after the invalid delimiter)
					// (state's position was tweaked for Mismatch to be right after the delimiter)
					regionStart = state.getPosition();

					continue outer;
				}

				// TODO: handle what if not valid (in this case, expand group)
				if (DEBUG) {
					System.out.printf("Not valid group value: @%s@%n", this.text.subSequence(start, end));
					System.out.println("State: " + state);
				}

				BEXMatchingState validState = this.search(state.getPosition(), this.text.length(),
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

				matcher.region(position, this.text.length());
				//				nextMatcher.region(position, this.text.length());
				// TODO: specify as option? (needed to handle spaces after group)
				matcher.useTransparentBounds(true);
				//				nextMatcher.useTransparentBounds(true);

				if (!matcher.find()) {
					//				if (!nextMatcher.find()) {
					// State is valid and cannot find another match
					// In this case, skip and try again from beginning at later point?
					if (DEBUG) {
						System.out.println("Cannot find match: " + i);
						//						System.out.println("Cannot find next match: " + (i + 1));
					}
					return false;
				}

				end = matcher.start();
				//				end = nextMatcher.start();

				if (end != position) {
					// TODO: there may be extra stuff between the valid position and the next start
					// (if this is also valid, it would be okay)
					if (DEBUG) {
						System.out.printf("New scenario %d: %d\t%d\t%s%n", i, matcher.start(), position,
								this.text.subSequence(position, matcher.start()));
						//						System.out.printf("New scenario %d: %d\t%d\t%s%n", i + 1, nextMatcher.start(), position,
						//								this.text.subSequence(position, nextMatcher.start()));

						System.out.printf("Position does not match matcher start: %d != %d%n", position,
								matcher.start());
						//						System.out.printf("Position does not match next matcher start: %d != %d%n", position,
						//								nextMatcher.start());
					}
					//					return false;
					// TODO: what should I pass for initial state
					state = this.search(position, end, groupMatchSetting);
				} else {
					break;
				}
			}

			if (start == end && !groupMatchSetting.isOptional()) {
				// TODO: check if expanding group would allow to match
				if (DEBUG) {
					System.out.println("Empty group, yet not optional");
				}
				return false;
			}

			String group = this.parentPattern.getGroups().get(i - 1);
			//			String group = this.parentPattern.getGroups().get(i);
			//			String value = this.text.subSequence(start, end).toString();

			if (DEBUG) {
				System.out.printf("Matcher %d has group name %s%n", i + 1, group);
			}

			// If group is already specified, the values must match
			// (unless it's an unnamed group)
			if (!group.equals("_")) {
				// TODO: what if group was matched as part of regex, does normal group have to match?
				IntBEXRange range = this.getInternalNoMatchCheck(group);

				// If group is already specified
				if (range != null && range != NOT_FOUND) {
					// Verify the content equals; otherwise, don't match
					// TODO: should go to next match or something... need to implement
					int oldLength = range.length();
					int newLength = end - start;

					// Fast check, based on length
					if (newLength != oldLength) {
						return false;
					}

					// Compare character by character
					int index1 = range.getStart();
					int index2 = start;

					for (int m = 0; m < oldLength; m++) {
						char c1 = this.text.charAt(index1++);
						char c2 = this.text.charAt(index2++);

						if (c1 != c2) {
							return false;
						}
					}
				} else {
					this.put(group, IntBEXRange.of(start, end));
				}
			}

			this.putCaptureGroups(matcher);

			//			System.out.printf("%s: @%s@%n", group, value);
			regionStart = matcher.end();
		}

		int matchStart = firstMatchStart;
		//		int matchStart = currentMatcher.start();
		int matchEnd = regionStart;

		this.matchRange.set(matchStart, matchEnd);

		if (DEBUG) {
			System.out.println("Found match: " + this.matchRange);
		}

		return true;
	}

	private BEXMatchingState search(final int start, final int end,
			final BEXGroupMatchSetting groupMatchSetting) {
		return this.search(start, end, groupMatchSetting, null);
	}

	private BEXMatchingState search(final int start, final int end,
			final BEXGroupMatchSetting groupMatchSetting, final BEXMatchingState state) {
		// Verify parentheses / delimiters are balanced
		// TODO: handle string (what if group is in String??)
		// TODO: handle comments (what if group is in comments??)

		boolean shouldStopWhenValid = groupMatchSetting.shouldStopWhenValid();

		if (DEBUG) {
			if (shouldStopWhenValid) {
				System.out.println("Should stop when valid!");
			}
		}

		// By default, don't include angled brackets <> as part of balancing (unless specified)
		Set<MatchingLanguageSetting> settings;
		if (groupMatchSetting.shouldMatchAngleBrackets()) {
			settings = new HashSet<>();
			settings.add(MatchingLanguageOption.MATCH_ANGLE_BRACKETS);
		} else {
			settings = Collections.emptySet();
		}

		//		String bracketStarts;
		//		String bracketEnds;
		//
		//		if (groupMatchSetting.shouldMatchAngleBrackets()) {
		//			bracketStarts = "([{<";
		//			bracketEnds = ")]}>";
		//		} else {
		//			bracketStarts = "([{";
		//			bracketEnds = ")]}";
		//		}

		ArrayDeque<BEXPair<String>> delimiters = new ArrayDeque<>();
		//		StringBuilder brackets = new StringBuilder();
		// TODO: how to handle multiple levels? Currently, only get top most level
		// For example, JSP expression within String literal gets JSP expression, but doesn't know about String literal outside

		MatchingStateOption stateOption = null;

		if (state != null) {
			delimiters.addAll(state.getDelimiters());
			//			brackets.append(state.getBrackets());
			stateOption = state.getOptions().stream().findFirst().orElse(null);
		}

		for (int i = start; i < end; i++) {
			int indexWithOffset = i + this.offset;
			Entry<IntRange, MatchingStateOption> entry = this.textStateMap.getEntry(indexWithOffset);

			if (entry != null && !entry.getValue().isCode()) {
				// Has a state option
				IntRange canonical = entry.getKey().canonical();
				boolean isEndOfRange = indexWithOffset == canonical.getEnd() - 1;

				stateOption = isEndOfRange ? null : entry.getValue();

				if (shouldStopWhenValid && delimiters.isEmpty() && isEndOfRange) {
					// TODO: need to write unit test to verify this should be plus 1
					return new BEXMatchingState(i + 1, Collections.emptyList());
				}
			} else {
				// XXX: pass to MatchingLanguage to check for delimiter
				Optional<BEXPair<String>> startDelimiter = this.language.findStartDelimiter(this.text, i, settings);

				if (startDelimiter.isPresent()) {
					delimiters.add(startDelimiter.get());
					i += startDelimiter.get().getLeft().length() - 1;
				} else {
					MatchingDelimiterState delimiterState = this.language.findEndDelimiter(delimiters.peekLast(),
							this.text, i, settings);
					MatchingDelimiterResult result = delimiterState.getResult();

					if (result == MatchingDelimiterResult.MISMATCHED) {
						// XXX: should position be after the mismatch - so can try again at this point
						return new BEXMatchingState(i + delimiterState.getDelimiter().length(), delimiters,
								MISMATCHED_DELIMITERS);
					} else if (result == MatchingDelimiterResult.FOUND) {
						// Remove last delimiter
						delimiters.removeLast();

						if (shouldStopWhenValid && delimiters.isEmpty()) {
							// TODO: should this be plus 1 or plus the length of the delimiter?
							return new BEXMatchingState(i + 1, Collections.emptyList());
						}
					}
				}

				//				char c = this.text.charAt(i);
				//				if (bracketStarts.indexOf(c) != -1) {
				//					brackets.append(c);
				//				} else {
				//					int bracketEndIndex = bracketEnds.indexOf(c);
				//
				//					if (bracketEndIndex != -1) {
				//						char bracketStart = bracketStarts.charAt(bracketEndIndex);
				//						if (lastChar(brackets) != bracketStart) {
				//							return new BEXMatchingState(i, delimiters, MISMATCHED_DELIMITERS);
				//						} else {
				//							// Remove last delimiter
				//							delimiters.removeLast();
				//							//							brackets.setLength(brackets.length() - 1);
				//
				//							if (shouldStopWhenValid && delimiters.isEmpty()) {
				//								return new BEXMatchingState(i + 1, Collections.emptyList());
				//							}
				//						}
				//					}
				//				}
			}
		}

		return new BEXMatchingState(end, delimiters, stateOption);
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
		if (DEBUG) {
			System.out.printf("Put group %s with value %s%n", group, value);
		}

		List<IntBEXRange> existingValues = this.multipleValuesMap.get(group);

		if (existingValues != null) {
			// Already has existing values, so add to the collection
			existingValues.add(value);
			return;
		}

		IntBEXRange existingValue = this.valuesMap.get(group);

		if (existingValue == null) {
			// First time seeing the specified group
			this.valuesMap.put(group, value);
		} else {
			// Keep values in singleValues, so can iterate over the groups in order and have correct group counts
			// (put value of null as a placeholder to indicate that the value should come from multipleValues)
			//			this.singleValues.remove(group);
			this.valuesMap.put(group, null);

			// Put existing value and new value into a collection in multipleValues
			List<IntBEXRange> newValues = new ArrayList<>();
			newValues.add(existingValue);
			newValues.add(value);

			if (this.multipleValuesMap.isEmpty()) {
				this.multipleValuesMap = new HashMap<>();
			}

			this.multipleValuesMap.put(group, newValues);
		}
	}

	@Override
	public IntBEXRange range() {
		this.checkForMatch();

		return IntBEXRange.of(this.matchRange.getLeft(), this.matchRange.getRight());
	}

	@Override
	public IntBEXRange range(final String group) {
		IntBEXRange range = this.getInternal(group);

		// Intentionally using identity equals
		if (range == NOT_FOUND) {
			throw new IllegalArgumentException("The specified group is not in the pattern: " + group);
		}

		return range;
	}

	private static IntBEXRange NOT_FOUND = IntBEXRange.of(Integer.MIN_VALUE, Integer.MIN_VALUE);
	private static IntBEXRange NULL_PAIR = IntBEXRange.of(-1, -1);

	private IntBEXRange getInternal(final String group) {
		this.checkForMatch();
		return this.getInternalNoMatchCheck(group);
	}

	private IntBEXRange getInternalNoMatchCheck(final String group) {
		List<IntBEXRange> existingValues = this.multipleValuesMap.get(group);

		if (existingValues != null) {
			// Return first non-null value or return null if all values are null
			return existingValues.stream()
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(NULL_PAIR);
		}

		return this.valuesMap.getOrDefault(group, NOT_FOUND);
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
		this.matchRange.set(-1, 0);
		this.clearGroups();
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
		this.language = BEXMatchingLanguage.JAVA;
		this.textStateMap = parseJavaTextStates(input);
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
		this.language = bexString.getLanguage();
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
	 * <p>Given the pattern <code>dog</code>, the input
	 * <code>"zzzdogzzzdogzzz"</code>, and the replacement string <code>"cat"</code>, an
	 * invocation of this method on a matcher for that expression would yield
	 * the string <code>"zzzcatzzzdogzzz"</code>.</p>
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

					cursor++;
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
	 * character at index {@link #start()}&nbsp;<code>-</code>&nbsp;<code>1</code>.</p>
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
	 * following code, for example, writes <code>one dog two dogs in the
	 * yard</code> to the standard-outputSyntax stream:</p>
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
	 * The group / value entries that make up the current match
	 * @return the group / value entries, in the order they appear in the pattern
	 * @since 0.9
	 */
	@Override
	public Set<Entry<String, String>> entrySet() {
		this.checkForMatch();
		return new EntrySet();
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
	 * Check for match
	 *
	 * @throws  IllegalStateException
	 *          If no match has yet been attempted,
	 *          or if the previous match operation failed
	 */
	private void checkForMatch() {
		if (this.matchRange.getLeft() < 0) {
			throw new IllegalStateException("No match available");
		}
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

	/**
	 *
	 * since 0.9
	 */
	private final class EntrySet extends AbstractImmutableSet<Entry<String, String>> {
		@Override
		public int size() {
			return BEXMatcher.this.valuesMap.size();
		}

		@Override
		public boolean contains(final Object o) {
			if (o instanceof Entry) {
				Entry<?, ?> entry = (Entry<?, ?>) o;

				if (entry.getKey() instanceof String) {
					String value = BEXMatcher.this.get((String) entry.getKey());
					return Objects.equals(value, entry.getValue());
				}
			}

			return false;
		}

		@Override
		public Iterator<Entry<String, String>> iterator() {
			Iterator<Entry<String, IntBEXRange>> groups = BEXMatcher.this.valuesMap.entrySet().iterator();

			return new Iterator<Entry<String, String>>() {
				@Override
				public boolean hasNext() {
					return groups.hasNext();
				}

				@Override
				public Entry<String, String> next() {
					if (!groups.hasNext()) {
						throw new NoSuchElementException();
					}

					Entry<String, IntBEXRange> group = groups.next();
					String groupName = group.getKey();
					String groupValue = group.getValue() != null
							? getSubstring(BEXMatcher.this.text(), group.getValue())
							: BEXMatcher.this.get(groupName);

					return entry(groupName, groupValue);
				}
			};
		}
	}
}
