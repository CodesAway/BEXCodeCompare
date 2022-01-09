package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION;

import java.util.function.BiFunction;

import info.codesaway.bex.Indexed;
import info.codesaway.bex.diff.BEXNormalizationFunction.BEXNormalizationFunctionHelper;

/**
 * @since 0.14
 */
public interface NormalizationFunction {
	/**
	 * NormalizationFunction which takes text and does not normalize
	 *
	 * <p>Can be used for normalization function when want to compare text without normalizing it first</p>
	 */
	// Note: intentionally made different name than DiffHelper.NO_NORMALIZATON_FUNCTION
	// (this allows having Eclipse favorites for DiffHelper and NormalizationFunction
	// (and static imports from both without fields having the same name)
	public static final NormalizationFunction NO_NORMALIZATION = normalization(null);

	public static final NormalizationFunction WHITESPACE_NORMALIZATION = normalization(
			WHITESPACE_NORMALIZATION_FUNCTION);

	public DiffNormalizedText normalize(final Indexed<String> leftIndexedText, final Indexed<String> rightIndexedText);

	public default DiffNormalizedText normalize(final DiffEdit diffEdit) {
		return this.normalize(diffEdit.getLeftIndexedText(), diffEdit.getRightIndexedText());
	}

	// TODO: could cache using ConcurrentHashMap to reduce number of instances created (not sure if it matters)

	public static NormalizationFunction normalization(
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return normalizationFunction == null
				? BEXNormalizationFunctionHelper.NO_NORMALIZATION
				: new BEXNormalizationFunction(normalizationFunction, null);
	}

	public static NormalizationFunction indexedNormalization(
			final BiFunction<Indexed<String>, Indexed<String>, DiffNormalizedText> indexedNormalizationFunction) {
		return indexedNormalizationFunction == null
				? BEXNormalizationFunctionHelper.NO_NORMALIZATION
				: new BEXNormalizationFunction(null, indexedNormalizationFunction);
	}
}
