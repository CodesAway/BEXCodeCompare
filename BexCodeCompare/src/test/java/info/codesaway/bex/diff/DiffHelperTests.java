package info.codesaway.bex.diff;

import static com.google.common.collect.Maps.immutableEntry;
import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.EQUAL;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.BasicDiffType.MOVE_LEFT;
import static info.codesaway.bex.diff.BasicDiffType.MOVE_RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.primitives.ImmutableIntArray;

import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.patience.PatienceMatch;

public class DiffHelperTests {

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
}
