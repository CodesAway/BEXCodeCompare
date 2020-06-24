package info.codesaway.bex.diff.substitution.java;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.util.regex.MatchResult;
import info.codesaway.util.regex.Matcher;

public class ImportSameClassnameDifferentPackage implements JavaSubstitution {
	@Override
	public SubstitutionDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher importMatcher = DiffHelper.IMPORT_MATCHER.get();

		importMatcher.reset(normalizedLeft).find();
		MatchResult leftMatchResult = importMatcher.toMatchResult();

		importMatcher.reset(normalizedRight).find();
		MatchResult rightMatchResult = importMatcher.toMatchResult();

		return DiffHelper.determineImportSameClassnameDiffType(leftMatchResult, rightMatchResult, false);
	}
}
