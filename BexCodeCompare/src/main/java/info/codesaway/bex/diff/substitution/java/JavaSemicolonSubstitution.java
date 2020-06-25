package info.codesaway.bex.diff.substitution.java;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;

public class JavaSemicolonSubstitution implements JavaSubstitution {
	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		if (normalizedLeft.isEmpty() && normalizedRight.equals(";")) {
			return new RefactoringDiffTypeValue(';', DiffSide.LEFT, "semicolon", null, true);
		} else if (normalizedLeft.equals(";") && normalizedRight.isEmpty()) {
			return new RefactoringDiffTypeValue(';', DiffSide.RIGHT, "semicolon", null, true);
		} else {
			return null;
		}
	}
}
