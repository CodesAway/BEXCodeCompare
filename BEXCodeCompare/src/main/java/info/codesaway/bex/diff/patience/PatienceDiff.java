package info.codesaway.bex.diff.patience;

import static info.codesaway.bex.diff.BEXNormalizationFunction.normalization;
import static info.codesaway.bex.diff.NormalizationFunction.NO_NORMALIZATION;
import static info.codesaway.bex.util.BEXUtilities.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.Indexed;
import info.codesaway.bex.IntPair;
import info.codesaway.bex.MutableIntBEXPair;
import info.codesaway.bex.diff.AbstractDiffAlgorithm;
import info.codesaway.bex.diff.BasicDiffType;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.NormalizationFunction;

public final class PatienceDiff extends AbstractDiffAlgorithm {
	private final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm;

	private PatienceDiff(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final NormalizationFunction normalizationFunction,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm) {
		super(leftLines, rightLines, normalizationFunction);
		this.fallbackDiffAlgorithm = fallbackDiffAlgorithm;
	}

	/**
	* Calculates the diff
	*
	* @param leftLines
	* @param rightLines
	* @return
	*/
	public static List<DiffEdit> diff(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm) {
		return diff(leftLines, rightLines, NO_NORMALIZATION, fallbackDiffAlgorithm);
	}

	/**
	 * Calculates the diff
	 *
	 * @param leftLines
	 * @param rightLines
	 * @return
	 */
	public static List<DiffEdit> diff(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm) {
		return diff(leftLines, rightLines, normalization(normalizationFunction), fallbackDiffAlgorithm);
	}

	/**
	 * Calculates the diff
	 *
	 * @param leftLines
	 * @param rightLines
	 * @return
	 * @since 0.14
	 */
	public static List<DiffEdit> diff(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final NormalizationFunction normalizationFunction,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm) {
		return new PatienceDiff(leftLines, rightLines, normalizationFunction, fallbackDiffAlgorithm).getDiff();
	}

	@Override
	public List<DiffEdit> diff() {
		PatienceSlice slice = new PatienceSlice(0, this.getLeftLines().size(), 0, this.getRightLines().size());

		// For first pass, don't remove head / tail lines, since this can lead to less than optimal looking diffs
		// Except from https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
		//      Git doesn't perform these steps first,
		//      it performs them after calculating the matching lines
		//      but before recursing into each slice.
		//
		//      After each new slice is constructed,
		//      we trim the head and tail of it
		//      by consuming any leading and trailing lines that match

		// In this implementation, this handling of head / tail is done in the method handleSliceMatch
		return this.diff(slice);
	}

	private List<DiffEdit> diff(final PatienceSlice slice) {
		if (slice.isLeftEmpty()) {
			// This means there are no lines remaining in the left text, but there may be ones in the right text
			// These lines are inserted lines
			return IntStream.range(slice.getRightStart(), slice.getRightEnd())
					.mapToObj(i -> new DiffEdit(BasicDiffType.INSERT, null, this.getRightLines().get(i)))
					.collect(Collectors.toList());
		} else if (slice.isRightEmpty()) {
			// This means there are no lines left in the right text, but there may be ones in the left text
			// These lines are deleted lines
			return IntStream.range(slice.getLeftStart(), slice.getLeftEnd())
					.mapToObj(i -> new DiffEdit(BasicDiffType.DELETE, this.getLeftLines().get(i), null))
					.collect(Collectors.toList());
		}

		List<PatienceMatch> uniqueMatchingLines = this.uniqueMatchingLines(slice);

		if (uniqueMatchingLines.isEmpty()) {
			// No more matching lines, perform fallback diff
			return this.fallbackDiff(slice);
		}

		PatienceMatch match = DiffHelper.patienceSort(uniqueMatchingLines);

		MutableIntBEXPair start = new MutableIntBEXPair(slice.getStart());

		// For each match, get info needed to create slices
		List<PatienceSliceMatch> sliceMatches = new ArrayList<>();
		while (true) {
			IntPair next = match != null ? match.getLineNumber() : slice.getEnd();

			PatienceSlice subslice = new PatienceSlice(start, next);

			if (match == null) {
				// End of chain of matches
				sliceMatches.add(new PatienceSliceMatch(subslice, Collections.emptyList()));
				break;
			}

			PatienceMatch nextMatch = match.getNext();

			if (nextMatch != null && nextMatch.isImmediatelyAfter(match)) {
				// If next match is consecutive with this match, combine, so don't have empty slice

				List<PatienceMatch> consecutiveMatches = new ArrayList<>();
				consecutiveMatches.add(match);

				do {
					consecutiveMatches.add(nextMatch);
					match = nextMatch;
					nextMatch = match.getNext();
				} while (nextMatch != null && nextMatch.isImmediatelyAfter(match));

				sliceMatches.add(new PatienceSliceMatch(subslice, consecutiveMatches));
			} else {
				// No consecutive matches, so just add this match
				sliceMatches.add(new PatienceSliceMatch(subslice, Arrays.asList(match)));
			}

			// Calculate start of next slice, which starts the line after the match
			start.set(match.getLineNumber());
			start.increment();

			match = match.getNext();
		}

		// Iterate over each slice

		// Run in serial execution, since doesn't seem to help performance making parallel
		return sliceMatches.stream()
				.flatMap(sm -> this.handleSliceMatch(sm).stream())
				.collect(Collectors.toList());
	}

