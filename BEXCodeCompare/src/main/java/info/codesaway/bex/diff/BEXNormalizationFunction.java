package info.codesaway.bex.diff;

import java.util.function.BiFunction;

import info.codesaway.bex.Indexed;

/**
 *
 * @since 0.14
 */
public final class BEXNormalizationFunction implements NormalizationFunction {
	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;
	private final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction;

	/**
	 * @see NormalizationFunction#NO_NORMALIZATION
	 */

	static class BEXNormalizationFunctionHelper {
		// Used internally to allow initializing public static field in NormalizationFunction interface
		// 9/20/2021 moved to private static class since was running into NullPointerException when running tests
		// (due to Java initialization order)
		static final BEXNormalizationFunction NO_NORMALIZATION = new BEXNormalizationFunction(null, null);
	}

	/**
	 * Creates a new NormalizationFunction
	 *
	 * <p><b>NOTE</b>: at least one of the parameters will be <code>null; the other may be <code>null</code>, to indicate that no normalization will occur</b>
	 * @param normalizationFunction
	 * @param indexedNormalizationFunction
	 */
	BEXNormalizationFunction(final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction) {
		this.normalizationFunction = normalizationFunction;
		this.indexedNormalizationFunction = indexedNormalizationFunction;
	}

	@Override
	public DiffNormalizedText normalize(final Indexed<String> leftIndexedText, final Indexed<String> rightIndexedText) {
		if (this.normalizationFunction != null) {
			return this.normalizationFunction.apply(leftIndexedText.getValue(), rightIndexedText.getValue());
		}

		if (this.indexedNormalizationFunction != null) {
			return this.indexedNormalizationFunction.apply(leftIndexedText, rightIndexedText);
		}

		return new DiffNormalizedText(leftIndexedText.getValue(), rightIndexedText.getValue());
	}
}
