package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.NormalizationFunction.NO_NORMALIZATION;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import info.codesaway.bex.BEXPairValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionType;

public final class TestUtilities {
	public static SubstitutionDiffType acceptSubstitutionType(final SubstitutionType substitutionType,
			final String leftText, final String rightText) {
		return acceptSubstitutionType(substitutionType, leftText, rightText, NO_NORMALIZATION);
	}

	public static SubstitutionDiffType acceptSubstitutionType(final SubstitutionType substitutionType,
			final String leftText, final String rightText,
			final NormalizationFunction normalizationFunction) {
		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, null, new DiffLine(1, rightText));

		DiffNormalizedText normalizedText = normalizationFunction.normalize(left.getLeftIndexedText(),
				right.getRightIndexedText());

		Map<DiffEdit, String> map = ImmutableMap.of(left, normalizedText.getLeft(), right,
				normalizedText.getRight());

		return substitutionType.accept(new BEXPairValue<>(left, right), map, normalizationFunction);
	}
}
