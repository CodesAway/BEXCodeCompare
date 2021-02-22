package info.codesaway.bex.diff;

import info.codesaway.bex.Indexed;

public interface DiffAlgorithm {
	// Breaking changes in 0.14
	/**
	 * Gets the normalization function
	 */
	public NormalizationFunction getNormalizationFunction();

	public default boolean isNormalizedEqualText(final Indexed<String> leftIndexedText,
			final Indexed<String> rightIndexedText) {
		return this.normalize(leftIndexedText, rightIndexedText).hasEqualText();
	}

	public default DiffNormalizedText normalize(final Indexed<String> leftIndexedText,
			final Indexed<String> rightIndexedText) {
		return DiffHelper.normalize(leftIndexedText, rightIndexedText, this.getNormalizationFunction());
	}
}