	protected List<DiffEdit> handleSliceMatch(final PatienceSliceMatch sliceMatch) {
		PatienceSlice subslice = sliceMatch.getSlice();
		List<PatienceMatch> matches = sliceMatch.getMatches();

		List<DiffEdit> lines = new ArrayList<>();

		// After each new slice is constructed,
		// we trim the head and tail of it
		// by consuming any leading and trailing lines that match
		List<DiffEdit> head = this.matchHead(subslice);
		List<DiffEdit> tail = this.matchTail(subslice);

		lines.addAll(head);
		lines.addAll(this.diff(subslice));
		lines.addAll(tail);

		// For each match, add an EQUAL DiffEdit for the matching lines
		// 1/5/2019 (or NORMALIZE DiffEdit if the lines are not identical, since this means they were normalized)
		for (PatienceMatch match : matches) {
			lines.add(this.newEqualOrNormalizeEdit(
					this.getLeftLines().get(match.getLeftLineNumber()),
					this.getRightLines().get(match.getRightLineNumber())));
		}

		return lines;
	}

	private List<DiffEdit> matchHead(final PatienceSlice slice) {
		if (slice.isEmpty()) {
			return Collections.emptyList();
		}

		List<DiffEdit> head = new ArrayList<>();

		while (!slice.isEmpty() && this.isNormalizedEqualText(this.getLeftIndexedText(slice.getLeftStart()),
				this.getRightIndexedText(slice.getRightStart()))) {
			head.add(this.newEqualOrNormalizeEdit(
					this.getLeftLines().get(slice.getLeftStart()),
					this.getRightLines().get(slice.getRightStart())));
			slice.incrementStarts();
		}

		return head;
	}

	private List<DiffEdit> matchTail(final PatienceSlice slice) {
		if (slice.isEmpty()) {
			return Collections.emptyList();
		}

		// Since tail is iterated from the bottom of the text, moving up, need to add in reverse order
		// Note: Instead, plan to add in regular order and reverse at end
		List<DiffEdit> tail = new ArrayList<>();

		while (!slice.isEmpty() && this.isNormalizedEqualText(this.getLeftIndexedText(slice.getLeftEnd() - 1),
				this.getRightIndexedText(slice.getRightEnd() - 1))) {
			slice.decrementEnds();
			tail.add(this.newEqualOrNormalizeEdit(
					this.getLeftLines().get(slice.getLeftEnd()),
					this.getRightLines().get(slice.getRightEnd())));
		}

		// Need to reverse the order, since added in regular order, with plan to reverse here at the end
		Collections.reverse(tail);
		return tail;
	}

	private List<PatienceMatch> uniqueMatchingLines(final PatienceSlice slice) {
		// Source: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
		// Source code uses Ruby Hash which maintains insertion order
		// For Java, the corresponding is LinkedHashMap

		// Map from the line of text to information on the number of occurrences and first occurrences of the line in each slice
		// (this is used to determine if the line is unique in each text and where in the text the line is)
		HashMap<String, FrequencyCount> counts = new LinkedHashMap<>();

		Indexed<String> indexedBlank = index(-1, "");

		// TODO: refactor to combine logic using LEFT / RIGHT
		// Iterate over the range of the slice for left lines
		for (int n = slice.getLeftStart(); n < slice.getLeftEnd(); n++) {
			String text = this.normalize(this.getLeftIndexedText(n), indexedBlank).getLeft();

			// Introduced variable to make lambda happy
			int lineNumber = n;

			// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
			counts.compute(text,
					(k, v) -> FrequencyCount.emptyIfNull(v).recordFoundInSlice(BEXSide.LEFT, lineNumber));
		}

		// Iterate over the range of the slice for right lines
		for (int n = slice.getRightStart(); n < slice.getRightEnd(); n++) {
			String text = this.normalize(indexedBlank, this.getRightIndexedText(n)).getRight();

			// Introduced variable to make lambda happy
			int lineNumber = n;

			// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
			counts.compute(text,
					(k, v) -> FrequencyCount.emptyIfNull(v).recordFoundInSlice(BEXSide.RIGHT, lineNumber));
		}

		return counts.values()
				.stream()
				.filter(FrequencyCount::isLineUnique)
				.map(c -> new PatienceMatch(c.getLeftLineNumber(), c.getRightLineNumber()))
				.collect(Collectors.toList());
	}

	private List<DiffEdit> fallbackDiff(final PatienceSlice slice) {
		return this.fallbackDiffAlgorithm.apply(
				this.getLeftLines().subList(slice.getLeftStart(), slice.getLeftEnd()),
				this.getRightLines().subList(slice.getRightStart(), slice.getRightEnd()));
	}
}
