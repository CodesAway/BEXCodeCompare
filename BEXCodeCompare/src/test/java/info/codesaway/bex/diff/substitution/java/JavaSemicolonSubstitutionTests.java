package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.NormalizationFunction.WHITESPACE_NORMALIZATION;
import static info.codesaway.bex.diff.TestUtilities.acceptSubstitutionType;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_SEMICOLON;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class JavaSemicolonSubstitutionTests {
	@Test
	void diamondSubstitutionTest() {
		String leftText = "		;    ";
		String rightText = "		";

		DiffType expectedType = new RefactoringDiffTypeValue(';', BEXSide.RIGHT, "semicolon", null, true);

		SubstitutionDiffType diffType = acceptSubstitutionType(JAVA_SEMICOLON, leftText, rightText,
				WHITESPACE_NORMALIZATION);

		assertThat(diffType).isEqualTo(expectedType);
	}

}
