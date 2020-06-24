package info.codesaway.bexcodecompare.diff.patience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;

import info.codesaway.bexcodecompare.diff.DiffAlgorithm;
import info.codesaway.bexcodecompare.diff.DiffEdit;
import info.codesaway.bexcodecompare.diff.DiffHelper;
import info.codesaway.bexcodecompare.diff.DiffLine;
import info.codesaway.bexcodecompare.diff.DiffNormalizedText;
import info.codesaway.bexcodecompare.diff.DiffTypeEnum;

public class PatienceDiff implements DiffAlgorithm {
	private final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm;

	private final List<DiffLine> lines1;
	private final List<DiffLine> lines2;

	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;

	private List<DiffEdit> diff;

	public PatienceDiff(final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm,
			final List<DiffLine> lines1, final List<DiffLine> lines2) {
		this(fallbackDiffAlgorithm, lines1, lines2, null);
	}

	public PatienceDiff(final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm,
			final List<DiffLine> lines1, final List<DiffLine> lines2,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		this.fallbackDiffAlgorithm = fallbackDiffAlgorithm;

		// Take a defensive clone of the passed values and retain them as immutable lists
		this.lines1 = ImmutableList.copyOf(lines1);
		this.lines2 = ImmutableList.copyOf(lines2);

		this.normalizationFunction = normalizationFunction != null ? normalizationFunction : NO_NORMALIZATION_FUNCTION;
	}

	/**
	* Calculates the diff
	*
	* @param lines1
	* @param lines2
	* @return
	* @since
	* <pre> Change History
	* ========================================================================================
	* Version  Change #        Developer           Date        Description
	* =======  =============== =================== ==========  ===============================
	* TRS.01T                  Amy Brennan-Luna    12/30/2018  Initial code
	*</pre>***********************************************************************************
	*/
	public static List<DiffEdit> diff(final List<DiffLine> lines1, final List<DiffLine> lines2,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm) {
		return new PatienceDiff(fallbackDiffAlgorithm, lines1, lines2).diff();
	}

	/**
	 * Calculates the diff
	 *
	 * @param lines1
	 * @param lines2
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/05/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public static List<DiffEdit> diff(final List<DiffLine> lines1, final List<DiffLine> lines2,
			final BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackDiffAlgorithm,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return new PatienceDiff(fallbackDiffAlgorithm, lines1, lines2, normalizationFunction).diff();
	}

	public List<DiffLine> getLines1() {
		return this.lines1;
	}

	public List<DiffLine> getLines2() {
		return this.lines2;
	}

	@Override
	public BiFunction<String, String, DiffNormalizedText> getNormalizationFunction() {
		return this.normalizationFunction;
	}

	// Helper methods to make code more readable

	/**
	 * Gets the text from the specified line in {@link #getLines1()}
	 *
	 * @param index
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/30/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public String getText1(final int index) {
		return this.getLines1().get(index).getText();
	}

	/**
	 * Gets the text from the specified line in {@link #getLines2()}
	 *
	 * @param index
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/30/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public String getText2(final int index) {
		return this.getLines2().get(index).getText();
	}

	/**
	 * Returns the calculated diff (will be <code>null</code> until {@link #diff()} is run)
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/30/2018 Initial code
	 *</pre>***********************************************************************************
	 */
	public List<DiffEdit> getDiff() {
		return this.diff;
	}

	@Override
	public List<DiffEdit> diff() {
		this.diff = new ArrayList<>();

		PatienceSlice slice = new PatienceSlice(0, this.getLines1().size(), 0, this.getLines2().size());

		// For first pass, don't remove head / tail lines, since this can lead to less than optimal looking diffs
		// Except from https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
		//      Git doesn’t perform these steps first,
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
		if (slice.isEmpty1()) {
			// This means there are no lines left in the first file, but there may be ones in the second
			// These lines are inserted lines
			return IntStream.range(slice.getLow2(), slice.getHigh2())
					.mapToObj(i -> new DiffEdit(DiffTypeEnum.INSERT, null, this.getLines2().get(i)))
					.collect(Collectors.toList());
		} else if (slice.isEmpty2()) {
			// This means there are no lines left in the second file, but there may be ones in the first
			// These lines are deleted lines
			return IntStream.range(slice.getLow1(), slice.getHigh1())
					.mapToObj(i -> new DiffEdit(DiffTypeEnum.DELETE, this.getLines1().get(i), null))
					.collect(Collectors.toList());
		}

		List<PatienceMatch> uniqueMatchingLines = this.uniqueMatchingLines(slice);

		if (uniqueMatchingLines.isEmpty()) {
			// No more matching lines, perform fallback diff
			return this.fallbackDiff(slice);
		}

		PatienceMatch match = DiffHelper.patienceSort(uniqueMatchingLines);

		int line1 = slice.getLow1();
		int line2 = slice.getLow2();

		// For each match, get info needed to create slices
		List<PatienceSliceMatch> sliceMatches = new ArrayList<>();
		while (true) {
			int next1 = match != null ? match.getLineNumber1() : slice.getHigh1();
			int next2 = match != null ? match.getLineNumber2() : slice.getHigh2();

			PatienceSlice subslice = new PatienceSlice(line1, next1, line2, next2);

			//            System.out.println("Match: " + match);

			if (match == null) {
				// End of chain of matches
				sliceMatches.add(new PatienceSliceMatch(subslice, ImmutableList.of()));
				break;
			}

			PatienceMatch nextMatch = match.getNext();

			if (nextMatch != null && nextMatch.isImmediatelyAfter(match)) {
				// If next match is consecutive with this match, combine, so don't have empty slice

				List<PatienceMatch> consecutiveMatches = new ArrayList<>();
				consecutiveMatches.add(match);

				do {
					//                    System.out.println("Consecutive match: " + nextMatch);
					consecutiveMatches.add(nextMatch);
					match = nextMatch;
					nextMatch = match.getNext();
				} while (nextMatch != null && nextMatch.isImmediatelyAfter(match));

				sliceMatches.add(new PatienceSliceMatch(subslice, consecutiveMatches));
			} else {
				// No consecutive matches, so just add this match
				sliceMatches.add(new PatienceSliceMatch(subslice, ImmutableList.of(match)));
			}

			// Calculate start of next slice, which starts the line after the match
			line1 = match.getLineNumber1() + 1;
			line2 = match.getLineNumber2() + 1;

			match = match.getNext();
		}

		// Iterate over each slice

		// Run in serial execution, since doesn't seem to help performance making parallel
		return sliceMatches.stream().flatMap(sm -> this.handleSliceMatch(sm).stream()).collect(Collectors.toList());
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
			lines.add(this.newEqualOrNormalizeEdit(this.getLines1().get(match.getLineNumber1()),
					this.getLines2().get(match.getLineNumber2())));
		}

