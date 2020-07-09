package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_DIAMOND_OPERATOR;
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

public class JavaDiamondOperatorSubstitutionTests {
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
		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, new DiffLine(1, rightText), null);
		Map<DiffEdit, String> map = ImmutableMap.of(left, leftText, right, rightText);

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.RIGHT, "diamond operator", type, true);
		SubstitutionDiffType diffType = JAVA_DIAMOND_OPERATOR.accept(left, right, map,
				DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION);

		assertThat(diffType).isEqualTo(expectedType);
	}

}
