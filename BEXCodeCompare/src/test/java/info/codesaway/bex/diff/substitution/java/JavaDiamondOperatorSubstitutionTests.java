package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.NormalizationFunction.WHITESPACE_NORMALIZATION;
import static info.codesaway.bex.diff.TestUtilities.acceptSubstitutionType;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_DIAMOND_OPERATOR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class JavaDiamondOperatorSubstitutionTests {
	@Test
	void diamondSubstitutionTest() {
		String leftText = "		this.jobSteps = new Vector<JobStep>();";
		String rightText = "		this.jobSteps = new Vector<>();";

		this.testHelper(leftText, rightText, "JobStep");
	}

	@Test
	void diamondSubstitutionReturnTest() {
		String leftText = "		return new ArrayList<Object>();";
		String rightText = "		return new ArrayList<>();";
		this.testHelper(leftText, rightText, "Object");
	}

	@Test
	void diamondSubstitutionGenericsTest() {
		String leftText = "ArrayList<ArrayList<Address>> addresses = new ArrayList<ArrayList<Address>>();";
		String rightText = "ArrayList<ArrayList<Address>> addresses = new ArrayList<>();";
		this.testHelper(leftText, rightText, "ArrayList<Address>");
	}

	private void testHelper(final String leftText, final String rightText, final String type) {
		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.RIGHT, "diamond operator", type, true);

		SubstitutionDiffType diffType = acceptSubstitutionType(JAVA_DIAMOND_OPERATOR, leftText, rightText,
				WHITESPACE_NORMALIZATION);

		assertThat(diffType).isEqualTo(expectedType);
	}

}
