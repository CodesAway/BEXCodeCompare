package info.codesaway.bex.diff;

import static com.google.common.collect.Maps.immutableEntry;
import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.IntBEXRange.closed;
import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.EQUAL;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.BasicDiffType.MOVE_LEFT;
import static info.codesaway.bex.diff.BasicDiffType.MOVE_RIGHT;
import static info.codesaway.bex.diff.BasicDiffType.NORMALIZE;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.diff.BasicDiffType.SUBSTITUTE;
import static info.codesaway.bex.diff.DiffHelper.handleBlankLines;
import static info.codesaway.bex.diff.DiffHelper.handleSplitLines;
import static info.codesaway.bex.diff.DiffHelper.handleSubstitution;
import static info.codesaway.bex.diff.DiffHelper.normalize;
import static info.codesaway.bex.diff.substitution.SubstitutionType.LCS_MIN_OPERATOR;
import static info.codesaway.bex.util.BEXUtilities.index;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.primitives.ImmutableIntArray;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.IntBEXPair;
import info.codesaway.bex.IntRange;
import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.patience.PatienceMatch;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionType;

class DiffHelperTests {

	public static List<Entry<ImmutableIntArray, ImmutableIntArray>> testPatienceSort() {
		return Arrays.asList(
				// Testing using example from
				// https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
				/*
				[
				  #<struct Patience::Match a_line=1, b_line=3>,
				  #<struct Patience::Match a_line=2, b_line=4>,
				  #<struct Patience::Match a_line=3, b_line=2>,
				  #<struct Patience::Match a_line=4, b_line=1>,
				  #<struct Patience::Match a_line=5, b_line=5>
				]
				*/
				immutableEntry(ImmutableIntArray.of(3, 4, 2, 1, 5), ImmutableIntArray.of(3, 4, 5)),
				immutableEntry(ImmutableIntArray.of(1), ImmutableIntArray.of(1)),
				immutableEntry(ImmutableIntArray.of(1, 2), ImmutableIntArray.of(1, 2)),
				immutableEntry(ImmutableIntArray.of(2, 1), ImmutableIntArray.of(1)));
	}

	// https://www.baeldung.com/parameterized-tests-junit-5
	@ParameterizedTest
	@MethodSource
	public void testPatienceSort(final Entry<ImmutableIntArray, ImmutableIntArray> entry) {
		ImmutableIntArray correspondingLines = entry.getKey();

		ImmutableIntArray expected = entry.getValue();
		ImmutableIntArray.Builder actual = ImmutableIntArray.builder(correspondingLines.length());

		ImmutableList<PatienceMatch> matches = Streams
				.mapWithIndex(correspondingLines.stream(),
						(value, i) -> new PatienceMatch((int) i + 1, value))
				.collect(ImmutableList.toImmutableList());

		PatienceMatch match = DiffHelper.patienceSort(matches);

		while (match != null) {
			actual.add(match.getRightLineNumber());
			match = match.getNext();
		}

		assertEquals(expected, actual.build());
	}

