package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_CAST;
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

class JavaCastSubstitutionTests {
	@Test
	void javaCastWithGenericsTest() {
		String leftText = "Parm parm = (Parm) this.getParm();";
		String rightText = "Parm parm = this.getParm();";
		this.testHelper(leftText, rightText, "Parm");
	}

	@Test
	void castWithGenericsTest() {
		String leftText = "List<Parm> parmList = (List<Parm>) this.batchParmsArray;";
		String rightText = "List<Parm> parmList = this.batchParmsArray;";
		this.testHelper(leftText, rightText, "List<Parm>");
	}

	@Test
	void castWithModifiersTest() {
		String leftText = "final List<Parm> parmList = (List<Parm>) this.batchParmsArray;";
		String rightText = "final List<Parm> parmList = this.batchParmsArray;";
		this.testHelper(leftText, rightText, "List<Parm>");
	}

	private void testHelper(final String leftText, final String rightText, final String type) {
		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, new DiffLine(1, rightText), null);
		Map<DiffEdit, String> map = ImmutableMap.of(left, leftText, right, rightText);

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.RIGHT, "cast", type, true);
		SubstitutionDiffType diffType = JAVA_CAST.accept(left, right, map,
				DiffHelper.NO_NORMALIZATION_FUNCTION);

		assertThat(diffType).isEqualTo(expectedType);
	}
}