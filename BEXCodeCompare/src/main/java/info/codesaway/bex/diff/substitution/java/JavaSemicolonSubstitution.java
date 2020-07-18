package info.codesaway.bex.diff.substitution.java;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;

public final class JavaSemicolonSubstitution implements JavaSubstitution {
	@Override
	public RefactoringDiffType accept(final BEXPair<DiffEdit> checkPair,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);

		BEXSide side = normalizedText.testLeftRightMirror((l, r) -> l.isEmpty() && r.equals(";"));

		if (side != null) {
			return new RefactoringDiffTypeValue(';', side, "semicolon", null, true);
		}

		side = normalizedText.testLeftRightMirror((l, r) -> l.endsWith(";;")
				&& l.substring(0, l.length() - 1).equals(r));

		if (side != null) {
			return new RefactoringDiffTypeValue(';', side.other(), "double semicolon", null, true);
		}

		return null;
	}
}
