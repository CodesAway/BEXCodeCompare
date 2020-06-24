package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.identityEquals;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public class JavaCastSubstitution implements JavaSubstitution {
	private static final ThreadLocal<Matcher> CAST_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"^(?<head>(?<type>\\w++)\\s++(?<variable>\\w++) = )\\(\\g{type}\\) (?<tail>.*+)"));

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher castMatcher = CAST_MATCHER.get();
		DiffSide side;
		String expectedText;
		String originalText;
		if (castMatcher.reset(normalizedLeft).find()) {
			side = DiffSide.RIGHT;
			expectedText = normalizedRight;
			originalText = normalizedLeft;
		} else if (castMatcher.reset(normalizedRight).find()) {
			side = DiffSide.LEFT;
			expectedText = normalizedLeft;
			originalText = normalizedRight;
		} else {
			return null;
		}

		// Does normal text replace
		// (wouldn't correctly handle if string text or comment contained something that would match the refactoring)
		// However, this would require parsing the source code

		// Use null character to allow special handling near the matches
		// Java text file shouldn't contain null characters
		String refactoredText = castMatcher.getReplacement("${head}\0${tail}");

		String type = castMatcher.get("type");

		return !identityEquals(refactoredText, originalText)
				&& equalsWithSpecialHandling(refactoredText, expectedText)
						? new RefactoringDiffTypeValue('R', side, "cast", type, true)
						: null;
	}
}
