package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.util.regex.Matcher;

public final class JavaUnboxing implements JavaSubstitution {
	private static final ThreadLocal<Matcher> UNBOXING_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"\\.(?<type>boolean|byte|char|double|float|int|long|short)Value\\(\\)"));

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher unboxingMatcher = UNBOXING_MATCHER.get();
		BEXSide side;
		String expectedText;
		if (unboxingMatcher.reset(normalizedLeft).find()) {
			side = BEXSide.RIGHT;
			expectedText = normalizedRight;
		} else if (unboxingMatcher.reset(normalizedRight).find()) {
			side = BEXSide.LEFT;
			expectedText = normalizedLeft;
		} else {
			return null;
		}

		String type = unboxingMatcher.get("type");
		String text = unboxingMatcher.replaceAll("");

		return text.equals(expectedText)
				? new RefactoringDiffTypeValue('R', side, "unboxing", type, true)
				: null;
	}
}
