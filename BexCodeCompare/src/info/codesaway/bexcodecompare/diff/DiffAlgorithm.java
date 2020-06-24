package info.codesaway.bexcodecompare.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.codesaway.bexcodecompare.diff.patience.FrequencyCount;
import info.codesaway.bexcodecompare.util.Utilities;

public interface DiffAlgorithm {
	/**
	 * Function which takes text and does not normalize (can be used for normalization function when want to compare text without normalizing it first)
	 */
	public static final BiFunction<String, String, DiffNormalizedText> NO_NORMALIZATION_FUNCTION = (a,
			b) -> new DiffNormalizedText(a, b);

	public List<DiffEdit> diff();

	/**
	 * Gets the normalization function
	 *
	 * <p>By default, returns {@link #NO_NORMALIZATION_FUNCTION}
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/05/2019  Initial code
	 *</pre>***********************************************************************************
	 */
	public default BiFunction<String, String, DiffNormalizedText> getNormalizationFunction() {
		return NO_NORMALIZATION_FUNCTION;
	}

	public default DiffNormalizedText normalize(final String text1, final String text2) {
		return normalize(text1, text2, this.getNormalizationFunction());
	}

	public static DiffNormalizedText normalize(final String text1, final String text2,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		BiFunction<String, String, DiffNormalizedText> usedNormalizationFunction = normalizationFunction != null
				? normalizationFunction : NO_NORMALIZATION_FUNCTION;

		return usedNormalizationFunction.apply(text1, text2);
	}

	/**
	 * Returns a new DiffEdit with DiffType of either {@link DiffTypeEnum#EQUAL} or {@link DiffTypeEnum#NORMALIZE} depending whether the specified text is equal or not
	 *
	 * <p>This can be used to easily create DiffEdit objects for equal text which may or may not have been normalized</p>
	 *
	 * @param line1 line1 (cannot be null)
	 * @param line2 line2 (cannot be null)
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/05/2019  Initial code
	 *</pre>***********************************************************************************
	 */
	public default DiffEdit newEqualOrNormalizeEdit(final DiffLine line1, final DiffLine line2) {
		if (line1 == null) {
			throw new NullPointerException("line1 cannot be null");
		}

		if (line2 == null) {
			throw new NullPointerException("line2 cannot be null");
		}

		DiffType diffType = line1.getText().equals(line2.getText()) ? DiffTypeEnum.EQUAL : DiffTypeEnum.NORMALIZE;

		return new DiffEdit(diffType, line1, line2);
	}

	/**
	*
	*
	* @param diff
	* @return
	* @since
	* <pre> Change History
	* ========================================================================================
	* Version  Change #        Developer           Date        Description
	* =======  =============== =================== ==========  ===============================
	* TRS.01T                  Amy Brennan-Luna    01/05/2019  Initial code
	*</pre>***********************************************************************************
	*/
	public static List<DiffEdit> handleMovedLines(final List<DiffEdit> diff) {
		return handleMovedLines(diff, NO_NORMALIZATION_FUNCTION);
	}

