package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.BasicDiffType.IGNORE;
import static info.codesaway.bex.diff.BasicDiffType.REFACTOR;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.diff.BasicDiffType.SUBSTITUTE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;

class BasicDiffTypeTests {

	@Test
	void testIgnoreShouldIgnore() {
		assertThat(IGNORE)
				.extracting(DiffType::shouldIgnore, DiffType::getSymbol)
				.containsExactly(true, 'X');
	}

	@Test
	void testSubstituteIsSubstitution() {
		assertThat(SUBSTITUTE)
				.isInstanceOf(SubstitutionDiffType.class)
				.extracting(DiffType::isSubstitution, DiffType::getSymbol)
				.containsExactly(true, 'S');
	}

	@Test
	void testReplacementIsSubstitution() {
		assertThat(REPLACEMENT_BLOCK)
				.isInstanceOf(SubstitutionDiffType.class)
				.extracting(DiffType::isSubstitution, DiffType::getSymbol)
				.containsExactly(true, 'R');
	}

	@Test
	void testRefactorIsSubstitution() {
		assertThat(REFACTOR)
				.isInstanceOf(RefactoringDiffType.class)
				.extracting(DiffType::isSubstitution, DiffType::getSymbol)
				.containsExactly(true, 'R');
	}
}
