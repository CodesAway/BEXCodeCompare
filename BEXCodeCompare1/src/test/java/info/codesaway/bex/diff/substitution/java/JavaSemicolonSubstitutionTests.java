package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_SEMICOLON;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

public class JavaSemicolonSubstitutionTests {
	@Test
	void diamondSubstitutionTest() {
		String leftText = "		;    ";
		String rightText = "		";

		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, new DiffLine(1, rightText), null);
		Map<DiffEdit, String> map = ImmutableMap.of(left, leftText.trim(), right, rightText.trim());

		DiffType expectedType = new RefactoringDiffTypeValue(';', DiffSide.RIGHT, "semicolon", null, true);
		SubstitutionDiffType diffType = JAVA_SEMICOLON.accept(left, right, map,
				DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION);

		assertThat(diffType).isEqualTo(expectedType);
	}

}
