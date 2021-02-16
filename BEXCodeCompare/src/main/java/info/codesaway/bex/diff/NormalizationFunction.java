package info.codesaway.bex.diff;

import info.codesaway.bex.Indexed;

/**
 * @since 0.14
 */
public interface NormalizationFunction {
	public DiffNormalizedText normalize(final DiffEdit diffEdit);

	public DiffNormalizedText normalize(final Indexed<String> leftIndexedText, final Indexed<String> rightIndexedText);
}
