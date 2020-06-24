package info.codesaway.bexcodecompare.diff;

import static info.codesaway.bexcodecompare.util.RegexUtilities.getThreadLocalMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.collect.LinkedHashMultimap;

import info.codesaway.bexcodecompare.diff.SubstitutionContainsDiffType.Direction;
import info.codesaway.bexcodecompare.diff.patience.PatienceMatch;
import info.codesaway.bexcodecompare.parsing.LcsString;
import info.codesaway.bexcodecompare.util.Utilities;
import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

public class DiffHelper {
	private DiffHelper() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Performs patience sort on the list of matches
	 *
	 * @param matches the non-empty list of matches (must be non-empty)
	 * @return the first match in a chain of matches (use {@link PatienceMatch#getNext()} to find the whole chain)
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/31/2018  Initial code
	 * TRS.02T                  Amy Brennan-Luna    03/25/2019  Moved from PatienceDiff to DiffHelper
	 *</pre>***********************************************************************************
	 */
	public static PatienceMatch patienceSort(final List<PatienceMatch> matches) {
		// Not needed since handled before calling this
		//        if (matches.isEmpty())
		//        {
		//            return null;
		//        }
		//

		List<PatienceMatch> stacks = new ArrayList<>();

		for (PatienceMatch match : matches) {
			int i = binarySearch(stacks, match);

			if (i >= 0) {
				match.setPrevious(stacks.get(i));
			}

			if (i + 1 == stacks.size()) {
				// Add new entry to end of list
				stacks.add(match);
			} else {
				stacks.set(i + 1, match);
			}
		}

		// Get the last element
		PatienceMatch match = stacks.get(stacks.size() - 1);

		// Construct a forward chain of matches

		while (match.hasPrevious()) {
			match.getPrevious().setNext(match);

			match = match.getPrevious();
		}

		// Return the earliest match in the chain
		// Will then traverse through via match.getNext()
		return match;
	}

	private static int binarySearch(final List<PatienceMatch> stacks, final PatienceMatch match) {
		int low = -1;
		int high = stacks.size();

		while (low + 1 < high) {
			int mid = (low + high) / 2;
			if (stacks.get(mid).getLineNumber2() < match.getLineNumber2()) {
				low = mid;
			} else {
				high = mid;
			}
		}

		return low;
	}

	/**
	 * Handle moved lines
	 *
	 * @param diff
	 * @param line1
	 * @param line2
	 * @param lineNumberToDiffIndexMap1
	 * @param lineNumberToDiffIndexMap2
	 * @return <code>true</code> if found a moved line; <code>false</code> if did not find a moved line
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/06/2019  Initial code
	 * TRS.02T                  Amy Brennan-Luna    03/25/2019  Moved from DiffAlgorithm to DiffHelper utility class
	 * TRS.03T					Amy Brennan-Luna	08/17/2019	Fixed bug where wasn't verifying that lines were identical
	 *</pre>***********************************************************************************
	 */
	// TODO: better document and rename method
	public static boolean handleMovedLines(final List<DiffEdit> diff, final int line1, final int line2,
			final Map<Integer, Integer> lineNumberToDiffIndexMap1,
			final Map<Integer, Integer> lineNumberToDiffIndexMap2) {

		//		System.out.println("diff:");
		//		diff.stream().forEach(System.out::println);

		//		System.out.println("Line1: " + line1);
		//		System.out.println("Line2: " + line2);

		// Index in diff list to update
		Integer index1 = lineNumberToDiffIndexMap1.get(line1);
		Integer index2 = lineNumberToDiffIndexMap2.get(line2);

		if (index1 == null || index2 == null) {
			// Line is not under consideration for move
			return false;
		}

		int i1 = index1;
		int i2 = index2;

		// Deleted line (in first file, but not second)
		DiffEdit diffEdit1 = diff.get(i1);
		// Inserted line (in second file, but not first)
		DiffEdit diffEdit2 = diff.get(i2);

		//		System.out.println("diffEdit1:" + diffEdit1);
		//		System.out.println("diffEdit2:" + diffEdit2);

		if (diffEdit1.getType() == DiffTypeEnum.MOVE || diffEdit2.getType() == DiffTypeEnum.MOVE) {
			// Already found and marked as moved in prior look (part of an earlier block move)
			return false;
		}

		// A@TRS.03T - for it to be considered a move, it must have identical lines
		// (this wasn't being checked when did the before / after move
		if (!diffEdit1.getOldText().equals(diffEdit2.getNewText())) {
			return false;
		}

		// Indicate that line was moved
		// TODO: Is there a way to indicate how it moved
		// (so know it changed from Insert -> Moved or Deleted -> Moved?)
		// Is there value in knowing?)
		DiffEdit movedDiff = new DiffEdit(DiffTypeEnum.MOVE, diffEdit1.getOldLine(), diffEdit2.getNewLine());

		diff.set(i1, movedDiff);
		diff.set(i2, movedDiff);

		return true;
	}

	public static final Pattern WORD_PATTERN = Pattern.compile("\\w++");
	public static final ThreadLocal<Matcher> WORD_MATCHER = ThreadLocal.withInitial(() -> WORD_PATTERN.matcher(""));

	// TODO: make something to convert from syntax to regex, with spaces built-in
	// (this way, this becomes easier to read / understand)
	public static final ThreadLocal<Matcher> ENHANCED_FOR_LOOP_MATCHER = getThreadLocalMatcher("for\\s*+\\(\\s*+"
			+ "(?<type>\\w++)\\s++(?<element>\\w++)\\s*+" + ":\\s*+(?<iterable>\\w++)\\s*+" + "\\)\\s*+\\{\\s*+");

	public static final ThreadLocal<Matcher> INDEX_FOR_LOOP_MATCHER = getThreadLocalMatcher(
			"for\\s*+\\(\\s*+" + "(?<type>int)\\s++(?<element>\\w++)\\s*+=\\s*+0\\s*+;\\s*+"
					+ "\\k<element>\\s*+<\\s*+(?<iterable>\\w++)\\.(?:(?<collection>size)\\(\\)||(?<array>length))\\s*+;\\s*+"
					+ "\\k<element>\\+\\+" + "\\)\\s*+\\{\\s*+");

	private static final Comparator<DiffEdit> DIFF_EDIT_COMPARATOR = Comparator
			.comparing(DiffHelper::getDiffEditLineNumber);

	private static int getDiffEditLineNumber(final DiffEdit diffEdit) {
		return diffEdit.getOldLine().orElse(diffEdit.getNewLine().orElse(DiffEdit.ABSENT_LINE)).getNumber();
	}

	/**
	 * Handle simple substitution block
	 *
	 * <p>Check if an inserted line is a substring of a deleted line (or visa-versa); if it is, the line is a substitution
	 *
	 * @param diffEdits list of {@link DiffTypeEnum#INSERT} and {@link DiffTypeEnum#DELETE} to check for substitutions
	 * @param normalizationFunction
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    03/29/2019  Initial coding
	 * TRS.02T					Amy Brennan-Luna	08/16/2019	Added SubstitutionType parameter
	 *</pre>***********************************************************************************
	 */
	// 2M@TRS.02T
	public static List<DiffEdit> handleSubstitutionContainsBlock(final List<DiffEdit> diffEdits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final SubstitutionType substitutionType) {
		//    public static List<DiffEdit> handleSubstitutionContainsBlock(final List<DiffEdit> diffEdits,
		//            final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		// Process list and for each line
		// 1) Normalize the line based on the normalization function (which may do no normalization)
		// 2) Find "words" in the line and create a key using the text between consecutive words
		// This will be used to find similar lines

		// Map from DiffEdit to normalized text
		Map<DiffEdit, String> normalizedTexts = evaluateNormalizedTexts(diffEdits, normalizationFunction);

		// Initialize empty collections which are added to by the recursive call then used by this method to factor into the results
		Set<DiffEdit> matches = new HashSet<>();

		// 3/27/2019 - changed to be a map from the old diff to the replacement (null indicating to delete the entry)
		Map<DiffEdit, DiffEdit> replacements = new HashMap<>();

		findSubstitutionContainsRecursiveBlock(diffEdits, normalizedTexts, matches, replacements, substitutionType);

		List<DiffEdit> results = new ArrayList<>();

		for (DiffEdit diffEdit : diffEdits) {
			if (matches.contains(diffEdit)) {
				// Replacement will be the substitution or null to indicate the DiffEdit should be removed
				DiffEdit replacement = replacements.get(diffEdit);

				if (replacement != null) {
					results.add(replacement);
				} else if (!replacements.containsKey(diffEdit)) {
					// If there is no key for the DiffEdit, throw an error since this is not expected
					throw new AssertionError("Could not find replacement for " + diffEdit);
				}
			} else {
				results.add(diffEdit);
			}
		}

		results = sort(results);

		//        System.out.println("Substitution contains block)");
		//        results.stream().forEach(System.out::println);

		return results;
	}

	/**
	 *
	 *
	 * @param diffEdits
	 * @param normalizedTexts
	 * @param matches
	 * @param replacements2
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Added SubstitutionType parameter
	 * TRS.02T					Amy Brennan-Luna	08/17/2019	Don't indicate substitution if blank line aligns with line with text more than 3 characters
	 * TRS.03T					Amy Brennan-Luna	08/29/2019	Handle long lines such as 200,000+ characters on single line in
	 * 															ClaretyESBProject\ClaretyESBCommonServices\ESBMonitor\test\SoapUI Projects\ESBMonitor-Tests-soapui-project.xml
	 *</pre>***********************************************************************************
	 */
	// 3M@TRS.01T
	private static void findSubstitutionContainsRecursiveBlock(final List<DiffEdit> diffEdits,
			final Map<DiffEdit, String> normalizedTexts, final Set<DiffEdit> matches,
			final Map<DiffEdit, DiffEdit> replacements, final SubstitutionType substitutionType) {
		//	private static void findSubstitutionContainsRecursiveBlock(final List<DiffEdit> diffEdits,
		//			final Map<DiffEdit, String> normalizedTexts, final Set<DiffEdit> matches,
		//			final Map<DiffEdit, DiffEdit> replacements) {
		LinkedHashMultimap<String, DiffEdit> similarDiffEdits = calculateSimilarDiffEdits(diffEdits, normalizedTexts);

		// Track which DiffEdit are already part of a substitution found in this method
		Set<DiffEdit> alreadyFound = new HashSet<>();

		// 3/27/2019 - changed to be a map from the old diff to the replacement (null indicating to delete the entry)
		Map<DiffEdit, DiffEdit> potentialReplacements = new HashMap<>();

		// Map from line number to DiffEdit for that line
		// Uses
		// 1) When determine increasing list of matches, will be used to get the DiffEdit for the line to add to the results
		// 2) When determining remaining lines for recursive call to check for substitutions
		// Use a NavigableMap (like TreeSet) to allow using subMap to get lines between the substitution matches
		TreeMap<Integer, DiffEdit> diffEditLines1 = new TreeMap<>();
		TreeMap<Integer, DiffEdit> diffEditLines2 = new TreeMap<>();

		for (DiffEdit diffEdit : diffEdits) {
			if (diffEdit.getOldLine().isPresent()) {
				diffEditLines1.put(diffEdit.getOldLineNumber(), diffEdit);
			}

			if (diffEdit.getNewLine().isPresent()) {
				diffEditLines2.put(diffEdit.getNewLineNumber(), diffEdit);
			}
		}

		List<PatienceMatch> patienceMatches = new ArrayList<>();

		for (Map.Entry<String, Collection<DiffEdit>> entry : similarDiffEdits.asMap().entrySet()) {
			Collection<DiffEdit> values = entry.getValue();

			if (values.size() < 2) {
				// Need a pair (one insert and one delete for potential for substitution)
				continue;
			}

			List<DiffEdit> inserts = values.stream().filter(d -> d.getType() == DiffTypeEnum.INSERT)
					.filter(d -> !alreadyFound.contains(d)).collect(Collectors.toList());

			List<DiffEdit> deletes = values.stream().filter(d -> d.getType() == DiffTypeEnum.DELETE)
					.filter(d -> !alreadyFound.contains(d)).collect(Collectors.toList());

			if (inserts.isEmpty() || deletes.isEmpty()) {
				// Need a pair (one insert and one delete for potential for substitution)
				continue;
			}

			// Limit line length since LCS is O(n^2) for memory usage
			int lcsMaxLineLength = 150;

			outer: for (DiffEdit insert : inserts) {
				String normalizedInsert = normalizedTexts.get(insert);

				for (Iterator<DiffEdit> iterator = deletes.iterator(); iterator.hasNext();) {
					DiffEdit delete = iterator.next();
					String normalizedDelete = normalizedTexts.get(delete);

					// If equal lines, throw error, since should never occur
					// (should never occur, since would be equal lines, already indicated as such by the diff)
					if (insert.equals(delete)) {
						throw new AssertionError("Insert matches delete: " + insert);
					}

					boolean isSubstring = false;
					String prefix = "";
					Direction direction = null;
					String suffix = "";

					if (normalizedInsert.length() >= normalizedDelete.length()) {
						// Insert is longer, so check if contains delete
						// TODO: 3/26/2019 - use last index of, so if empty block of text, see entire line as prefix versus suffix
						int index = normalizedInsert.lastIndexOf(normalizedDelete);

						if (index != -1) {
							// Insert contains delete
							isSubstring = true;
							prefix = normalizedInsert.substring(0, index);
							direction = Direction.NEW_CONTAINS_OLD;
							suffix = normalizedInsert.substring(index + normalizedDelete.length());
						}
					} else {
						// Delete is longer, so check if contains insert
						// TODO: 3/26/2019 - use last index of, so if empty block of text, see entire line as prefix versus suffix
						int index = normalizedDelete.lastIndexOf(normalizedInsert);

						if (index != -1) {
							// Delete contains insert
							isSubstring = true;
							prefix = normalizedDelete.substring(0, index);
							direction = Direction.OLD_CONTAINS_NEW;
							suffix = normalizedDelete.substring(index + normalizedInsert.length());
						}
					}

					// A@TRS.02T
					//					if (delete.getOldLineNumber() == 69) {
					//						System.out.println("isSubstring: " + isSubstring);
					//						System.out.println("normalizedDelete: @" + normalizedDelete + "@");
					//						System.out.println("normalizedInsert: @" + normalizedInsert + "@");
					//						System.out.println("prefix: @" + prefix + "@");
					//						System.out.println("suffix: @" + suffix + "@");
					//					}

					int blankLineCompareThreshhold = 3;
					if (isSubstring && (normalizedDelete.isEmpty() || normalizedInsert.isEmpty())
							&& (prefix.length() > blankLineCompareThreshhold
									|| suffix.length() > blankLineCompareThreshhold)) {
						isSubstring = false;

						//						if (delete.getOldLineNumber() == 69) {
						//							System.out.println("Don't treat as substring");
						//						}
					}

					if (isSubstring) {
						// Indicate delete / insert as already found
						if (!alreadyFound.add(delete) || !alreadyFound.add(insert)) {
							// Throw error if they were previously found (should never occur)
							throw new AssertionError("Already found delete or insert: " + delete + "\t" + insert);
						}

						SubstitutionContainsDiffType diffType = new SubstitutionContainsDiffType(prefix, direction,
								suffix);

						// Store result to map and then retrieve it later
						DiffEdit substitution = new DiffEdit(diffType, delete.getOldLine(), insert.getNewLine());

						potentialReplacements.put(insert, substitution);
						potentialReplacements.put(delete, null);

						patienceMatches.add(new PatienceMatch(delete.getOldLineNumber(), insert.getNewLineNumber()));

						// Already found a substitution for the deleted diff, so remove it from future checks
						iterator.remove();

						continue outer;
					} else if (substitutionType != SubstitutionType.SIMPLE) {
						// Check if lines are similar

						if (normalizedDelete.length() > lcsMaxLineLength
								|| normalizedInsert.length() > lcsMaxLineLength) {
							// 5/10/2020 - changed to continue
							continue;
							//							return;
						}

						//						System.out.println("Check if similar");
						//						System.out.println(normalizedInsert.length());
						//						System.out.println(normalizedDelete.length());

						int lcsLength = new LcsString(normalizedInsert, normalizedDelete).lcsLength();

						//						System.out.println("LCS: " + insert + "\t" + delete + "\t" +
						//								lcsLength + "\t" + normalizedInsert.length() + "\t" + normalizedDelete.length());

						// TODO: move this as a parameter
						//						double threshhold = 1;
						double threshhold = 0.66;

						// 5/1/2020 - chaged to get max length, since goal is most of text should match
						if (lcsLength >= threshhold * Math.max(normalizedInsert.length(), normalizedDelete.length())
								// Don't treat as substitution if doesn't have any similiarities
								&& lcsLength > 0) {
							// Mark lines as similar

							// Indicate delete / insert as already found
							if (!alreadyFound.add(delete) || !alreadyFound.add(insert)) {
								// Throw error if they were previously found (should never occur)
								throw new AssertionError("Already found delete or insert: " + delete + "\t" + insert);
							}

							// Store result to map and then retrieve it later
							DiffEdit substitution = new DiffEdit(DiffTypeEnum.SUBSTITUTE, delete.getOldLine(),
									insert.getNewLine());

							potentialReplacements.put(insert, substitution);
							potentialReplacements.put(delete, null);

							patienceMatches
									.add(new PatienceMatch(delete.getOldLineNumber(), insert.getNewLineNumber()));

							// Already found a substitution for the deleted diff, so remove it from future checks
							iterator.remove();

							continue outer;
						}
					}
				}

				// If couldn't find matching line, try again, but check LCS based on min of normalized length (instead of max)
				if (substitutionType == SubstitutionType.ANY && normalizedInsert.length() <= lcsMaxLineLength) {
					for (Iterator<DiffEdit> iterator = deletes.iterator(); iterator.hasNext();) {
						DiffEdit delete = iterator.next();
						String normalizedDelete = normalizedTexts.get(delete);

						if (normalizedDelete.length() > lcsMaxLineLength) {
							continue;
						}

						int lcsLength = new LcsString(normalizedInsert, normalizedDelete).lcsLength();

						// TODO: move this as a parameter
						double threshhold = 0.66;

						// 5/12/2020 - for second pass, try min (since max didn't have any results)
						if (lcsLength >= threshhold * Math.min(normalizedInsert.length(), normalizedDelete.length())
								// Don't treat as substitution if doesn't have any similiarities
								&& lcsLength > 0) {
							// Mark lines as similar

							// Indicate delete / insert as already found
							if (!alreadyFound.add(delete) || !alreadyFound.add(insert)) {
								// Throw error if they were previously found (should never occur)
								throw new AssertionError("Already found delete or insert: " + delete + "\t" + insert);
							}

							// Store result to map and then retrieve it later
							DiffEdit substitution = new DiffEdit(DiffTypeEnum.SUBSTITUTE, delete.getOldLine(),
									insert.getNewLine());

							potentialReplacements.put(insert, substitution);
							potentialReplacements.put(delete, null);

							patienceMatches
									.add(new PatienceMatch(delete.getOldLineNumber(), insert.getNewLineNumber()));

							// Already found a substitution for the deleted diff, so remove it from future checks
							iterator.remove();

							continue outer;
						}
					}
				}

				if (Utilities.in(substitutionType, SubstitutionType.REFACTORING, SubstitutionType.ANY)) {
					for (Iterator<DiffEdit> iterator = deletes.iterator(); iterator.hasNext();) {
						DiffEdit delete = iterator.next();
						String normalizedDelete = normalizedTexts.get(delete);
						// Recognize common refactoring

						// Enhanced for loop

						Matcher enhancedForLoopMatcher = ENHANCED_FOR_LOOP_MATCHER.get();
						Matcher indexedForLoopMatcher = INDEX_FOR_LOOP_MATCHER.get();

						//						System.out.println("Maybe refactoring substitution?");
						//						System.out.println("Insert: " + normalizedInsert);
						//						System.out.println("Delete: " + normalizedDelete);

						//						System.out
						//								.println("Delete matches? " + indexedForLoopMatcher.reset(normalizedDelete).matches());
						//						System.out
						//								.println("Insert matches? " + enhancedForLoopMatcher.reset(normalizedInsert).matches());

						if (enhancedForLoopMatcher.reset(normalizedInsert).matches()
								&& indexedForLoopMatcher.reset(normalizedDelete).matches()
								|| enhancedForLoopMatcher.reset(normalizedDelete).matches()
										&& indexedForLoopMatcher.reset(normalizedInsert).matches()) {
							//							System.out.println("Maybe refactoring substitution?");
							//							System.out.println("Insert: " + normalizedInsert);
							//							System.out.println("Delete: " + normalizedDelete);

							// Verify iterable name matches
							if (enhancedForLoopMatcher.get("iterable").equals(indexedForLoopMatcher.get("iterable"))) {
								//								System.out.println("Refactoring substitution");
								//								System.out.println("Insert: " + normalizedInsert);
								//								System.out.println("Delete: " + normalizedDelete);

								// Store result to map and then retrieve it later
								DiffEdit substitution = new DiffEdit(DiffTypeEnum.SUBSTITUTE, delete.getOldLine(),
										insert.getNewLine());

								potentialReplacements.put(insert, substitution);
								potentialReplacements.put(delete, null);

								patienceMatches
										.add(new PatienceMatch(delete.getOldLineNumber(), insert.getNewLineNumber()));

								// Already found a substitution for the deleted diff, so remove it from future checks
								iterator.remove();

								continue outer;
							}
						}
					}
				}
			}
		}

		if (potentialReplacements.isEmpty()) {
			// No replacements
			return;
		}

		// Sort by line number 1 (so can use patience sort against line 2 and find longest increasing subsequence)
		patienceMatches.sort(Comparator.comparing(PatienceMatch::getLineNumber1));

		PatienceMatch match = DiffHelper.patienceSort(patienceMatches);

		// Use to allow looping one extra time to find anything after the last match
		// (reduced amount of code necessary)
		PatienceMatch nextMatch = match;

		Integer previousMatchLine1 = Integer.MIN_VALUE;
		Integer previousMatchLine2 = Integer.MIN_VALUE;

		do {
			match = nextMatch;

			if (match != null) {
				DiffEdit delete = diffEditLines1.get(match.getLineNumber1());
				DiffEdit insert = diffEditLines2.get(match.getLineNumber2());

				matches.add(delete);
				matches.add(insert);

				replacements.put(delete, potentialReplacements.get(delete));
				replacements.put(insert, potentialReplacements.get(insert));
			}

			// Find lines between matches that are not already part of a match
			// See if any of these can be marked as substitution
			// (mainly affects lines without words, but may affect lines where multiple matches were possible and didn't chose correct one)
			// (this logic uses other substitutions to create a partition to find matches within - similar to patience diff)
			Integer matchLine1 = match != null ? match.getLineNumber1() : Integer.MAX_VALUE;
			Integer matchLine2 = match != null ? match.getLineNumber2() : Integer.MAX_VALUE;

			List<DiffEdit> diffEdits1 = diffEditLines1.subMap(previousMatchLine1, false, matchLine1, false).values()
					.stream().collect(Collectors.toList());

			List<DiffEdit> diffEdits2 = diffEditLines2.subMap(previousMatchLine2, false, matchLine2, false).values()
					.stream().collect(Collectors.toList());

			if (!diffEdits1.isEmpty() && !diffEdits2.isEmpty()) {
				// Only need to check if have inserts and deletes available
				// (otherwise, the extra lines are just a block of inserts / deletes (no substitutions are possible)

				List<DiffEdit> combinedDiffs = new ArrayList<>(diffEdits1.size() + diffEdits2.size());
				combinedDiffs.addAll(diffEdits1);
				combinedDiffs.addAll(diffEdits2);

				// 1M@TRS.01T
				findSubstitutionContainsRecursiveBlock(combinedDiffs, normalizedTexts, matches, replacements,
						substitutionType);
				//				findSubstitutionContainsRecursiveBlock(combinedDiffs, normalizedTexts, matches, replacements);
			}

			previousMatchLine1 = matchLine1;
			previousMatchLine2 = matchLine2;

			if (match != null) {
				// Iterate to next match
				nextMatch = match.getNext();
			}

			// Look while the current match is not null
			// If the next match is null, loop one last time to allow finding any remaining items after last match
			// (so can see if they can be handled via simple substitution)
		} while (match != null);
	}

	private static Map<DiffEdit, String> evaluateNormalizedTexts(final List<DiffEdit> diffEdits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		Map<DiffEdit, String> normalizedTexts = new HashMap<>();

		for (DiffEdit diffEdit : diffEdits) {
			String normalizedText;

			if (diffEdit.getType() == DiffTypeEnum.DELETE) {
				normalizedText = DiffAlgorithm.normalize(diffEdit.getText(), "", normalizationFunction).getText1();
			} else if (diffEdit.getType() == DiffTypeEnum.INSERT) {
				normalizedText = DiffAlgorithm.normalize("", diffEdit.getText(), normalizationFunction).getText2();
			} else {
				throw new AssertionError("Unexpected diff type: " + diffEdit.getType() + "\t" + diffEdit);
			}

			normalizedTexts.put(diffEdit, normalizedText);
		}

		return normalizedTexts;
	}

	private static LinkedHashMultimap<String, DiffEdit> calculateSimilarDiffEdits(final List<DiffEdit> diffEdits,
			final Map<DiffEdit, String> normalizedTexts) {
		LinkedHashMultimap<String, DiffEdit> similarDiffEdits = LinkedHashMultimap.create();

		// Keep results of first match separate until end
		// (these will have more entries, and should be handled last, since will first try to find more specific match criteria)
		LinkedHashMultimap<String, DiffEdit> similarDiffEditsFirstMatch = LinkedHashMultimap.create();

		for (DiffEdit diffEdit : diffEdits) {
			boolean firstMatch = true;
			int priorWordStart = 0;
			String normalizedText = normalizedTexts.get(diffEdit);

			Matcher matcher = WORD_MATCHER.get().reset(normalizedText);

			while (matcher.find()) {
				if (firstMatch) {
					firstMatch = false;

					String key = matcher.group();
					similarDiffEditsFirstMatch.put(key, diffEdit);
					priorWordStart = matcher.start();

					continue;
				}

				// Key becomes the start of the prior word through the end of the current word
				// (creates a more unique key, used to help find matches faster)
				String key = normalizedText.substring(priorWordStart, matcher.end());
				similarDiffEdits.put(key, diffEdit);
				priorWordStart = matcher.start();
			}

			if (firstMatch) {
				// Didn't find any match, group together so can find each other
				// This handles blank lines and lines without words (just symbols)
				similarDiffEdits.put("", diffEdit);
			}
		}

		// Add all entries from similarDiffEditsFirstMatch to the end of similarDiffEdits
		similarDiffEdits.putAll(similarDiffEditsFirstMatch);

		return similarDiffEdits;
	}

	/**
	 * Sorts the specified DiffEdit (not in place)
	 *
	 * @param diffEdits
	 * @return the sorted DiffEdit
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T
	 *</pre>***********************************************************************************
	 */
	private static List<DiffEdit> sort(final List<DiffEdit> diffEdits) {
		if (diffEdits.size() <= 1) {
			// Already sorted
			return diffEdits;
		}

		List<DiffEdit> diffEditLines1 = new ArrayList<>();
		List<DiffEdit> diffEditLines2 = new ArrayList<>();

		for (DiffEdit diffEdit : diffEdits) {
			if (diffEdit.getOldLine().isPresent()) {
				diffEditLines1.add(diffEdit);
			}

			if (diffEdit.getNewLine().isPresent()) {
				diffEditLines2.add(diffEdit);
			}
		}

		diffEditLines1.sort(Comparator.comparing(DiffEdit::getOldLineNumber));
		diffEditLines2.sort(Comparator.comparing(DiffEdit::getNewLineNumber));

		int a = 0;
		int b = 0;

		List<DiffEdit> results = new ArrayList<>();

		while (a < diffEditLines1.size() && b < diffEditLines2.size()) {
			DiffEdit edit1 = diffEditLines1.get(a);
			DiffEdit edit2 = diffEditLines2.get(b);

			//            System.out.println("Testing sort");
			//            System.out.println(edit1);
			//            System.out.println(edit2);

			if (edit1.equals(edit2)) {
				results.add(edit1);
				a++;
				b++;
			}
			// TODO: is this move logic correct??
			// (won't have any impact yet, since not doing substitutions after move)
			// (though, plan to move this sort logic out and use it for all substitutions)
			else if (edit2.getType().isMove()) {
				results.add(edit2);
				b++;
			} else if (edit1.getType().isMove()) {
				results.add(edit1);
				a++;
			} else if (edit2.getOldLine().isPresent() && edit1.getOldLineNumber() < edit2.getOldLineNumber()) {
				// edit1 is earlier
				results.add(edit1);
				a++;
			} else if (edit1.getNewLine().isPresent() && edit2.getNewLineNumber() < edit1.getNewLineNumber()) {
				// edit2 is earlier
				results.add(edit2);
				b++;
			} else if (!edit2.getOldLine().isPresent()) {
				// Sort deletions before inserts
				results.add(edit1);
				a++;
			} else {
				throw new AssertionError(String.format("Error while sorting%n%s%n%s", edit1, edit2));
			}
		}

		// Add in remaining lines
		for (; a < diffEditLines1.size(); a++) {
			results.add(diffEditLines1.get(a));
		}

		for (; b < diffEditLines2.size(); b++) {
			results.add(diffEditLines2.get(b));
		}

		//        return diffEdits;
		return results;
	}
}
