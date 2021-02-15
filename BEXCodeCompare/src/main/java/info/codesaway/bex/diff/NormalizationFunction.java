package info.codesaway.bex.diff;

import java.util.function.BiFunction;

import info.codesaway.bex.Indexed;

/**
 *
 * @since 0.14
 */
public final class NormalizationFunction {
	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;
	private final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction;

	public static final NormalizationFunction NO_NORMALIZATION_FUNCTION = new NormalizationFunction(null, null);

	/**
	 * Creates a new NormalizationFunction
	 *
	 * <p><b>NOTE</b>: at least one of the parameters will be <code>null; the other may be <code>null</code>, to indicate that no normalization will occur</b>
	 * @param normalizationFunction
	 * @param indexedNormalizationFunction
	 */
	private NormalizationFunction(final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction) {
		this.normalizationFunction = normalizationFunction;
		this.indexedNormalizationFunction = indexedNormalizationFunction;
	}

	public DiffNormalizedText normalize(final DiffEdit diffEdit) {
		if (this.normalizationFunction != null) {
			return this.normalizationFunction.apply(diffEdit.getLeftText(), diffEdit.getRightText());
		}

		if (this.indexedNormalizationFunction != null) {
			return this.indexedNormalizationFunction.apply(diffEdit.getLeftIndexedText(),
					diffEdit.getRightIndexedText());
		}

		return new DiffNormalizedText(diffEdit.getLeftText(), diffEdit.getRightText());
	}

	public DiffNormalizedText normalize(final Indexed<String> leftIndexedText, final Indexed<String> rightIndexedText) {
		if (this.normalizationFunction != null) {
			return this.normalizationFunction.apply(leftIndexedText.getValue(), rightIndexedText.getValue());
		}

		if (this.indexedNormalizationFunction != null) {
			return this.indexedNormalizationFunction.apply(leftIndexedText, rightIndexedText);
		}

		return new DiffNormalizedText(leftIndexedText.getValue(), rightIndexedText.getValue());
	}

	// TODO: could cache using ConcurrentHashMap to reduce number of instances created (not sure if it matters)

	public static NormalizationFunction normalization(
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return new NormalizationFunction(normalizationFunction, null);
	}

	public static NormalizationFunction indexedNormalization(
			final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction) {
		return new NormalizationFunction(null, indexedNormalizationFunction);
	}
}
