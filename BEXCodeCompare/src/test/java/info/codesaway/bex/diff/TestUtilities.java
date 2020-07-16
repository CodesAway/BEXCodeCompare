package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableMap;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionType;

public class TestUtilities {
	public static SubstitutionDiffType acceptSubstitutionType(final SubstitutionType substitutionType,
			final String leftText, final String rightText) {
		return acceptSubstitutionType(substitutionType, leftText, rightText, DiffHelper.NO_NORMALIZATION_FUNCTION);
	}

	public static SubstitutionDiffType acceptSubstitutionType(final SubstitutionType substitutionType,
			final String leftText, final String rightText,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, null, new DiffLine(1, rightText));

		DiffNormalizedText normalizedText = DiffHelper.normalize(leftText, rightText, normalizationFunction);

		Map<DiffEdit, String> map = ImmutableMap.of(left, normalizedText.getLeft(), right,
				normalizedText.getRight());

		return substitutionType.accept(new BEXPair<>(left, right), map, normalizationFunction);
	}
}
