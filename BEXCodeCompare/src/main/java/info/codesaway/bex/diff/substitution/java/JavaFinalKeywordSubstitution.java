package info.codesaway.bex.diff.substitution.java;

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

public final class JavaFinalKeywordSubstitution implements JavaSubstitution {
	// Does normal text replace
	// (wouldn't correctly handle if string text or comment contained something that would match the refactoring)
	// However, this would require parsing the source code
	private static final ThreadLocal<Matcher> FINAL_KEYWORD_MATCHER = getThreadLocalMatcher(
			"\\bfinal\\s");

	@Override
	public RefactoringDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);

		BEXSide side = normalizedText.testLeftRightMirror((l, r) -> l.length() > r.length());

		// Use null character to allow special handling near the matches
		// Java text file shouldn't contain null characters
		BEXPair<String> text = normalizedText.map(t -> FINAL_KEYWORD_MATCHER.get().reset(t).replaceAll("\0"));

		return text.test(JavaSubstitution::equalsWithSpecialHandling)
				? new RefactoringDiffTypeValue('R', side, "final keyword", null, true)
				: null;
	}
}
