package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.TestUtilities.acceptSubstitutionType;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_FINAL_KEYWORD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class JavaFinalKeywordSubstitutionTests {
	@Test
	void finalKeywoardSubstitutionTest() {
		String leftText = "final String text;";
		String rightText = "String text;";

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.LEFT, "final keyword", null, true);
		SubstitutionDiffType diffType = acceptSubstitutionType(JAVA_FINAL_KEYWORD, leftText, rightText);

		assertThat(diffType).isEqualTo(expectedType);
	}
}