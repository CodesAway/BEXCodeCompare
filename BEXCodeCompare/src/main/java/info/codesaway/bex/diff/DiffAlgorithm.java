package info.codesaway.bex.diff;

import java.util.function.BiFunction;

public interface DiffAlgorithm {
	/**
	 * Gets the normalization function
	 */
	public BiFunction<String, String, DiffNormalizedText> getNormalizationFunction();

	public default DiffNormalizedText normalize(final String leftText, final String rightText) {
		return DiffHelper.normalize(leftText, rightText, this.getNormalizationFunction());
	}
}
