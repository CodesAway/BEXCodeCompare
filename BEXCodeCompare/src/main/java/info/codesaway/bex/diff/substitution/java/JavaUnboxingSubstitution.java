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

public final class JavaUnboxingSubstitution implements JavaSubstitution {
	private static final ThreadLocal<Matcher> UNBOXING_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			"\\.(?<type>boolean|byte|char|double|float|int|long|short)Value\\(\\)"));

	@Override
	public RefactoringDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);

		Matcher unboxingMatcher = UNBOXING_MATCHER.get();
		BEXSide side = normalizedText.testLeftMirror(t -> unboxingMatcher.reset(t).find());

		if (side == null) {
			return null;
		}

		// Indicate the refactoring is on the other side
		side = side.other();
		String expectedText = normalizedText.get(side);

		String type = unboxingMatcher.get("type");
		String text = unboxingMatcher.replaceAll("");

		return text.equals(expectedText)
				? new RefactoringDiffTypeValue('R', side, "unboxing", type, true)
				: null;
	}
}
