package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_FINAL_KEYWORD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

public class JavaFinalKeywordSubstitutionTests {
	@Test
	void finalKeywoardSubstitutionTest() {
		String leftText = "final String text;";
		String rightText = "String text;";

		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, new DiffLine(1, rightText), null);
		Map<DiffEdit, String> map = ImmutableMap.of(left, leftText, right, rightText);

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.LEFT, "final keyword", null, true);
		SubstitutionDiffType diffType = JAVA_FINAL_KEYWORD.accept(left, right, map,
				DiffHelper.NO_NORMALIZATION_FUNCTION);

		assertThat(diffType).isEqualTo(expectedType);

		//		System.out.println(diffType);
		//		assertThat(diffType).isInstanceOf(SubstitutionDiffType.class);
	}
}