package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.DiffHelper.handleSubstitution;
import static info.codesaway.bex.diff.NormalizationFunction.NO_NORMALIZATION;
import static info.codesaway.bex.diff.NormalizationFunction.WHITESPACE_NORMALIZATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;

class RefactorEnhancedForLoopTests {
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

		handleSubstitution(diff, WHITESPACE_NORMALIZATION, new EnhancedForLoopRefactoring());

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.LEFT, "enhanced for", "myList", true);

		assertThat(diff)
				.extracting(DiffEdit::getType)
				.containsExactly(expectedType, expectedType);
	}

	@Test
	void testIndexToEnhancedForWithThisField() {
		// TODO Auto-generated method stub

		DiffLine indexedLine = new DiffLine(1, "for (int cell = 0; cell < this.cells.size(); cell++) {");

		DiffLine enhancedLine = new DiffLine(1, "			for (Cell element : this.cells) {");

		this.testHelper(indexedLine, enhancedLine, "this.cells");
	}

	@Test
	void testIndexToEnhancedForWithObjectField() {
		// TODO Auto-generated method stub

		DiffLine indexedLine = new DiffLine(1, "for (int m = 0; m < value.msg.size(); m++) {");

		DiffLine enhancedLine = new DiffLine(1, "	for		(Object element : value.msg) {");
		this.testHelper(indexedLine, enhancedLine, "value.msg");
	}

	@Test
	void testIndexToEnhancedForWithGetter() {
		DiffLine indexedLine = new DiffLine(1, "for (int m = 0; m < value.getMessages().size(); m++) {");

		DiffLine enhancedLine = new DiffLine(1, "	for		(Object element : value.getMessages()) {");
		this.testHelper(indexedLine, enhancedLine, "value.getMessages()");
	}

	private void testHelper(final DiffLine indexedLine, final DiffLine enhancedLine, final String iterableName) {
		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(INSERT, null, indexedLine),
				new DiffEdit(DELETE, enhancedLine, null)));

		// TODO: old code was using WHITESPACE_NORMALIZATION_FUNCTION
		// Though, noticed don't need whitespace normalization, due to how EnhancedForLoopRefactoring is implemented
		handleSubstitution(diff, NO_NORMALIZATION, new EnhancedForLoopRefactoring());

		DiffType expectedType = new RefactoringDiffTypeValue('R', BEXSide.LEFT, "enhanced for", iterableName, true);

		assertThat(diff)
				.extracting(DiffEdit::getType)
				.containsExactly(expectedType);
	}
}