	/**
	 * Modifies the passed list of DiffEdit to handle moved lines
	 *
	 * @param diff
	 * @return the modified passed list (same reference as parameter, returned to allow nested method calls)
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/05/2019  Initial code
	 *</pre>***********************************************************************************
	 */
	public static List<DiffEdit> handleMovedLines(final List<DiffEdit> diff,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		// Use logic from Patience Diff to find identical unique lines which are different and mark them as moved
		// (logic from PatienceDiff.uniqueMatchingLines)

		// Create a map from line number in the file to the index in the diff list
		// Create one for file 1
		Map<Integer, Integer> lineNumberToDiffIndexMap1 = new HashMap<>();
		// Create a second for file 2
		Map<Integer, Integer> lineNumberToDiffIndexMap2 = new HashMap<>();

		for (int i = 0; i < diff.size(); i++) {
			DiffEdit diffEdit = diff.get(i);

			// Only check if line was moved for inserted / deleted lines
			// (for example, lines which are equal or normalized equal should not be considered)
			if (!Utilities.in(diffEdit.getType(), DiffTypeEnum.INSERT, DiffTypeEnum.DELETE)) {
				continue;
			}

			if (diffEdit.getOldLine().isPresent()) {
				lineNumberToDiffIndexMap1.put(diffEdit.getOldLineNumber(), i);
			}

			if (diffEdit.getNewLine().isPresent()) {
				lineNumberToDiffIndexMap2.put(diffEdit.getNewLineNumber(), i);
			}
		}

		//        List<DiffEdit> filteredDiff = diff
		//                .stream()
		//                // Only check if line was moved for inserted / deleted lines
		//                // (for example, lines which are equal or normalized equal should not be considered)
		//                .filter(de -> Utilities.in(de.getType(), DiffType.INSERT, DiffType.DELETE))
		//                .collect(Collectors.toList());

		// Store the list of filtered diffs
		// (stored the index of the entry in the original diff list)
		// (keeping a list of filtered diffs allows iterating a smaller list of values, versus looking at equal, normalized, and moved lines again)
		// Note: storing a list of integers, versus the actaul DiffEdit, since the move will create a new DiffEdit, so it's not noticed that a move occurred
		// Whereas, by storing the index, the DiffEdit in the diff list can be checked and when updated, will be noticed that it has become a MOVE
		List<Integer> filteredDiff = IntStream.range(0, diff.size())
				.filter(i -> Utilities.in(diff.get(i).getType(), DiffTypeEnum.INSERT, DiffTypeEnum.DELETE))
				// Box integers from int to Integer type
				.mapToObj(i -> i).collect(Collectors.toList());

		boolean done;

		do {
			//            System.out.println("Checking for moved lines");
			// Map from the line of text to information on the number of occurrences and first occurrences of the line in each slice
			// (this is used to determine if the line is unique in each file and where in the file the line is)
			// (reset counts on each loop)
			HashMap<String, FrequencyCount> counts = new LinkedHashMap<>();

			// update the counts table for each line we see, incrementing the counts and storing the first line number at which we see each line

			// Use iterator to allow removing instances as iterate if are not INSERT or DELETE (such as MOVE)
			for (Iterator<Integer> iterator = filteredDiff.iterator(); iterator.hasNext();) {
				int index = iterator.next();
				DiffEdit diffEdit = diff.get(index);
				if (!Utilities.in(diffEdit.getType(), DiffTypeEnum.INSERT, DiffTypeEnum.DELETE)) {
					// Only check if line was moved for inserted / deleted lines
					// (for example, lines which are equal or normalized equal should not be considered
					// (also lines which have already been marked as moved shouldn't be considered)
					iterator.remove();

					continue;
				}

				if (diffEdit.getOldLine().isPresent()) {
					DiffLine line = diffEdit.getOldLine().get();

					// 1/5/2019 - normalize text first
					String text = normalize(line.getText(), "", normalizationFunction).getText1();

					// Introduced variable to make lambda happy
					int lineNumber = line.getNumber();

					// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
					counts.compute(text,
							(k, v) -> (v != null ? v : new FrequencyCount()).recordFoundInSlice1(lineNumber));
				}

				if (diffEdit.getNewLine().isPresent()) {
					DiffLine line = diffEdit.getNewLine().get();

					// 1/5/2019 - normalize text first
					String text = normalize("", line.getText(), normalizationFunction).getText2();

					// Introduced variable to make lambda happy
					int lineNumber = line.getNumber();

					// Use Java 8 Map.compute to record that the line was found, creating a new PatienceCount if needed
					counts.compute(text,
							(k, v) -> (v != null ? v : new FrequencyCount()).recordFoundInSlice2(lineNumber));
				}
			}

			List<FrequencyCount> movedUniqueLines = counts.values().stream().filter(c -> c.isLineUnique())
					.collect(Collectors.toList());

			//            System.out.println(movedUniqueLines);
			if (movedUniqueLines.isEmpty()) {
				// No more moved lines
				break;
			}

			// First handle the unique moved lines
			for (FrequencyCount count : movedUniqueLines) {
				int line1 = count.getLineNumber1();
				int line2 = count.getLineNumber2();

				DiffHelper.handleMovedLines(diff, line1, line2, lineNumberToDiffIndexMap1, lineNumberToDiffIndexMap2);
			}

			// Then, check lines before / after the moved unique lines to see if they are identical
			// If identical, mark as moved lines - essentially part of a block move
			// Note: this is done separately to prevent extra loops through the whole diff
			// * Will mark look as not done if find any previous / next lines that can be treated as a move even though they are not unique
			// * If did this as part of the above loop, would accidently mark blocks of unique moved lines  as a reason to do an extra loop
			// (even though if the lines are unique, this won't allow newly found unique lines, since the unique lines form a closure
			// and additional matches is only possible if find previous / next lines not in this closure of unique lines)

			// Initially mark as done
			// (will be set to false if find additional lines, other than unique lines)
			// (this will occur only if find previous / next lines, as part of a block move)
			done = true;

			for (FrequencyCount count : movedUniqueLines) {
				int line1 = count.getLineNumber1();
				int line2 = count.getLineNumber2();

				// Look at previous lines (if any) to see if identical and part of this block move
				int previousLine1 = line1 - 1;
				int previousLine2 = line2 - 1;

				//				System.out.println("lineNumberToDiffIndexMap1: " + lineNumberToDiffIndexMap1);
				//				System.out.println("lineNumberToDiffIndexMap2: " + lineNumberToDiffIndexMap2);

				while (DiffHelper.handleMovedLines(diff, previousLine1, previousLine2, lineNumberToDiffIndexMap1,
						lineNumberToDiffIndexMap2)) {
					previousLine1--;
					previousLine2--;

					// Found additional lines, outside of the unique lines
					// (so continue looping, since may introduce new unique lines)
					done = false;
				}

				// Look at next lines (if any) to see if identical and part of this block move
				int nextLine1 = line1 + 1;
				int nextLine2 = line2 + 1;

				while (DiffHelper.handleMovedLines(diff, nextLine1, nextLine2, lineNumberToDiffIndexMap1,
						lineNumberToDiffIndexMap2)) {
					nextLine1++;
					nextLine2++;

					// Found additional lines, outside of the unique lines
					// (so continue looping, since may introduce new unique lines)
					done = false;
				}
			}

			// Was using entrySet to populate movedUniqueLines (for debug purposes)
			//                movedUniqueLines
			//                        .stream()
			//                        .forEach(e -> System.out.println("Check if has moved: " +
			//                                e.getKey() + "\t" + e.getValue().getLineNumber1() + "\t"
			//                                + e.getValue().getLineNumber2()));
		} while (!done);

		return diff;
	}

