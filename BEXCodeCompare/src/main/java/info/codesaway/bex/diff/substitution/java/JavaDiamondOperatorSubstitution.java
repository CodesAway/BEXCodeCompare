package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public final class JavaDiamondOperatorSubstitution implements JavaSubstitution {
	private static final String TYPE_PART_REGEX = "\\w++(?:\\[\\]|<\\w++>)?+";
	private static final ThreadLocal<Matcher> DIAMOND_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"(?<head>(?:(?<variable>\\w++) = |return\\s++)"
					+ "new (?<class>\\w++)<)"
					+ "(?<type>" + TYPE_PART_REGEX + "(?: , " + TYPE_PART_REGEX + ")*+)"
					+ "(?<tail>>\\()"));

	@Override
	public RefactoringDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);

		Matcher diamondMatcher = DIAMOND_MATCHER.get();

		BEXSide side = normalizedText.testLeftMirror(t -> diamondMatcher.reset(t).find());
		if (side == null) {
			return null;
		}

		// Indicate the refactoring is on the other side
		side = side.other();
		String expectedText = normalizedText.get(side);

		String refactoredText = diamondMatcher.replaceFirst("${head}${tail}");

		String type = diamondMatcher.get("type");

		return expectedText.equals(refactoredText)
				? new RefactoringDiffTypeValue('R', side, "diamond operator", type, true)
				: null;
	}
}
