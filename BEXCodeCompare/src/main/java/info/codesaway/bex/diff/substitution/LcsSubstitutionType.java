package info.codesaway.bex.diff.substitution;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;

import info.codesaway.bex.diff.BasicDiffType;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.util.lcs.LcsString;

public class LcsSubstitutionType implements SubstitutionType {
	private final double threshold;
	private final int lcsMaxLineLength;
	private final IntBinaryOperator operator;

	public LcsSubstitutionType(final double threshold, final int lcsMaxLineLength, final IntBinaryOperator operator) {
		this.threshold = threshold;
		this.lcsMaxLineLength = lcsMaxLineLength;
		this.operator = operator;
	}

	@Override
	public SubstitutionDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		if (normalizedLeft.length() > this.lcsMaxLineLength
				|| normalizedRight.length() > this.lcsMaxLineLength) {
			// Don't run LCS for long lines, since LCS is O(n^2) for memory and could cause a memory exception for too long of lines
			return null;
		}

		int lcsLength = new LcsString(normalizedLeft, normalizedRight).lcsLength();

		if (lcsLength == 0) {
			return null;
		}

		int value = this.operator.applyAsInt(normalizedLeft.length(), normalizedRight.length());

		boolean meetsThreshold = lcsLength >= this.threshold * value;
		return meetsThreshold ? BasicDiffType.SUBSTITUTE : null;
	}
}
