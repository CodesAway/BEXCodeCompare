package info.codesaway.bex.diff.substitution;

import java.util.Map;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.NormalizationFunction;

public interface RefactoringType extends SubstitutionType {
	// Note: breaking change in 0.14 in order to support new NormalizationFunction interface

	@Override
	public RefactoringDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			NormalizationFunction normalizationFunction);

	public default RefactoringDiffType acceptSingleSide(final BEXSide side, final DiffEdit diffEdit,
			final Map<DiffEdit, String> normalizedTexts,
			final NormalizationFunction normalizationFunction) {
		return null;
	}
	//	public default RefactoringDiffType acceptSingleSide(final BEXSide side, final DiffEdit diffEdit,
	//			final Map<DiffEdit, String> normalizedTexts,
	//			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
	//		return null;
	//	}
}