	/**
	 * Combines consecutive DiffEdit to form DiffBlock when possible
	 *
	 * <p>The returned list will consist of DiffUnit objects and may be combined back to a flat map using the following:</p>
	 *
	 *  // TODO: indicate how to using flatMap to make as a stream
	 * @param diff
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T
	 *</pre>***********************************************************************************
	 */
	public static List<DiffUnit> combineToDiffBlocks(final List<DiffEdit> diff) {
		return combineToDiffBlocks(diff, false);
	}

	/**
	 * Combines consecutive DiffEdit to form DiffBlock when possible
	 *
	 * <p>The returned list will consist of DiffUnit objects and may be combined back to a flat map using the following:</p>
	 *
	 *  // TODO: indicate how to using flatMap to make as a stream
	 * @param diff
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Added new parameter to combine replacements
	 *</pre>***********************************************************************************
	 */
	// 1M@TRS.01T
	public static List<DiffUnit> combineToDiffBlocks(final List<DiffEdit> diff, final boolean combineReplacements) {
		//	public static List<DiffUnit> combineToDiffBlocks(final List<DiffEdit> diff) {
		List<DiffUnit> diffBlocks = new ArrayList<>();

		for (int i = 0; i < diff.size(); i++) {
			DiffEdit diffEdit = diff.get(i);

			boolean partOfBlock = false;

			if (i + 1 < diff.size()) {
				// Check to see if can make block
				// TODO: continue here

				DiffEdit nextDiffEdit = diff.get(i + 1);

				// Check if next DiffEdit is the same type and immediately follows the current DiffEdit
				// (if so, it forms a block of changes)
				// 1M@TRS.01T
				if (isPartOfBlock(diffEdit, nextDiffEdit, combineReplacements)) {
					partOfBlock = true;

					// Check each following DiffEdit to see if part of block
					List<DiffEdit> edits = new ArrayList<>();
					edits.add(diffEdit);

					do {
						// Add the next diff, since it's part of the block
						edits.add(nextDiffEdit);

						// Increment, since checking the next set of consecutive differences to see if the immmediatly following each other
						i++;
						diffEdit = nextDiffEdit;

						if (i + 1 < diff.size()) {
							nextDiffEdit = diff.get(i + 1);
						} else {
							break;
						}
						// 1M@TRS.01T
					} while (isPartOfBlock(diffEdit, nextDiffEdit, combineReplacements));

					// If all edits are the same type, use that type
					// Otherwise, use DiffEditEnum.REPLACEMENT

					// 2A@TRS.01T
					DiffType diffType = edits.stream().map(e -> e.getType()).distinct().count() == 1
							? edits.get(0).getType() : DiffTypeEnum.REPLACEMENT;

					// 1M@TRS.01T
					diffBlocks.add(new DiffBlock(diffType, edits));
					//					diffBlocks.add(new DiffBlock(edits.get(0).getType(), edits));
				}
			}

			if (!partOfBlock) {
				// Since not part of block, add just the diff
				diffBlocks.add(diffEdit);
			}
		}

		return diffBlocks;
	}

