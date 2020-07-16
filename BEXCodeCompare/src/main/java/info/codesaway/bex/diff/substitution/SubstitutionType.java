package info.codesaway.bex.diff.substitution;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;

@FunctionalInterface
public interface SubstitutionType {
	/**
	 * Determines if the delete / insert pair is a substitution
	 *
	 * @param checkPair the pair of DiffEdit to check
	 * @param normalizedTexts
	 * @param normalizationFunction
	 * @return the SubstitutionDiffType or <code>null</code> indicating the delete / insert pair is not a valid substitution
	 */
	SubstitutionDiffType accept(final BEXPair<DiffEdit> checkPair, final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction);

	public static final SubstitutionContainsSubstitutionType SUBSTITUTION_CONTAINS = new SubstitutionContainsSubstitutionType();
	public static final LcsSubstitution LCS_MIN_OPERATOR = new LcsSubstitution(0.66, 150, Math::min);
	public static final LcsSubstitution LCS_MAX_OPERATOR = new LcsSubstitution(0.66, 150, Math::max);
}
