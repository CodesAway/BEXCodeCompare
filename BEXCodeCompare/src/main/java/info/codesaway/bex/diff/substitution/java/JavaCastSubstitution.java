package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.identityEquals;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public class JavaCastSubstitution implements JavaSubstitution {
	private static final String TYPE_REGEX = "(?<type>\\w++(?:<\\w++>)?+)";
	private static final ThreadLocal<Matcher> CAST_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"(?J)"
					// common case of local variable
					+ "(?:(?<head>" + TYPE_REGEX + " (?<variable>\\w++) = )\\(\\g{type}\\)"
					// Handle field reference (such as this.field = (Cast) value
					+ "|(?<head>(?:\\w++\\.)*+\\w++ = )\\(" + TYPE_REGEX + "\\)"
					+ ") "
					+ "(?<tail>.*+)"));

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher castMatcher = CAST_MATCHER.get();

		//		System.out.println("CAST? " + castMatcher.reset(normalizedLeft).find() + "\t" + normalizedLeft);

		BEXSide side;
		String expectedText;
		String originalText;
		if (castMatcher.reset(normalizedLeft).find()) {
			side = BEXSide.RIGHT;
			expectedText = normalizedRight;
			originalText = normalizedLeft;
		} else if (castMatcher.reset(normalizedRight).find()) {
			side = BEXSide.LEFT;
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
		//		String refactoredText = castMatcher.getReplacement("${head}\0${tail}");
		String refactoredText = castMatcher.replaceFirst("${head}\0${tail}");

		String type = castMatcher.get("type");

		return !identityEquals(refactoredText, originalText)
				&& equalsWithSpecialHandling(refactoredText, expectedText)
						? new RefactoringDiffTypeValue('R', side, "cast", type, true)
						: null;
	}
}
