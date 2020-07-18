package info.codesaway.bex.diff.substitution.java;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

public final class ImportSameClassnameDifferentPackage implements JavaSubstitution {
	@Override
	public SubstitutionDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		return DiffHelper.determineImportSameClassnameDiffType(checkPair.map(normalizedTexts::get), false);
	}
}