		return lines;
	}

	private List<DiffEdit> matchHead(final PatienceSlice slice) {
		if (slice.isEmpty()) {
			return Collections.emptyList();
		}

		List<DiffEdit> head = new ArrayList<>();

		while (!slice.isEmpty()
				&& this.normalize(this.getText1(slice.getLow1()), this.getText2(slice.getLow2())).hasEqualText()) {
			head.add(this.newEqualOrNormalizeEdit(this.getLines1().get(slice.getLow1()),
					this.getLines2().get(slice.getLow2())));
			slice.incrementLows();
		}

		return head;
	}

	private List<DiffEdit> matchTail(final PatienceSlice slice) {
		if (slice.isEmpty()) {
			return Collections.emptyList();
		}

		// Since tail is iterated from the bottom of the file, moving up, need to add in reverse order
		// Note: Instead, plan to add in regular order and reverse at end
		List<DiffEdit> tail = new ArrayList<>();

		while (!slice.isEmpty() && this
				.normalize(this.getText1(slice.getHigh1() - 1), this.getText2(slice.getHigh2() - 1)).hasEqualText()) {
			slice.decrementHighs();
			tail.add(this.newEqualOrNormalizeEdit(this.getLines1().get(slice.getHigh1()),
					this.getLines2().get(slice.getHigh2())));
		}

		// Need to reverse the order, since added in regular order, with plan to reverse here at the end
		Collections.reverse(tail);
		return tail;
	}

	private List<PatienceMatch> uniqueMatchingLines(final PatienceSlice slice) {
		// Source: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/
		// Source code uses Ruby Hash which maintains insertion order
		// For Java, the corresponding is LinkedHashMap

		//        System.out.println("In uniqueMatchingLines");

		// Map from the line of text to information on the number of occurrences and first occurrences of the line in each slice
		// (this is used to determine if the line is unique in each file and where in the file the line is)
		HashMap<String, FrequencyCount> counts = new LinkedHashMap<>();

		// update the counts table for each line we see, incrementing the counts and storing the first line number at which we see each line

		// Iterate over the range of the slice for lines 1
		for (int n = slice.getLow1(); n < slice.getHigh1(); n++) {
			// 1/5/2019 - normalize text first
			String text = this.normalize(this.getText1(n), "").getText1();

			// Introduced variable to make lambda happy
			int lineNumber = n;

			// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
			counts.compute(text, (k, v) -> (v != null ? v : new FrequencyCount()).recordFoundInSlice1(lineNumber));
		}

		// Iterate over the range of the slice for lines 2
		for (int n = slice.getLow2(); n < slice.getHigh2(); n++) {
			// 1/5/2019 - normalize text first
			String text = this.normalize("", this.getText2(n)).getText2();

			// Introduced variable to make lambda happy
			int lineNumber = n;

			// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
			counts.compute(text, (k, v) -> (v != null ? v : new FrequencyCount()).recordFoundInSlice2(lineNumber));
		}

		return counts.values().stream()
				// Select only those entries whose occurrence counts are both 1
				.filter(FrequencyCount::isLineUnique)
				// Map these entries to a list of Match objects containing the line numbers from each document
				.map(c -> new PatienceMatch(c.getLineNumber1(), c.getLineNumber2())).collect(Collectors.toList());
	}

	private List<DiffEdit> fallbackDiff(final PatienceSlice slice) {
		//        System.out.println("Called fallbackDiff!");

		return this.fallbackDiffAlgorithm.apply(this.getLines1().subList(slice.getLow1(), slice.getHigh1()),
				this.getLines2().subList(slice.getLow2(), slice.getHigh2()));
	}
}