	/**
	 * Indicates if the <code>nextDiffEdit</code> is the same type and immediately after the <code>diffEdit</code>.
	 *
	 * @param diffEdit
	 * @param nextDiffEdit
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/06/2019  Initial code
	 * TRS.02T					Amy Brennan-Luna	08/16/2019	Added new parameter to indicate if should combine replacements
	 *</pre>***********************************************************************************
	 */
	// 1M@TRS.02T
	static boolean isPartOfBlock(final DiffEdit diffEdit, final DiffEdit nextDiffEdit,
			final boolean combineReplacements) {
		//	static boolean isPartOfBlock(final DiffEdit diffEdit, final DiffEdit nextDiffEdit) {
		// Verify same type

		// 1A@TRS.02T
		boolean isReplancement = false;

		if (!nextDiffEdit.getType().equals(diffEdit.getType())) {

			// MTRS.02T start
			if (combineReplacements) {
				// Check if should be part of a replacement

				isReplancement = canBePartOfReplacement(diffEdit.getType())
						&& canBePartOfReplacement(nextDiffEdit.getType());

				if (!isReplancement) {
					return false;
				}
			} else {
				return false;
			}
			// M@TRS.02T end
		}

		int oldLineNumber = diffEdit.getOldLineNumber();
		int newLineNumber = diffEdit.getNewLineNumber();

		int nextOldLineNumber = nextDiffEdit.getOldLineNumber();
		int nextNewLineNumber = nextDiffEdit.getNewLineNumber();

		// Verify consecutive old lines
		boolean consecutiveOldLine = (oldLineNumber == -1 && nextOldLineNumber == -1
				|| nextOldLineNumber == oldLineNumber + 1
				// 1A@TRS.02T
				|| isReplancement && (oldLineNumber == -1 || nextOldLineNumber == -1));

		if (!consecutiveOldLine) {
			return false;
		}

		// Verify consecutive new lines
		boolean consecutiveNewLine = (newLineNumber == -1 && nextNewLineNumber == -1
				|| nextNewLineNumber == newLineNumber + 1
				// 1A@TRS.02T
				|| isReplancement && (newLineNumber == -1 || nextNewLineNumber == -1));

		if (!consecutiveNewLine) {
			return false;
		}

		return true;
	}

