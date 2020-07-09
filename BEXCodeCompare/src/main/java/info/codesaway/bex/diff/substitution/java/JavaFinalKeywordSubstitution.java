package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public class JavaFinalKeywordSubstitution implements JavaSubstitution {
	private static final ThreadLocal<Matcher> FINAL_KEYWORD_MATCHER = getThreadLocalMatcher(
			"\\bfinal\\s");

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher matcher = FINAL_KEYWORD_MATCHER.get();

		// Does normal text replace
		// (wouldn't correctly handle if string text or comment contained something that would match the refactoring)
		// However, this would require parsing the source code

		BEXSide side = normalizedLeft.length() > normalizedRight.length() ? BEXSide.LEFT : BEXSide.RIGHT;

		// Use null character to allow special handling near the matches
		// Java text file shouldn't contain null characters
		String leftText = matcher.reset(normalizedLeft).replaceAll("\0");
		String rightText = matcher.reset(normalizedRight).replaceAll("\0");

		//		System.out.println(leftText);
		//		System.out.println(rightText);

		return equalsWithSpecialHandling(leftText, rightText)
				? new RefactoringDiffTypeValue('R', side, "final keyword", null, true)
				: null;
	}
}
