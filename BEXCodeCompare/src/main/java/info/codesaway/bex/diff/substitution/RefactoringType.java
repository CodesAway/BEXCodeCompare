package info.codesaway.bex.diff.substitution;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;

public interface RefactoringType extends SubstitutionType {
	@Override
	public RefactoringDiffType accept(final DiffEdit delete, final DiffEdit insert,
			final Map<DiffEdit, String> normalizedTexts,
			BiFunction<String, String, DiffNormalizedText> normalizationFunction);

	public default RefactoringDiffType acceptSingleSide(final BEXSide side, final DiffEdit diffEdit,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return null;
	}
}