	static boolean canBePartOfReplacement(final DiffType diffType) {
		return Utilities.in(diffType, DiffTypeEnum.INSERT, DiffTypeEnum.DELETE) || diffType.isSubstitution();
	}

	/**
	 *
	 *
	 * @param diff
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Changed to add SubstitutionType parameter and rename method
	 *</pre>***********************************************************************************
	 */
	public static List<DiffEdit> handleSubstitution(final List<DiffEdit> diff,
			final SubstitutionType substitutionType) {
		return handleSubstitution(diff, NO_NORMALIZATION_FUNCTION, substitutionType);
		//		return handleSimpleSubstitution(diff, NO_NORMALIZATION_FUNCTION);
	}

	/**
	 * Handle simple substitutions
	 *
	 * @param diff
	 * @param normalizationFunction
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    03/25/2019  Initial coding
	 * TRS.02T					Amy Brennan-Luna	08/16/2019	Changed method signature to use new SubstitutionType
	 * 																(pass SubstitutionType.SIMPLE for prior functionality
	 *</pre>***********************************************************************************
	 */
	// 2M@TRS.02T
	public static List<DiffEdit> handleSubstitution(final List<DiffEdit> diff,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final SubstitutionType substitutionType) {
		//    public static List<DiffEdit> handleSimpleSubstitution(final List<DiffEdit> diff,
		//            final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		List<DiffEdit> diffEdits = new ArrayList<>();

		List<DiffEdit> results = new ArrayList<>();

		// Only need to handle substitution if has an insert and delete in the same block
		// (if only has inserts or only deletes, no substitution occurred in the block)
		boolean hasInsert = false;
		boolean hasDelete = false;

		//        int maxLine1 = Integer.MIN_VALUE;
		//        int maxLine2 = Integer.MIN_VALUE;

		for (DiffEdit diffEdit : diff) {
			//            if (Utilities.in(diffEdit.getType(), DiffType.EQUAL, DiffType.NORMALIZE)) {
			if (!Utilities.in(diffEdit.getType(), DiffTypeEnum.INSERT, DiffTypeEnum.DELETE)) {
				// If lines are equal or normalized equal, will start next block
				// (if lines are a different type than INSERT or DELETE, need to also start new block)
				// (this is the only way to ensure lines don't get reordered)
				// (for example, if moved blocks was handled before calling this method)
				if (!diffEdits.isEmpty() && hasInsert && hasDelete) {
					// Only handle diffs if there's both an insert and delete
					// (this will imply diffEdits is not empty, but added for clarity of code)

					results.addAll(DiffHelper.handleSubstitutionContainsBlock(diffEdits, normalizationFunction,
							substitutionType));
				} else {
					// Nothing to handle, add the existing diffs
					results.addAll(diffEdits);
				}

				results.add(diffEdit);

				// Reset values
				diffEdits.clear();
				hasInsert = false;
				hasDelete = false;

				//                maxLine1 = diffEdit.getOldLineNumber();
				//                maxLine2 = diffEdit.getNewLineNumber();

				continue;
			}

			// Should only handle inserted and deleted lines??
			// For example, if already indicated as moved or substitution, shouldn't do anything

			diffEdits.add(diffEdit);

			if (diffEdit.getType() == DiffTypeEnum.INSERT) {
				hasInsert = true;
			}

			if (diffEdit.getType() == DiffTypeEnum.DELETE) {
				hasDelete = true;
			}
		}

		if (!diffEdits.isEmpty() && hasInsert && hasDelete) {
			results.addAll(
					DiffHelper.handleSubstitutionContainsBlock(diffEdits, normalizationFunction, substitutionType));
		} else {
			// Nothing to handle, add the existing diffs
			results.addAll(diffEdits);
		}

		// Copy back to original parameter
		// (mimics other methods which process results in line)
		diff.clear();

		// TODO: need to sort results
		// (though may not want to do it here, due to potential of reordering other lines)
		// For example, in my test deleted line 102 is at the end but should now be earlier due to the substitutions

		// Sort by starting at smallest number on each side
		// Increment by 1 and see if has new line which matches
		// If so, this is next
		// If not, increment by 1 and see if has old line which matches

		diff.addAll(results);

		//		System.out.println("New line = 11232");
		//		results.stream()
		//				.filter(d -> d.getNewLineNumber() == 11232)
		//				.forEach(System.out::println);

		return diff;
	}

