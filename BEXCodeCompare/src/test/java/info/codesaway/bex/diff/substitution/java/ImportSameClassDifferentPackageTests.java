package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class ImportSameClassDifferentPackageTests {
	@Test
	void testImportSameClassDifferencePackage() {
		String leftText = "import something.MyClass;";
		String rightText = "import cool.MyClass;";

		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
		DiffEdit right = new DiffEdit(DELETE, new DiffLine(1, rightText), null);
		Map<DiffEdit, String> map = ImmutableMap.of(left, leftText, right, rightText);

		SubstitutionDiffType diffType = IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE.accept(left, right, map,
				DiffHelper.NO_NORMALIZATION_FUNCTION);
		assertThat(diffType).isInstanceOf(SubstitutionDiffType.class);
	}

	//	@Test
	//	void testDeleteAndInsertImportSameClassDifferencePackage() {
	//		String leftText = "import something.MyClass;";
	//		String rightText = "import cool.MyClass;";
	//
	//		DiffEdit left = new DiffEdit(INSERT, new DiffLine(1, leftText), null);
	//		DiffEdit right = new DiffEdit(DELETE, null, new DiffLine(2, rightText));
	//		DiffEdit equal = new DiffEdit(EQUAL, new DiffLine(2, "// Comment"), new DiffLine(1, "// Comment"));
	//
	//		List<DiffEdit> diff = new ArrayList<>(Arrays.asList(left, equal, right));
	//
	//		DiffHelper.handleImports(diff);
	//
	//		assertThat(diff).anyMatch(d -> d.getType() instanceof ImportSameClassnameDiffType);
	//	}
}
