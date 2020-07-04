package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public class JavaDiamondOperatorSubstitution implements JavaSubstitution {
	private static final String TYPE_PART_REGEX = "\\w++(?:\\[\\]|<\\w++>)?+";
	private static final ThreadLocal<Matcher> DIAMOND_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"(?<head>(?:(?<variable>\\w++) = |return\\s++)"
					+ "new (?<class>\\w++)<)"
					+ "(?<type>" + TYPE_PART_REGEX + "(?: , " + TYPE_PART_REGEX + ")*+)"
					+ "(?<tail>>\\()"));

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		//		System.out.println("Java Diamond?");
		//		System.out.println(normalizedLeft);
		//		System.out.println(normalizedRight);

		Matcher diamondMatcher = DIAMOND_MATCHER.get();
		DiffSide side;
		String expectedText;
		if (diamondMatcher.reset(normalizedLeft).find()) {
			side = DiffSide.RIGHT;
			expectedText = normalizedRight;
		} else if (diamondMatcher.reset(normalizedRight).find()) {
			side = DiffSide.LEFT;
			expectedText = normalizedLeft;
		} else {
			return null;
		}

		String refactoredText = diamondMatcher.replaceFirst("${head}${tail}");

		String type = diamondMatcher.get("type");

		return expectedText.equals(refactoredText)
				? new RefactoringDiffTypeValue('R', side, "diamond operator", type, true)
				: null;
	}
}