	public static void handleSplitLines(final List<DiffUnit> diffUnits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		for (int i = 0; i < diffUnits.size(); i++) {
			DiffUnit block = diffUnits.get(i);

			// Only output if substitution block
			// (if only a single statement then wouldn't need to check if combined lines would be similar to each other)

			// Only consider if substitution / replacement block
			// (if already equal / normalized equal, nothing to do
			// (if insert / delete, nothing to do)
			if (!(block instanceof DiffBlock) || !block.getType().isSubstitution()) {
				continue;
			}

			StringBuilder normalizedOldText = new StringBuilder();
			StringBuilder normalizedNewText = new StringBuilder();

			// First handling simple case where entire block is identical text, just split into multiple lines
			// TODO: find example where parts of block are identical text (or similar text) and handle
			for (DiffEdit diffEdit : block.getEdits()) {
				String text1 = diffEdit.getOldText();
				String text2 = diffEdit.getNewText();

				DiffNormalizedText normalizedText = normalizationFunction.apply(text1, text2);

				String normalizedText1 = normalizedText.getText1();
				String normalizedText2 = normalizedText.getText2();

				// Format with line numbers
				if (diffEdit.getOldLine().isPresent()) {
					normalizedOldText.append(normalizedText1);
				}

				if (diffEdit.getNewLine().isPresent()) {
					normalizedNewText.append(normalizedText2);
				}
			}

			// TODO: see what impact normalizing the text again has
			// No impact based on how I implemented the normalization function
			//			DiffNormalizedText normalizedTextResult = normalizationFunction.apply(normalizedOldText.toString(),
			//					normalizedNewText.toString());

			// Identical text split across lines
			//			if (normalizedTextResult.getText1().equals(normalizedTextResult.getText2())) {
			if (normalizedOldText.toString().equals(normalizedNewText.toString())) {
				// Should mark entire block as normalized
				// TODO: what should be done with the individual diff lines?
				// (should their type be changed as well?)

				diffUnits.set(i, new DiffBlock(DiffTypeEnum.NORMALIZE, block.getEdits()));
			}
		}
	}

	/**
	 * Handle blank lines
	 *
	 * @param diffUnits
	 * @param normalizationFunction
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Initial coding
	 *</pre>***********************************************************************************
	 */
	public static void handleBlankLines(final List<DiffUnit> diffUnits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		outer: for (int i = 0; i < diffUnits.size(); i++) {
			DiffUnit unit = diffUnits.get(i);

			// If all lines in the unit are blank, mark the entire unit as normalized equal

			for (DiffEdit diffEdit : unit.getEdits()) {
				String text1 = diffEdit.getOldText();
				String text2 = diffEdit.getNewText();

				DiffNormalizedText normalizedText = normalizationFunction.apply(text1, text2);

				String normalizedText1 = normalizedText.getText1();
				String normalizedText2 = normalizedText.getText2();

				if (!normalizedText1.isEmpty() || !normalizedText2.isEmpty()) {
					// One of the lines isn't empty, so entire block is not empty
					continue outer;
				}
			}

			// Entire block is empty
			diffUnits.set(i, new DiffBlock(DiffTypeEnum.NORMALIZE, unit.getEdits()));
		}
	}
}
