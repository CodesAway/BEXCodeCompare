package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;

public class RefactorEnhancedForLoopTests {
	@Test
	void testIndexToEnhancedForWithLocalVariable() {
		// TODO Auto-generated method stub

		DiffLine indexedLine1 = new DiffLine(1, "for (int j = 0; j < myList.size(); j++) {");
		DiffLine indexedLine2 = new DiffLine(2, "    Object obj=myList.get(j);");

		DiffLine enhancedLine = new DiffLine(1, "for (Object obj : myList) {");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(INSERT, null, indexedLine1),
				new DiffEdit(INSERT, null, indexedLine2),
				new DiffEdit(DELETE, enhancedLine, null)));

		DiffHelper.handleSubstitution(diff, DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION,
				new RefactorEnhancedForLoop());

		DiffType expectedType = new RefactoringDiffTypeValue('R', DiffSide.LEFT, "enhanced for", "myList", true);

		assertThat(diff)
				.extracting(DiffEdit::getType)
				.containsExactly(expectedType, expectedType);
	}

	@Test
	void testIndexToEnhancedForWithField() {
		// TODO Auto-generated method stub

		DiffLine indexedLine1 = new DiffLine(1, "for (int cell = 0; cell < this.cells.size(); cell++) {");

		DiffLine enhancedLine = new DiffLine(1, "			for (Cell element : this.cells) {");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(INSERT, null, indexedLine1),
				new DiffEdit(DELETE, enhancedLine, null)));

		DiffHelper.handleSubstitution(diff, DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION,
				new RefactorEnhancedForLoop());

		DiffType expectedType = new RefactoringDiffTypeValue('R', DiffSide.LEFT, "enhanced for", "this.cells", true);

		assertThat(diff)
				.extracting(DiffEdit::getType)
				.containsExactly(expectedType);
	}
}
