package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRGroupMatchSetting.MATCH_ANGLE_BRACKETS;
import static info.codesaway.becr.matching.BECRGroupMatchSetting.OPTIONAL;
import static info.codesaway.becr.matching.BECRMatchingUtilities.hasText;
import static info.codesaway.becr.matching.BECRMatchingUtilities.isWordCharacter;
import static info.codesaway.becr.matching.BECRMatchingUtilities.nextChar;
import static info.codesaway.becr.matching.BECRMatchingUtilities.prevChar;
import static info.codesaway.becr.matching.BECRMatchingUtilities.stringChar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.codesaway.util.regex.Pattern;

public final class BECRPattern {
	// TODO: expand, but most common will be text [group text]*
	// Initially, support this and add others over time
	// What if starts or ends with group?
	// What if has two groups side by side?

	private final List<Pattern> patterns;
	private final List<String> groups;
	private final Map<Integer, BECRGroupMatchSetting> groupMatchSettings;

	//	private final int regexPatternFlags;

	private BECRPattern(final List<Pattern> patterns, final List<String> groups,
			final Map<Integer, BECRGroupMatchSetting> groupMatchSettings) {
		if (patterns.isEmpty()) {
			throw new IllegalArgumentException("No patterns specified");
		}

		if (groups.size() != patterns.size() - 1) {
			throw new IllegalArgumentException("Expecting exactly 1 fewer group elements than pattern elements");
		}

		this.patterns = Collections.unmodifiableList(patterns);
		this.groups = Collections.unmodifiableList(groups);
		this.groupMatchSettings = Collections.unmodifiableMap(groupMatchSettings);

		//		this.regexPatternFlags = regexPatternFlags;
	}

	private static final String REGEX_BLOCK_START = "@--";
	private static final String REGEX_BLOCK_END = "--!";

