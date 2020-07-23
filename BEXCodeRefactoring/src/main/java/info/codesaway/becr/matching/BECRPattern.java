package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRMatchingUtilities.hasText;
import static info.codesaway.becr.matching.BECRMatchingUtilities.isWordCharacter;
import static info.codesaway.becr.matching.BECRMatchingUtilities.nextChar;
import static info.codesaway.becr.matching.BECRMatchingUtilities.prevChar;
import static info.codesaway.becr.matching.BECRMatchingUtilities.stringChar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import info.codesaway.util.regex.Pattern;

public class BECRPattern {
	// TODO: expand, but most common will be text [group text]*
	// Initially, support this and add others over time
	// What if starts or ends with group?
	// What if has two groups side by side?

	private final List<Pattern> patterns;
	private final List<String> groups;

	private final int regexPatternFlags;

	private BECRPattern(final List<Pattern> patterns, final List<String> groups, final int regexPatternFlags) {
		if (patterns.isEmpty()) {
			throw new IllegalArgumentException("No patterns specified");
		}

		if (groups.size() != patterns.size() - 1) {
			throw new IllegalArgumentException("Expecting exactly 1 fewer group elements than pattern elements");
		}

		this.patterns = Collections.unmodifiableList(patterns);
		this.groups = Collections.unmodifiableList(groups);

		this.regexPatternFlags = regexPatternFlags;
	}

	private static final String REGEX_BLOCK_START = "@~~";
	private static final String REGEX_BLOCK_END = "~~@";

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

		List<Pattern> patterns = new ArrayList<>();

		// TODO: support optional group matches
		List<String> groups = new ArrayList<>();

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

				regexBuilder.append(pattern.substring(i, end));
				i = end + REGEX_BLOCK_END.length();
			} else if (hasText(pattern, i, "~[~]")) {
				regexBuilder.append("~");
			} else if (c == '~' && nextChar(pattern, i) == '['
					&& (isWordCharacter(nextChar(pattern, i + 1)) || nextChar(pattern, i + 1) == ' ')) {
				int originalStart = i;

				// Start of group
				i += 2;

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

				if (isSpace) {
					// Match horizontal space in text (excludes line terminators)
					regex = "\\h++";
				} else if (hasText(pattern, i, ":w")) {
					regex = "\\w+?";
					i += 2;
				} else if (hasText(pattern, i, ".")) {
					regex = "[\\w.-]+?";
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
						patterns.add(Pattern.compile(regexBuilder.toString(), regexPatternFlags));
						regexBuilder.setLength(0);

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

			} else {
				regexBuilder.append(Pattern.literal(stringChar(pattern, i)));
				i++;
			}
		}

		if (regexBuilder.length() > 0) {
			patterns.add(Pattern.compile(regexBuilder.toString(), regexPatternFlags));
			//			System.out.printf("Rest of regex: %s%s%s%n", REGEX_BLOCK_START, regexBuilder, REGEX_BLOCK_END);
		}

		return new BECRPattern(patterns, groups, regexPatternFlags);
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
}
