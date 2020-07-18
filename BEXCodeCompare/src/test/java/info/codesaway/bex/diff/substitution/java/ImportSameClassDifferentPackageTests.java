package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.TestUtilities.acceptSubstitutionType;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class ImportSameClassDifferentPackageTests {
	@Test
	void testImportSameClassDifferencePackage() {
		String leftText = "import something.MyClass;";
		String rightText = "import cool.MyClass;";

		SubstitutionDiffType diffType = acceptSubstitutionType(IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE, leftText,
				rightText);
		// TODO: assert the specific DiffType that gets returned
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