	public static BECRPattern compile(final String pattern, final BECRPatternFlag... flags) {
		// Allow duplicate names in capture groups
		// (this way, don't cause error if specify the same group name twice)
		int regexPatternFlags = Pattern.DUPLICATE_NAMES;

		if (flags != null) {
			for (BECRPatternFlag flag : flags) {
				switch (flag) {
				case CASE_INSENSITIVE:
					regexPatternFlags |= Pattern.CASE_INSENSITIVE;
					break;
				case UNICODE:
					regexPatternFlags |= Pattern.UNICODE_CHARACTER_CLASS;
					break;
				default:
					break;
				}
			}
		}

		ArrayList<Pattern> patterns = new ArrayList<>();

		// TODO: support optional group matches
		ArrayList<String> groups = new ArrayList<>();
		Map<Integer, BECRGroupMatchSetting> groupMatchSettings = new HashMap<>();

		StringBuilder regexBuilder = new StringBuilder();

		for (int i = 0; i < pattern.length();) {
			char c = pattern.charAt(i);

			if (hasText(pattern, i, REGEX_BLOCK_START)) {
				int regexBlockStart = i;

				// Start of regex
				i += REGEX_BLOCK_START.length();

				int end = pattern.indexOf(REGEX_BLOCK_END, i);

				if (end == -1) {
					throw new IllegalArgumentException(
							"Regex match does not have valid end: " + pattern.substring(regexBlockStart));
				}

				// Put regex in non-capture group
				// (this way, if ends with "|", represents empty string and doesn't cross into next logic)
				// (also any flags set don't impart rest of matching)
				regexBuilder.append("(?:").append(pattern.substring(i, end)).append(")");
				i = end + REGEX_BLOCK_END.length();
			} else if (hasText(pattern, i, ":[:]")) {
				regexBuilder.append(":");
				i += 4;
			} else if (c == ':' && nextChar(pattern, i) == '['
					&& isNextCharStartOfGroup(pattern, i + 1)) {
				int originalStart = i;

				// Start of group
				i += 2;

				boolean isOptional = pattern.charAt(i) == '?';
				if (isOptional) {
					i++;
				}

				// TODO: add tests to ensure if has dangling characters, then correct exception is thrown
				boolean isSpace = pattern.charAt(i) == ' ';
				if (isSpace) {
					i++;
				}

				int groupNameStart = i;

				char d;

				do {
					if (i >= pattern.length()) {
						break;
					}

					d = pattern.charAt(i++);
				} while (isWordCharacter(d));

				// Last character isn't part of group name
				i--;

				int groupNameEnd = i;

				String regex = null;
				BECRGroupMatchSetting groupMatchSetting = BECRGroupMatchSetting.DEFAULT;

				if (isSpace) {
					// Match horizontal space in text (excludes line terminators)
					regex = isOptional ? "\\h*+" : "\\h++";
				} else if (hasText(pattern, i, ":w")) {
					regex = isOptional ? "\\w*?" : "\\w+?";
					i += 2;
				} else if (hasText(pattern, i, ".")) {
					// TODO: test against comby
					// though, based no my example, this shouldnt' be lazy
					// (should be greedy, but not possessive)
					regex = isOptional ? "[\\w.-]*" : "[\\w.-]+";
					//					regex = isOptional ? "[\\w.-]*?" : "[\\w.-]+?";
					i++;
				} else if (hasText(pattern, i, "<>")) {
					groupMatchSetting = groupMatchSetting.turnOn(MATCH_ANGLE_BRACKETS);
					i += 2;
				} else if (hasText(pattern, i, "*")) {
					// Wildcard to match 0 or more characters (excludes line terminators)
					regex = ".*?";
					i++;
				} else if (hasText(pattern, i, "+")) {
					// Wildcard to match 1 or more characters (excludes line terminators)
					regex = ".+?";
					i++;
				}

				if (hasText(pattern, i, "]")) {
					String groupName = pattern.substring(groupNameStart, groupNameEnd);

					if (regex != null) {
						if (!groupName.isEmpty()) {
							regexBuilder.append("(?<").append(groupName).append(">");
						}

						regexBuilder.append(regex);

						if (!groupName.isEmpty()) {
							regexBuilder.append(")");
						}
					} else {
						// If starts with group, regexBuilder will be empty
						// TODO: this seems to work fine, but should it instead use the pattern "^"?
						// Likely need to test multi-find, but thinking matching space would be preferred
						// This way, if doing multi-find and first fails, could always try again later in the input??
						patterns.add(Pattern.compile(regexBuilder.toString(), regexPatternFlags));
						regexBuilder.setLength(0);

						if (isOptional) {
							groupMatchSetting = groupMatchSetting.turnOn(OPTIONAL);
						}

						if (!groupMatchSetting.isDefault()) {
							groupMatchSettings.put(groups.size(), groupMatchSetting);
						}
						groups.add(groupName);
					}

					i++;
				} else {
					// Not valid group
					// Throw error since likely intended to use group
					// (also, this way, can add more functionality over time without braking existing code)
					throw new IllegalArgumentException("Invalid syntax: " + pattern.substring(originalStart, i + 1));
				}
			} else if (c == ' ') {
				// Handle whitespace
				// A single space represents optional whitespace (except when between two alphanumeric)
				// For example "int x"; in this case, the space should be seen as required
				// Two consecutive spaces represents required whitespace
				// TODO: 4 or more spaces wouldn't match anything since 2 would eat all the spaces
				// TODO: 3 spaces is valid (same as 2), since 3rd space wouldn't match anything (it's useless)
				if (nextChar(pattern, i) == ' ') {
					// 2 space next to each other, so required space
					regexBuilder.append("\\s++");
					i += 2;
				} else if (isWordCharacter(prevChar(pattern, i))
						&& isWordCharacter(nextChar(pattern, i))) {
					// If space is between 2 alphanumeric, then space is required
					regexBuilder.append("\\s++");
					i++;
				} else {
					regexBuilder.append("\\s*+");
					i++;
				}
			} else if (c == '"') {
				// Ensure the double quote isn't escaped
				// (if it is, then don't match it)
				regexBuilder.append("(?<!\\\\)\"");
				i++;
			} else {
				regexBuilder.append(Pattern.literal(stringChar(pattern, i)));
				i++;
			}
		}

		if (regexBuilder.length() > 0) {
			patterns.add(Pattern.compile(regexBuilder.toString(), regexPatternFlags));
			//			System.out.printf("Rest of regex: %s%s%s%n", REGEX_BLOCK_START, regexBuilder, REGEX_BLOCK_END);
		} else {
			// If ends with group, then group will capture rest of input
			patterns.add(Pattern.compile("$"));
		}

		// Reduce memory footprint (likely minimal impact, but mine as well do it)
		patterns.trimToSize();
		groups.trimToSize();

		return new BECRPattern(patterns, groups, groupMatchSettings);
	}

	private static boolean isNextCharStartOfGroup(final String pattern, final int i) {
		char nextChar = nextChar(pattern, i);

		return isWordCharacter(nextChar) || nextChar == ' ' || nextChar == '?';
	}

	public BECRMatcher matcher(final CharSequence text) {
		return new BECRMatcher(this, text);
	}

	List<Pattern> getPatterns() {
		return this.patterns;
	}

	List<String> getGroups() {
		return this.groups;
	}

	Map<Integer, BECRGroupMatchSetting> getGroupMatchSettings() {
		return this.groupMatchSettings;
	}

	//	boolean isOptionalGroup(final int group) {
	//		return this.hasGroupOption(group, BECRGroupOption.OPTIONAL);
	//	}
	//
	//	boolean shouldMatchAngleBrackets(final int group) {
	//		return this.hasGroupOption(group, BECRGroupOption.MATCH_ANGLE_BRACKETS);
	//	}
	//
	//	private boolean hasGroupOption(final int group, final BECRGroupOption option) {
	//		return this.groupOptions
	//				.getOrDefault(option, Collections.emptySet())
	//				.contains(group);
	//	}
}
