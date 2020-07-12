package info.codesaway.bex.diff.substitution.java;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;

public final class JavaSemicolonSubstitution implements JavaSubstitution {
	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		if (normalizedLeft.isEmpty() && normalizedRight.equals(";")) {
			return new RefactoringDiffTypeValue(';', BEXSide.LEFT, "semicolon", null, true);
		} else if (normalizedLeft.equals(";") && normalizedRight.isEmpty()) {
			return new RefactoringDiffTypeValue(';', BEXSide.RIGHT, "semicolon", null, true);
		} else if (normalizedLeft.endsWith(";;")
				&& normalizedLeft.substring(0, normalizedLeft.length() - 1).equals(normalizedRight)) {
			return new RefactoringDiffTypeValue(';', BEXSide.RIGHT, "double semicolon", null, true);
		} else if (normalizedRight.endsWith(";;")
				&& normalizedRight.substring(0, normalizedRight.length() - 1).equals(normalizedLeft)) {
			return new RefactoringDiffTypeValue(';', BEXSide.LEFT, "double semicolon", null, true);
		} else {
			return null;
		}
	}
}