	@Test
	public void testMyersLinearDiff() {
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));
		List<DiffEdit> diff = MyersLinearDiff.diff(leftLines, rightLines);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(INSERT, null, rightLines.get(0)),
				new DiffEdit(INSERT, null, rightLines.get(1)),
				new DiffEdit(EQUAL, leftLines.get(0), rightLines.get(2)),
				new DiffEdit(DELETE, leftLines.get(1), null),
				new DiffEdit(DELETE, leftLines.get(2), null));

		assertEquals(expected, diff);
	}

	@Test
	public void testPatienceHandleMovedLines() {
		// TODO: not really a unit test since tests diff and handleMovedLines
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));
		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, MyersLinearDiff::diff);
		DiffHelper.handleMovedLines(diff);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(2)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(EQUAL, leftLines.get(2), rightLines.get(0)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(0), rightLines.get(2)));

		assertEquals(expected, diff);
	}

	@Test
	public void testCombineToDiffBlocks() {
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(3, "a"), new DiffLine(4, "b"), new DiffLine(5, "c"));

		List<DiffEdit> diff = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(0)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(MOVE_LEFT, leftLines.get(2), rightLines.get(2)));

		List<DiffUnit> diffUnits = DiffHelper.combineToDiffBlocks(diff);

		List<DiffUnit> expected = ImmutableList.of(new DiffBlock(MOVE_LEFT, diff));

		assertEquals(expected, diffUnits);
	}

	@Test
	public void testCombineToDiffBlocksLimitWhenShouldCombine() {
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(3, "a"), new DiffLine(4, "b"), new DiffLine(5, "c"));

		DiffEdit third = new DiffEdit(MOVE_LEFT, leftLines.get(2), rightLines.get(2));

		List<DiffEdit> diff = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(0)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				third);

		List<DiffUnit> diffUnits = DiffHelper.combineToDiffBlocks(diff, true, (x, y) -> y.getRightLineNumber() != 5);

		List<DiffUnit> expected = ImmutableList.of(new DiffBlock(MOVE_LEFT, diff.subList(0, 2)), third);

		assertEquals(expected, diffUnits);
	}

	@Test
	public void testNeverCombineToDiffBlocks() {
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(3, "a"), new DiffLine(4, "b"), new DiffLine(5, "c"));

		DiffEdit first = new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(0));
		DiffEdit second = new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1));
		DiffEdit third = new DiffEdit(MOVE_LEFT, leftLines.get(2), rightLines.get(2));

		List<DiffEdit> diff = ImmutableList.of(first, second, third);

		// Never combine, since always return false (silly to call combine, but used for testing purposes)
		List<DiffUnit> diffUnits = DiffHelper.combineToDiffBlocks(diff, true, (x, y) -> false);

		List<DiffUnit> expected = ImmutableList.of(first, second, third);

		assertEquals(expected, diffUnits);
	}

	@Test
	// Issue #103
	public void testCombineToDiffBlocksPartitionImportantAndNonImportantChanges() {
		// Text doesn't matter, since combine focuses on DiffType
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, ""), new DiffLine(1, ""), new DiffLine(2, ""),
				new DiffLine(3, ""));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, ""), new DiffLine(1, ""), new DiffLine(2, ""),
				new DiffLine(3, ""));

		DiffType shouldTreatAsNormalizedEqual = new SubstitutionDiffTypeValue(' ', "", false, true);

		DiffEdit first = new DiffEdit(INSERT, leftLines.get(0), rightLines.get(0));
		DiffEdit second = new DiffEdit(shouldTreatAsNormalizedEqual, leftLines.get(1), rightLines.get(1));
		DiffEdit third = new DiffEdit(shouldTreatAsNormalizedEqual, leftLines.get(2), rightLines.get(2));
		DiffEdit fourth = new DiffEdit(INSERT, leftLines.get(3), rightLines.get(3));

		List<DiffEdit> diff = ImmutableList.of(first, second, third, fourth);

		List<DiffUnit> diffUnits = DiffHelper.combineToDiffBlocks(diff, true,
				(x, y) -> x.shouldTreatAsNormalizedEqual() == y.shouldTreatAsNormalizedEqual());

		List<DiffUnit> expected = ImmutableList.of(first,
				new DiffBlock(shouldTreatAsNormalizedEqual, diff.subList(1, 3)),
				fourth);

		assertEquals(expected, diffUnits);
	}

	@Test
	public void testDetermineEnclosedRange() {
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(3, "a"), new DiffLine(4, "b"), new DiffLine(5, "c"));

		List<DiffEdit> diff = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(0)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(MOVE_LEFT, leftLines.get(2), rightLines.get(2)));

		List<DiffUnit> diffUnits = DiffHelper.combineToDiffBlocks(diff);

		DiffUnit diffUnit = diffUnits.get(0);

		BEXPair<IntRange> enclosedRange = DiffHelper.determineEnclosedRange(diffUnit);
		BEXPair<IntRange> expectedRange = bexPair(closed(0, 2), closed(3, 5));
		assertThat(enclosedRange).isEqualTo(expectedRange);
	}

	@Test
	//	@Disabled("Not sure if I plan or need to add functionality for consecutive ranges")
	public void testHasConsecutiveRanges() {
		IntRange left1 = closed(0, 2);
		IntRange left2 = closed(3, 5);

		IntRange right1 = closed(1, 5);
		IntRange right2 = closed(6, 10);

		IntBEXPair line1 = IntBEXPair.of(left1.getEnd(), right1.getEnd());
		IntBEXPair line2 = IntBEXPair.of(left2.getStart(), right2.getStart());

		assertTrue(DiffHelper.isConsecutive(line1, line2, false));
	}

	@Test
	public void testMyersDiffNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));

		List<DiffEdit> diff = MyersLinearDiff.diff(leftLines, rightLines, normalizationFunction);
		assertThat(diff).hasSize(5);
	}

	@Test
	public void testMyersDiffNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));

		List<DiffEdit> diff = MyersLinearDiff.diff(leftLines, rightLines, normalizationFunction);
		assertThat(diff).hasSize(5);
	}

	@Test
	public void testPatienceDiffNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));

		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, normalizationFunction,
				MyersLinearDiff.with(normalizationFunction));
		assertThat(diff).hasSize(5);
	}

	@Test
	public void testPatienceDiffNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));

		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, normalizationFunction,
				MyersLinearDiff.with(normalizationFunction));
		assertThat(diff).hasSize(5);
	}

	/**
	 * @see DiffHelper#normalize(DiffEdit, BiFunction)
	 */
	@Test
	public void testNormalizeDiffEditNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		DiffEdit diffEdit = new DiffEdit(EQUAL, new DiffLine(0, "left"), new DiffLine(0, "right"));
		DiffNormalizedText normalizedText = normalize(diffEdit, normalizationFunction);

		DiffNormalizedText expectedValue = new DiffNormalizedText("left", "right");
		assertThat(normalizedText).isEqualTo(expectedValue);
	}

	/**
	 * @see DiffHelper#normalize(String, String, BiFunction)
	 */
	@Test
	public void testNormalizeTextNullNormalizationBiFunction() {
		DiffNormalizedText normalizedText = normalize("left", "right", null);

		DiffNormalizedText expectedValue = new DiffNormalizedText("left", "right");
		assertThat(normalizedText).isEqualTo(expectedValue);
	}

	/**
	 * @see DiffHelper#normalize(DiffEdit, NormalizationFunction)
	 */
	@Test
	public void testNormalizeDiffEditNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		DiffEdit diffEdit = new DiffEdit(EQUAL, new DiffLine(0, "left"), new DiffLine(0, "right"));
		DiffNormalizedText normalizedText = normalize(diffEdit, normalizationFunction);

		DiffNormalizedText expectedValue = new DiffNormalizedText("left", "right");
		assertThat(normalizedText).isEqualTo(expectedValue);
	}

	/**
	 * @see DiffHelper#normalize(info.codesaway.bex.BEXSide, String, BiFunction)
	 */
	@Test
	public void testNormalizeSidedTextNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		String leftNormalizedText = normalize(LEFT, "left", normalizationFunction);
		assertThat(leftNormalizedText).isEqualTo("left");

		String rightNormalizedText = normalize(RIGHT, "right", normalizationFunction);
		assertThat(rightNormalizedText).isEqualTo("right");
	}

	/**
	 * @see DiffHelper#normalize(info.codesaway.bex.BEXSide, String, NormalizationFunction)
	 */
	@Test
	public void testNormalizeSidedTextNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		String leftNormalizedText = normalize(LEFT, "left", normalizationFunction);
		assertThat(leftNormalizedText).isEqualTo("left");

		String rightNormalizedText = normalize(RIGHT, "right", normalizationFunction);
		assertThat(rightNormalizedText).isEqualTo("right");
	}

	/**
	 * @see DiffHelper#normalize(info.codesaway.bex.BEXSide, String, NormalizationFunction)
	 */
	@Test
	public void testNormalizeSidedIndexedTextNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		DiffNormalizedText normalizedText = normalize(index(-1, "left"), index(-1, "right"), normalizationFunction);

		DiffNormalizedText expectedValue = new DiffNormalizedText("left", "right");
		assertThat(normalizedText).isEqualTo(expectedValue);
	}

	/**
	 * @see DiffHelper#handleMovedLines(List, BiFunction)
	 */
	@Test
	public void testHandleMovedLinesNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		// TODO: not really a unit test since tests diff and handleMovedLines
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));
		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, MyersLinearDiff::diff);
		DiffHelper.handleMovedLines(diff, normalizationFunction);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(2)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(EQUAL, leftLines.get(2), rightLines.get(0)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(0), rightLines.get(2)));

		assertEquals(expected, diff);
	}

	/**
	 * @see DiffHelper#handleMovedLines(List, NormalizationFunction)
	 */
	@Test
	public void testHandleMovedLinesNullNormalizationFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		// TODO: not really a unit test since tests diff and handleMovedLines
		List<DiffLine> leftLines = ImmutableList.of(new DiffLine(0, "a"), new DiffLine(1, "b"), new DiffLine(2, "c"));
		List<DiffLine> rightLines = ImmutableList.of(new DiffLine(0, "c"), new DiffLine(1, "b"), new DiffLine(2, "a"));
		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, MyersLinearDiff::diff);
		DiffHelper.handleMovedLines(diff, normalizationFunction);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(MOVE_LEFT, leftLines.get(0), rightLines.get(2)),
				new DiffEdit(MOVE_LEFT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(EQUAL, leftLines.get(2), rightLines.get(0)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(1), rightLines.get(1)),
				new DiffEdit(MOVE_RIGHT, leftLines.get(0), rightLines.get(2)));

		assertEquals(expected, diff);
	}

	/**
	 * @see DiffHelper#handleSubstitution(List, BiFunction, SubstitutionType...)
	 */
	@Test
	public void testHandleSubstitutionNullNormalizationBiFunction() {
		DiffLine leftLine = new DiffLine(1, "blahL");
		DiffLine rightLine = new DiffLine(1, "blahR");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(DELETE, leftLine, null),
				new DiffEdit(INSERT, null, rightLine)));

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;
		handleSubstitution(diff, normalizationFunction, LCS_MIN_OPERATOR);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(SUBSTITUTE, leftLine, rightLine));

		assertEquals(expected, diff);
	}

	/**
	 * @see DiffHelper#handleSubstitution(List, NormalizationFunction, SubstitutionType...)
	 */
	@Test
	public void testHandleSubstitutionNullNormalizationFunction() {
		DiffLine leftLine = new DiffLine(1, "blahL");
		DiffLine rightLine = new DiffLine(1, "blahR");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(DELETE, leftLine, null),
				new DiffEdit(INSERT, null, rightLine)));

		NormalizationFunction normalizationFunction = null;
		handleSubstitution(diff, normalizationFunction, LCS_MIN_OPERATOR);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(SUBSTITUTE, leftLine, rightLine));

		assertEquals(expected, diff);
	}

	@Test
	public void testHandleSubstitutionNullNormalizationBiFunctionCustomSubstitution() {
		DiffLine leftLine = new DiffLine(1, "blahL");
		DiffLine rightLine = new DiffLine(1, "blahR");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(DELETE, leftLine, null),
				new DiffEdit(INSERT, null, rightLine)));

		SubstitutionDiffType expectedDiffType = new SubstitutionDiffTypeValue('!', "sub");

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;
		SubstitutionType substitutionType = (checkPair, normalizedTexts,
				normalizationFunction1) -> normalizationFunction1.normalize(index(0, ""), index(0, "")).hasEqualText()
						? expectedDiffType
						: null;

		handleSubstitution(diff, normalizationFunction, substitutionType);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(expectedDiffType, leftLine, rightLine));

		assertEquals(expected, diff);
	}

	@Test
	public void testHandleSubstitutionNullNormalizationFunctionCustomSubstitution() {
		DiffLine leftLine = new DiffLine(1, "blahL");
		DiffLine rightLine = new DiffLine(1, "blahR");

		List<DiffEdit> diff = new ArrayList<>(ImmutableList.of(
				new DiffEdit(DELETE, leftLine, null),
				new DiffEdit(INSERT, null, rightLine)));

		SubstitutionDiffType expectedDiffType = new SubstitutionDiffTypeValue('!', "sub");

		NormalizationFunction normalizationFunction = null;
		SubstitutionType substitutionType = (checkPair, normalizedTexts,
				normalizationFunction1) -> normalizationFunction1.normalize(index(0, ""), index(0, "")).hasEqualText()
						? expectedDiffType
						: null;

		handleSubstitution(diff, normalizationFunction, substitutionType);

		List<DiffEdit> expected = ImmutableList.of(
				new DiffEdit(expectedDiffType, leftLine, rightLine));

		assertEquals(expected, diff);
	}

	/**
	 * @see DiffHelper#handleSplitLines(List, BiFunction)
	 */
	@Test
	public void testHandleSplitLinesNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		List<DiffEdit> diffEdits = ImmutableList.of(new DiffEdit(INSERT, new DiffLine(1, "ABC"), null),
				new DiffEdit(INSERT, new DiffLine(2, "123"), null),
				new DiffEdit(DELETE, null, new DiffLine(10, "ABC123")));

		DiffBlock diffBlock = new DiffBlock(REPLACEMENT_BLOCK, diffEdits);
		List<DiffUnit> diffUnits = Arrays.asList(diffBlock);

		handleSplitLines(diffUnits, normalizationFunction);

		assertThat(diffUnits.get(0).getType()).isEqualTo(NORMALIZE);
	}

	/**
	 * @see DiffHelper#handleSplitLines(List, NormalizationFunction)
	 */
	@Test
	public void testHandleSplitLinesNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		List<DiffEdit> diffEdits = ImmutableList.of(new DiffEdit(INSERT, new DiffLine(1, "ABC"), null),
				new DiffEdit(INSERT, new DiffLine(2, "123"), null),
				new DiffEdit(DELETE, null, new DiffLine(10, "ABC123")));

		DiffBlock diffBlock = new DiffBlock(REPLACEMENT_BLOCK, diffEdits);
		List<DiffUnit> diffUnits = Arrays.asList(diffBlock);

		handleSplitLines(diffUnits, normalizationFunction);

		assertThat(diffUnits.get(0).getType()).isEqualTo(NORMALIZE);
	}

	/**
	 * @see DiffHelper#handleSplitLines(List, BiFunction)
	 */
	@Test
	public void testHandleBlankLinesNullNormalizationBiFunction() {
		BiFunction<String, String, DiffNormalizedText> normalizationFunction = null;

		List<DiffEdit> diffEdits = ImmutableList.of(new DiffEdit(INSERT, new DiffLine(1, ""), null),
				new DiffEdit(INSERT, new DiffLine(2, ""), null),
				new DiffEdit(DELETE, null, new DiffLine(10, "")));

		DiffBlock diffBlock = new DiffBlock(REPLACEMENT_BLOCK, diffEdits);
		List<DiffUnit> diffUnits = Arrays.asList(diffBlock);

		handleBlankLines(diffUnits, normalizationFunction);

		assertThat(diffUnits.get(0).getType()).isEqualTo(NORMALIZE);
	}

	/**
	 * @see DiffHelper#handleSplitLines(List, NormalizationFunction)
	 */
	@Test
	public void testHandleBlankLinesNullNormalizationFunction() {
		NormalizationFunction normalizationFunction = null;

		List<DiffEdit> diffEdits = ImmutableList.of(new DiffEdit(INSERT, new DiffLine(1, ""), null),
				new DiffEdit(INSERT, new DiffLine(2, ""), null),
				new DiffEdit(DELETE, null, new DiffLine(10, "")));

		DiffBlock diffBlock = new DiffBlock(REPLACEMENT_BLOCK, diffEdits);
		List<DiffUnit> diffUnits = Arrays.asList(diffBlock);

		handleBlankLines(diffUnits, normalizationFunction);

		assertThat(diffUnits.get(0).getType()).isEqualTo(NORMALIZE);
	}
}
