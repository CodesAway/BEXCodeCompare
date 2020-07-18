package info.codesaway.bex.diff.substitution;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.BasicDiffType;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.util.lcs.LcsString;

public final class LcsSubstitution implements SubstitutionType {
	private final double threshold;
	private final int lcsMaxLineLength;
	private final IntBinaryOperator operator;

	public LcsSubstitution(final double threshold, final int lcsMaxLineLength, final IntBinaryOperator operator) {
		this.threshold = threshold;
		this.lcsMaxLineLength = lcsMaxLineLength;
		this.operator = operator;
	}

	@Override
	public SubstitutionDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);

		if (normalizedText.testOrBoth(t -> t.length() > this.lcsMaxLineLength)) {
			// Don't run LCS for long lines, since LCS is O(n^2) for memory and could cause a memory exception for too long of lines
			return null;
		}

		int lcsLength = new LcsString(normalizedText.getLeft(), normalizedText.getRight()).lcsLength();

		if (lcsLength == 0) {
			return null;
		}

		int value = this.operator.applyAsInt(normalizedText.getLeft().length(), normalizedText.getRight().length());

		boolean meetsThreshold = lcsLength >= this.threshold * value;
		return meetsThreshold ? BasicDiffType.SUBSTITUTE : null;
	}
}
