package info.codesaway.bex.diff;

import static info.codesaway.bex.BEXPairUtilities.mapGet;
import static info.codesaway.bex.BEXSide.BEX_SIDES;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.util.BEXUtilities.firstNonNull;
import static info.codesaway.bex.util.BEXUtilities.not;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.IntPair;
import info.codesaway.bex.diff.patience.FrequencyCount;
import info.codesaway.bex.diff.patience.PatienceMatch;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringType;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionType;
import info.codesaway.bex.diff.substitution.java.ImportSameClassnameDiffType;
import info.codesaway.util.regex.MatchResult;
import info.codesaway.util.regex.Matcher;

// TODO: write unit tests to confirm everything works as expected
public final class DiffHelper {
	private DiffHelper() {
		throw new UnsupportedOperationException();
	}

	public static final BiFunction<String, String, DiffNormalizedText> WHITESPACE_NORMALIZATION_FUNCTION = DiffHelper::normalizeWhitespace;

	private static final ThreadLocal<Matcher> MULTIPLE_WHITESPACE_MATCHER = getThreadLocalMatcher("\\s++");

	private static final ThreadLocal<Matcher> NORMALIZE_WHITESPACE_MATCHER = getThreadLocalMatcher(
			"\\b \\B|\\B \\b|\\B \\B");

	/**
	 * Normalize whitespace
	 *
	 * @param leftLine
	 * @param rightLine
	 * @return
	 */
	private static DiffNormalizedText normalizeWhitespace(final String leftLine, final String rightLine) {
		String leftNormalizedText = leftLine.trim();
		String rightNormalizedText = rightLine.trim();

		// Normalize whitespace
		// TODO: optimize so doesn't use regex
		// Check what Eclipse's built in whitespace ignore does

		// Replace multiple whitespace with a single space
		leftNormalizedText = MULTIPLE_WHITESPACE_MATCHER.get().reset(leftNormalizedText).replaceAll(" ");
		rightNormalizedText = MULTIPLE_WHITESPACE_MATCHER.get().reset(rightNormalizedText).replaceAll(" ");

		leftNormalizedText = NORMALIZE_WHITESPACE_MATCHER.get().reset(leftNormalizedText).replaceAll("");
		rightNormalizedText = NORMALIZE_WHITESPACE_MATCHER.get().reset(rightNormalizedText).replaceAll("");

		return new DiffNormalizedText(leftNormalizedText, rightNormalizedText);
	}

	/**
	 * Function which takes text and does not normalize
	 *
	 * <p>Can be used for normalization function when want to compare text without normalizing it first</p>
	 */
	public static final BiFunction<String, String, DiffNormalizedText> NO_NORMALIZATION_FUNCTION = DiffNormalizedText::new;

	public static DiffNormalizedText normalize(final DiffEdit diffEdit,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return normalize(diffEdit.getLeftText(), diffEdit.getRightText(), normalizationFunction);
	}

	public static DiffNormalizedText normalize(final String leftText, final String rightText,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		return firstNonNull(normalizationFunction, NO_NORMALIZATION_FUNCTION).apply(leftText, rightText);
	}

	private static Map<DiffEdit, String> normalizeTexts(final List<DiffEdit> diffEdits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		Map<DiffEdit, String> result = new HashMap<>(diffEdits.size());

		//		Builder<DiffEdit, String> builder = ImmutableMap.builderWithExpectedSize(diffEdits.size());

		for (DiffEdit diffEdit : diffEdits) {
			String normalizedText = normalize(diffEdit.getFirstSide(), diffEdit.getText(), normalizationFunction);
			result.put(diffEdit, normalizedText);
			//			builder.put(diffEdit, normalizedText);
		}

		return Collections.unmodifiableMap(result);
		//		return builder.build();
	}

	public static String normalize(final BEXSide side, final String text,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return side == BEXSide.LEFT
				? normalize(text, "", normalizationFunction).getLeft()
				: normalize("", text, normalizationFunction).getRight();
	}

	/**
	 * Performs patience sort on the list of matches
	 *
	 * @param matches the list of matches (already sorted by left line number)
	 * @return the first match in a chain of matches (use {@link PatienceMatch#getNext()} to retrieve the whole chain)
	 */
	public static PatienceMatch patienceSort(final List<PatienceMatch> matches) {
		if (matches.isEmpty()) {
			return null;
		}

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

	/**
	 *
	 * @param stacks
	 * @param match
	 * @return the zero-based index of the stack to the <b>left</b> of where the match should be placed
	 */
	private static int binarySearch(final List<PatienceMatch> stacks, final PatienceMatch match) {
		// Reference: https://blog.jcoglan.com/2017/09/28/implementing-patience-diff/

		// Subtract 2 since want to get the stack to the left of where the match should be placed
		return -Collections.binarySearch(stacks, match, PatienceMatch.RIGHT_LINE_NUMBER_COMPARATOR) - 2;
	}

	/**
	*
	*
	* @param diff
	* @return
	*/
	public static List<DiffEdit> handleMovedLines(final List<DiffEdit> diff) {
		return handleMovedLines(diff, NO_NORMALIZATION_FUNCTION);
	}

	/**
	 * Modifies the passed list of DiffEdit to handle moved lines
	 *
	 * @param diff
	 * @return the modified passed list (same reference as parameter, returned to allow nested method calls)
	 */
	public static List<DiffEdit> handleMovedLines(final List<DiffEdit> diff,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		Map<Integer, DiffWithIndex> leftMap = createMap(diff, LEFT);
		Map<Integer, DiffWithIndex> rightMap = createMap(diff, RIGHT);

		boolean done;

		do {
			HashMap<String, FrequencyCount> counts = new LinkedHashMap<>();

			Stream<DiffWithIndex> stream = Stream.concat(leftMap.values().stream(), rightMap.values().stream());
			Iterable<DiffWithIndex> iterable = stream::iterator;

			for (DiffWithIndex iDiff : iterable) {
				BEXSide side = iDiff.getFirstSide();
				DiffLine line = iDiff.getLine(side).get();
				String text = normalize(side, line.getText(), normalizationFunction);

				counts.compute(text,
						(k, v) -> FrequencyCount.emptyIfNull(v).recordFoundInSlice(side, line.getNumber()));
			}

			List<FrequencyCount> movedUniqueLines = counts.values()
					.stream()
					.filter(FrequencyCount::isLineUnique)
					.collect(Collectors.toList());

			// TODO: support non-unique moved lines?
			// Look into histogram diff algorithm
			if (movedUniqueLines.isEmpty()) {
				break;
			}

			for (FrequencyCount count : movedUniqueLines) {
				int leftLine = count.getLeftLineNumber();
				int rightLine = count.getRightLineNumber();

				findMovedLines(diff, leftLine, rightLine, leftMap, rightMap, normalizationFunction);
			}

			done = true;

			// Check if previous / next lines are identical (if so, they are considered part of the move)
			for (FrequencyCount count : movedUniqueLines) {
				int leftLine = count.getLeftLineNumber();
				int rightLine = count.getRightLineNumber();

				if (findMovedLines(-1, diff, leftLine, rightLine, leftMap, rightMap, normalizationFunction)) {
					done = false;
				}

				if (findMovedLines(1, diff, leftLine, rightLine, leftMap, rightMap, normalizationFunction)) {
					done = false;
				}
			}
		} while (!done);

		return diff;
	}

	public static final ThreadLocal<Matcher> IMPORT_MATCHER = getThreadLocalMatcher(
			"^\\s*+import(?<static>\\s++static)?+\\s++(?<package>(?:[A-Za-z0-9]++\\.)+)?(?<class>[A-Z][A-Za-z0-9]*+)(?<method>\\.\\w++)?+;");

	// TODO: need to tweak to yield better changes
	/*
	public static List<DiffEdit> handleImports(final List<DiffEdit> diff) {
		List<DiffWithIndex> possibleImports = IntStream.range(0, diff.size())
				.mapToObj(i -> new DiffWithIndex(diff.get(i), i))
				.filter(DiffWithIndex::isInsertOrDelete)
				.filter(d -> d.getDiffEdit().getText().trim().startsWith("import"))
				.collect(toList());

		// Group imports by classname
		Map<String, List<DiffWithIndex>> importsByClassName = new HashMap<>();
		Map<DiffWithIndex, MatchResult> matchResults = new HashMap<>();

		for (DiffWithIndex possibleImport : possibleImports) {
			Matcher matcher = IMPORT_MATCHER.get().reset(possibleImport.getDiffEdit().getText());

			if (!matcher.find()) {
				continue;
			}

			matchResults.put(possibleImport, matcher.toMatchResult());
			importsByClassName.computeIfAbsent(matcher.group("class"), k -> new ArrayList<>()).add(possibleImport);
		}

		// Check results
		for (Entry<String, List<DiffWithIndex>> entry : importsByClassName.entrySet()) {
			List<DiffWithIndex> list = entry.getValue();
			if (list.size() != 2) {
				continue;
			}

			DiffWithIndex firstDiff = list.get(0);
			DiffWithIndex secondDiff = list.get(1);

			DiffWithIndex left = null;
			DiffWithIndex right = null;

			if (firstDiff.hasLeftLine()) {
				if (secondDiff.hasRightLine()) {
					left = firstDiff;
					right = secondDiff;
				}
			} else if (secondDiff.hasLeftLine()) {
				left = secondDiff;
				right = firstDiff;
			}

			if (left != null && right != null) {
				ImportSameClassnameDiffType diffType = determineImportSameClassnameDiffType(matchResults.get(left),
						matchResults.get(right), true);

				//				System.out.println(entry);

				if (diffType != null) {
					DiffEdit diffEdit = new DiffEdit(diffType, left.getLeftLine(), right.getRightLine());
					diff.set(left.getIndex(), diffEdit);
					diff.set(right.getIndex(), diffEdit);
				}
			}
		}

		return diff;
	}*/

	public static ImportSameClassnameDiffType determineImportSameClassnameDiffType(final BEXPair<String> checkPair,
			final boolean isMove) {

		if (checkPair.testOrBoth(Objects::isNull)) {
			return null;
		}

		BEXPair<MatchResult> matchResult = checkPair.map(DiffHelper::toImportMatchResult);

		if (matchResult.testOrBoth(not(MatchResult::matched))) {
			return null;
		}

		if (matchResult.testOrBoth(r -> !r.matched("class") || !r.matched("package"))) {
			return null;
		}

		if (matchResult.testOrBoth(r -> r.matched("static"))) {
			// For now, don't compare static imports
			return null;
		}

		BEXPair<String> className = matchResult.map(r -> r.get("class"));

		if (!className.test(Objects::equals)) {
			return null;
		}

		return new ImportSameClassnameDiffType(className.getRight(), matchResult.map(DiffHelper::getImportPackage),
				isMove);
	}

	private static MatchResult toImportMatchResult(final String text) {
		Matcher importMatcher = DiffHelper.IMPORT_MATCHER.get();
		importMatcher.reset(text).find();
		return importMatcher.toMatchResult();
	}

	private static String getImportPackage(final MatchResult importMatchResult) {
		String importPackage = importMatchResult.get("package");
		if (importPackage.endsWith(".")) {
			importPackage = importPackage.substring(0, importPackage.length() - 1);
		}

		return importPackage;
	}

	private static Map<Integer, DiffWithIndex> createMap(final List<DiffEdit> diff, final BEXSide side) {
		return IntStream.range(0, diff.size())
				.filter(i -> diff.get(i).getFirstSide() == side)
				.mapToObj(i -> new DiffWithIndex(diff.get(i), i))
				.collect(toMap(d -> d.getLine(d.getFirstSide()).get().getNumber(), Function.identity()));
	}

	private static boolean findMovedLines(final int direction, final List<DiffEdit> diff,
			final int initialLeftLine, final int initialRightLine,
			final Map<Integer, DiffWithIndex> leftMap, final Map<Integer, DiffWithIndex> rightMap,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		boolean foundMovedLines = false;
		int leftLineIndex = initialLeftLine + direction;
		int rightLineIndex = initialRightLine + direction;

		while (findMovedLines(diff, leftLineIndex, rightLineIndex, leftMap, rightMap, normalizationFunction)) {
			foundMovedLines = true;
			leftLineIndex += direction;
			rightLineIndex += direction;
		}

		return foundMovedLines;
	}

	/**
	 * Find moved lines and handle them if found
	 *
	 * @param diff
	 * @param leftLine
	 * @param rightLine
	 * @return <code>true</code> if found a moved line; <code>false</code> if did not find a moved line
	 */
	private static boolean findMovedLines(final List<DiffEdit> diff, final int leftLine, final int rightLine,
			final Map<Integer, DiffWithIndex> leftMap, final Map<Integer, DiffWithIndex> rightMap,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {

		Integer leftMapKey = leftLine;
		Integer rightMapKey = rightLine;

		DiffWithIndex leftDiff = leftMap.get(leftMapKey);
		DiffWithIndex rightDiff = rightMap.get(rightMapKey);

		if (leftDiff == null || rightDiff == null) {
			// Line is not under consideration for move
			return false;
		}

		Optional<DiffLine> leftDiffLine = leftDiff.getLeftLine();
		Optional<DiffLine> rightDiffLine = rightDiff.getRightLine();

		// For it to be considered a move, it must have equal / normalized equal lines
		if (!normalize(leftDiffLine.get().getText(), rightDiffLine.get().getText(), normalizationFunction)
				.hasEqualText()) {
			return false;
		}

		diff.set(leftDiff.getIndex(), new DiffEdit(BasicDiffType.MOVE_LEFT, leftDiffLine, rightDiffLine));
		diff.set(rightDiff.getIndex(), new DiffEdit(BasicDiffType.MOVE_RIGHT, leftDiffLine, rightDiffLine));

		leftMap.remove(leftMapKey);
		rightMap.remove(rightMapKey);

		return true;
	}

	private static final ThreadLocal<Matcher> WORD_MATCHER = getThreadLocalMatcher("\\w++");

	/**
	 *
	 *
	 * @param diff
	 * @return
	 */
	public static List<DiffEdit> handleSubstitution(final List<DiffEdit> diff,
			final SubstitutionType... substitutionTypes) {
		return handleSubstitution(diff, NO_NORMALIZATION_FUNCTION, substitutionTypes);
	}

	/**
	 * Handle substitutions
	 *
	 * @param diff
	 * @param normalizationFunction
	 * @return
	 */
	public static List<DiffEdit> handleSubstitution(final List<DiffEdit> diff,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final SubstitutionType... substitutionTypes) {
		List<DiffEdit> diffEdits = new ArrayList<>();

		List<DiffEdit> results = new ArrayList<>();

		List<RefactoringType> refactoringTypes = Arrays.stream(substitutionTypes)
				.filter(RefactoringType.class::isInstance)
				.map(RefactoringType.class::cast)
				.collect(Collectors.toList());

		// Only need to handle substitution if has a left line and right line in the same block
		// TODO: if there are only left lines or right lines, may still be part of RefactoringType
		// TODO: how should moves be handled / ignored so could run handleMove before handleSubstitution if user desired
		boolean hasLeftLine = false;
		boolean hasRightLine = false;

		for (DiffEdit diffEdit : diff) {
			if (diffEdit.isInsertOrDelete()) {
				diffEdits.add(diffEdit);

				if (diffEdit.hasLeftLine()) {
					hasLeftLine = true;
				} else if (diffEdit.hasRightLine()) {
					hasRightLine = true;
				}
			} else {
				if (!diffEdits.isEmpty()) {
					results.addAll(hasLeftLine && hasRightLine
							? findSubstitutions(diffEdits, normalizationFunction, substitutionTypes, refactoringTypes)
							: diffEdits);

					// Reset values
					diffEdits.clear();
					hasLeftLine = false;
					hasRightLine = false;
				}

				results.add(diffEdit);
			}
		}

		if (!diffEdits.isEmpty()) {
			results.addAll(hasLeftLine && hasRightLine
					? findSubstitutions(diffEdits, normalizationFunction, substitutionTypes, refactoringTypes)
					: diffEdits);
		}

		// TODO: need to sort results
		// (though may not want to do it here, due to potential of reordering other lines)

		// Copy back to original parameter
		// (mimics other methods which process results in-line)
		diff.clear();
		diff.addAll(results);

		return diff;
	}

	/**
	 * Find substitutions
	 *
	 * @param diffEdits list of {@link BasicDiffType#INSERT} and {@link BasicDiffType#DELETE} to check for substitutions
	 * @param normalizationFunction
	 * @return
	 */
	private static List<DiffEdit> findSubstitutions(final List<DiffEdit> diffEdits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final SubstitutionType[] substitutionTypes, final List<RefactoringType> refactoringTypes) {

		Map<DiffEdit, String> normalizedTexts = normalizeTexts(diffEdits, normalizationFunction);

		// Initialize empty collections which are added to by the recursive call then used by this method to factor into the results
		Set<DiffEdit> matches = new HashSet<>();

		// 3/27/2019 - changed to be a map from the old diff to the replacement (null indicating to delete the entry)
		Map<DiffEdit, DiffEdit> replacements = new HashMap<>();

		findSubstitutionsRecursive(diffEdits, normalizedTexts, matches, replacements, normalizationFunction,
				substitutionTypes, refactoringTypes);

		List<DiffEdit> results = new ArrayList<>();

		for (DiffEdit diffEdit : diffEdits) {
			if (matches.contains(diffEdit)) {
				// Replacement will be the substitution or null to indicate the DiffEdit should be removed
				DiffEdit replacement = replacements.get(diffEdit);

				if (replacement != null) {
					results.add(replacement);
				} else if (!replacements.containsKey(diffEdit)) {
					// If there is no key for the DiffEdit, throw an error since this is not expected
					// (there should be a null value for the key to indicate that the DiffEdit has been substituted and has no replacement
					throw new AssertionError("Could not find replacement for " + diffEdit);
				}
			} else {
				// Check to see if a single side replacement
				DiffEdit replacement = replacements.get(diffEdit);

				results.add(replacement != null ? replacement : diffEdit);
			}
		}

		results = sort(results);
		return results;
	}

	/**
	 *
	 * @param diffEdits list of {@link BasicDiffType#INSERT} and {@link BasicDiffType#DELETE} to check for substitutions
	 * @param normalizedTexts
	 * @param matches
	 * @param replacements
	 * @param substitutionTypes
	 * @param refactoringTypes
	 */
	private static void findSubstitutionsRecursive(final List<DiffEdit> diffEdits,
			final Map<DiffEdit, String> normalizedTexts, final Set<DiffEdit> matches,
			final Map<DiffEdit, DiffEdit> replacements,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction,
			final SubstitutionType[] substitutionTypes, final List<RefactoringType> refactoringTypes) {
		// TODO: accept as parameter (to allow user defined traversal)
		Collection<BEXPair<DiffEdit>> checkPairs = calculateSimilarDiffEdits(diffEdits, normalizedTexts,
				!refactoringTypes.isEmpty());

		// Track which DiffEdit are already part of a substitution found in this method
		// TODO: see if can remove
		Set<DiffEdit> alreadyFound = new HashSet<>();
		Map<DiffEdit, Set<DiffEdit>> alreadyChecked = new HashMap<>();
		//		HashMultimap<DiffEdit, DiffEdit> alreadyChecked = HashMultimap.create();

		// 3/27/2019 - changed to be a map from the old diff to the replacement (null indicating to delete the entry)
		Map<DiffEdit, DiffEdit> potentialReplacements = new HashMap<>();

		List<PatienceMatch> patienceMatches = new ArrayList<>();

		for (BEXPair<DiffEdit> checkPair : checkPairs) {
			if (checkPair.testOrBoth(alreadyFound::contains)) {
				continue;
			}

			if (checkPair.test((l, r) -> !alreadyChecked.computeIfAbsent(l, x -> new HashSet<>()).add(r))) {
				continue;
			}

			// TODO: also consider lone left lines / right lines if not part of substitution
			// (this way can group with enhanced for refactoring the deleted local loop variable / value

			for (SubstitutionType substitutionType : substitutionTypes) {
				SubstitutionDiffType diffType = substitutionType.accept(checkPair, normalizedTexts,
						normalizationFunction);

				if (diffType != null) {
					if (!diffType.isSubstitution()) {
						throw new AssertionError("DiffType is not a substitution");
					}

					//					if (diffType.getSymbol() == 'S') {
					//						System.out.println(diffType + "\t" + substitutionType.getClass());
					//						System.out.println("L: " + left);
					//						System.out.println("R: " + right);
					//					}

					DiffEdit substitution = new DiffEdit(diffType, checkPair.mapWithSide(DiffEdit::getLine));

					checkPair.acceptWithSide((e, side) -> potentialReplacements.put(e, side == RIGHT
							? substitution
							: null));

					checkPair.acceptBoth(alreadyFound::add);

					patienceMatches.add(new PatienceMatch(substitution.getLeftLineNumber(),
							substitution.getRightLineNumber()));
					break;
				}
			}
		}

		// TODO: is there a better way to do this?
		if (!refactoringTypes.isEmpty()) {
			BEX_SIDES.acceptBoth(side -> {
				for (DiffEdit diffEdit : diffEdits) {
					if (!diffEdit.hasLine(side)) {
						continue;
					}

					if (alreadyFound.contains(diffEdit)) {
						continue;
					}

					for (RefactoringType refactoringType : refactoringTypes) {
						RefactoringDiffType diffType = refactoringType.acceptSingleSide(side, diffEdit,
								normalizedTexts, normalizationFunction);

						if (diffType != null) {
							DiffEdit refactoring = new DiffEdit(side, diffType, diffEdit.getLine(side).get());

							replacements.put(diffEdit, refactoring);
							alreadyFound.add(diffEdit);
						}
					}
				}
			});
		}

		if (potentialReplacements.isEmpty()) {
			// No replacements
			return;
		}

		BEXPair<NavigableMap<Integer, DiffEdit>> diffEditLines = new BEXPair<>(TreeMap::new);

		for (DiffEdit diffEdit : diffEdits) {
			BEX_SIDES.acceptBoth(side -> {
				if (diffEdit.hasLine(side)) {
					diffEditLines.get(side).put(diffEdit.getLineNumber(side), diffEdit);
				}
			});
		}

		// Sort by left line number (so can use patience sort against right line and find longest increasing subsequence)
		patienceMatches.sort(PatienceMatch.LEFT_LINE_NUMBER_COMPARATOR);

		PatienceMatch match = patienceSort(patienceMatches);

		// Use to allow looping one extra time to find anything after the last match
		// (reduced amount of code necessary)
		PatienceMatch nextMatch = match;

		BEXPair<Integer> previousMatchLine = BEXPair.of(Integer.MIN_VALUE);

		// TODO: change to while loop, to make more obvious that looping while match isn't null
		while (match != null) {
			//		do {
			match = nextMatch;

			BEXPair<Integer> matchLine;
			if (match != null) {
				matchLine = match.getLineNumber().toBEXPair();

				BEXPair<DiffEdit> diffEdit = mapGet(diffEditLines, matchLine);

				diffEdit.acceptBoth(matches::add);
				diffEdit.acceptBoth(e -> replacements.put(e, potentialReplacements.get(e)));
			} else {
				matchLine = BEXPair.of(Integer.MAX_VALUE);
			}

			// Find lines between matches that are not already part of a match
			// See if any of these can be marked as substitution
			// (mainly affects lines without words, but may affect lines where multiple matches were possible and didn't chose correct one)
			// (this logic uses other substitutions to create a partition to find matches within - similar to patience diff)

			// Variable introduced to make lambda happy
			BEXPair<Integer> prevMatchLine = previousMatchLine;

			BEXPair<Collection<DiffEdit>> subDiffEdits = diffEditLines.mapWithSide((m, side) -> m
					.subMap(prevMatchLine.get(side), false, matchLine.get(side), false)
					.values());

			if (subDiffEdits.testAndBoth(not(Collection::isEmpty))) {
				// Only need to check if have left lines and right lines available
				// TODO: handle refactorings if only has left / right lines (such as enhanced for each deletes local variable)

				List<DiffEdit> combinedDiffs = new ArrayList<>(
						subDiffEdits.getLeft().size() + subDiffEdits.getRight().size());

				combinedDiffs.addAll(subDiffEdits.getLeft());
				combinedDiffs.addAll(subDiffEdits.getRight());

				findSubstitutionsRecursive(combinedDiffs, normalizedTexts, matches, replacements, normalizationFunction,
						substitutionTypes, refactoringTypes);
			}

			if (match == null) {
				break;
			}

			previousMatchLine = matchLine;

			//			if (match != null) {
			// Iterate to next match
			nextMatch = match.getNext();
			//			}

			// Look while the current match is not null
			// If the next match is null, loop one last time to allow finding any remaining items after last match
			// (so can see if they can be handled via simple substitution)
			//		} while (match != null);
		}
	}

	// Handle in order, which yields correct results for refactoring that contains state based on the text order
	private static final ToIntFunction<BEXPair<DiffEdit>> EARLIER_LEFT_LINE_FUNCTION = p -> p.getLeft()
			.getLeftLineNumber();
	private static final ToIntFunction<BEXPair<DiffEdit>> EARLIER_RIGHT_LINE_FUNCTION = p -> p.getRight()
			.getRightLineNumber();

	private static final Comparator<BEXPair<DiffEdit>> SIMILAR_DIFFEDIT_COMPARATOR = Comparator
			.comparingInt(EARLIER_LEFT_LINE_FUNCTION)
			.thenComparingInt(EARLIER_RIGHT_LINE_FUNCTION);

	/**
	 *
	 * @param diffEdits list of {@link BasicDiffType#INSERT} and {@link BasicDiffType#DELETE} to check for substitutions
	 * @param normalizedTexts
	 * @return
	 */
	private static Collection<BEXPair<DiffEdit>> calculateSimilarDiffEdits(final List<DiffEdit> diffEdits,
			final Map<DiffEdit, String> normalizedTexts, final boolean includeSingleSideDiffEdit) {

		// TODO: how to use includeSingleSideDiffEdit?

		// Partition of similar DiffEdit
		// (records with the same key are "similar" to each other and are possible canidates for substitution)
		// (the goal is to reduce the number of lines being compared to try to find similar lines)
		//		HashMultimap<String, DiffEdit> similarDiffEdits = HashMultimap.create();
		Map<String, Collection<DiffEdit>> similarLefts = new HashMap<>();
		Map<String, Collection<DiffEdit>> similarRights = new HashMap<>();

		for (DiffEdit diffEdit : diffEdits) {
			if (!diffEdit.isInsertOrDelete()) {
				throw new AssertionError("Unexpected DiffType: " + diffEdit.getType());
			}

			boolean hasPriorMatch = false;
			String normalizedText = normalizedTexts.get(diffEdit);

			Map<String, Collection<DiffEdit>> similarDiffEdits = diffEdit.hasLeftLine() ? similarLefts : similarRights;

			// TODO: change to parameter or allow user to specify how determine "partitions" of similar DiffEdit
			Matcher matcher = WORD_MATCHER.get().reset(normalizedText);

			while (matcher.find()) {
				hasPriorMatch = false;
				similarDiffEdits.computeIfAbsent(matcher.group(), k -> new HashSet<>()).add(diffEdit);
				//				similarDiffEdits.put(matcher.group(), diffEdit);
			}

			if (!hasPriorMatch) {
				// Didn't find any match, group together so can find each other
				// This handles blank lines and lines without words (just symbols)
				similarDiffEdits.computeIfAbsent("", k -> new HashSet<>()).add(diffEdit);
				//				similarDiffEdits.put("", diffEdit);
			}
		}

		Set<BEXPair<DiffEdit>> results = new TreeSet<>(SIMILAR_DIFFEDIT_COMPARATOR);

		// Add pairs based on the partition
		for (Entry<String, Collection<DiffEdit>> entry : similarLefts.entrySet()) {
			Collection<DiffEdit> rightEdits = similarRights.get(entry.getKey());
			if (rightEdits == null) {
				// No right lines with this key
				continue;
			}

			Collection<DiffEdit> leftEdits = entry.getValue();
			for (DiffEdit left : leftEdits) {
				for (DiffEdit right : rightEdits) {
					BEXPair<DiffEdit> pair = new BEXPair<>(left, right);
					//					System.out.printf("Pair:%nL: %s%nR: %s%n", pair.getLeft(), pair.getRight());
					results.add(pair);
				}
			}
		}

		return results;
	}

	/**
	 * Sorts the specified diff (not in place)
	 *
	 * @param diffEdits
	 * @return the sorted DiffEdit
	 */
	// TODO: is this still useful / working as desired
	private static List<DiffEdit> sort(final List<DiffEdit> diff) {
		if (diff.size() <= 1) {
			// Already sorted
			return diff;
		}

		List<DiffEdit> leftDiffEditLines = diff.stream()
				.filter(DiffEdit::hasLeftLine)
				.sorted(DiffEdit.LEFT_LINE_NUMBER_COMPARATOR)
				.collect(Collectors.toList());

		List<DiffEdit> rightDiffEditLines = diff.stream()
				.filter(DiffEdit::hasRightLine)
				.sorted(DiffEdit.RIGHT_LINE_NUMBER_COMPARATOR)
				.collect(Collectors.toList());

		int leftIndex = 0;
		int rightIndex = 0;

		List<DiffEdit> results = new ArrayList<>();

		while (leftIndex < leftDiffEditLines.size() && rightIndex < rightDiffEditLines.size()) {
			DiffEdit leftDiffEdit = leftDiffEditLines.get(leftIndex);
			DiffEdit rightDiffEdit = rightDiffEditLines.get(rightIndex);

			//			System.out.println(leftDiffEdit + "\t" + leftDiffEdit.getType());
			//			System.out.println(rightDiffEdit + "\t" + rightDiffEdit.getType());

			if (leftDiffEdit.equals(rightDiffEdit)) {
				// TODO: when would this occur?
				results.add(leftDiffEdit);
				leftIndex++;
				rightIndex++;

				// TODO: is this move logic correct??
				// (plan to use it for all substitutions)
			} else if (rightDiffEdit.isMove()) {
				results.add(rightDiffEdit);
				rightIndex++;
			} else if (leftDiffEdit.isMove()) {
				results.add(leftDiffEdit);
				leftIndex++;
			} else if (rightDiffEdit.hasLeftLine()
					&& leftDiffEdit.getLeftLineNumber() < rightDiffEdit.getLeftLineNumber()) {
				// left edit is earlier
				results.add(leftDiffEdit);
				leftIndex++;
			} else if (leftDiffEdit.hasRightLine()
					&& rightDiffEdit.getRightLineNumber() < leftDiffEdit.getRightLineNumber()) {
				// right edit is earlier
				results.add(rightDiffEdit);
				rightIndex++;
			} else if (!rightDiffEdit.hasLeftLine()) {
				// Sort deletions before inserts
				results.add(leftDiffEdit);
				leftIndex++;
			} else {
				throw new AssertionError(String.format("Error while sorting%n%s%n%s", leftDiffEdit, rightDiffEdit));
			}
		}

		// Add in remaining lines
		for (; leftIndex < leftDiffEditLines.size(); leftIndex++) {
			results.add(leftDiffEditLines.get(leftIndex));
		}

		for (; rightIndex < rightDiffEditLines.size(); rightIndex++) {
			results.add(rightDiffEditLines.get(rightIndex));
		}

		return results;
	}

	/**
	 * Combines consecutive DiffEdit to form DiffBlock when possible
	 *
	 * <p>The returned list will consist of DiffUnit objects (either DiffBlock or DiffEdit)</p>
	 *
	 * @param diff
	 * @return
	 */
	public static List<DiffUnit> combineToDiffBlocks(final List<DiffEdit> diff) {
		return combineToDiffBlocks(diff, false);
	}

	/**
	 * Combines consecutive DiffEdit to form DiffBlock when possible
	 *
	 * <p>The returned list will consist of DiffUnit objects (either DiffBlock or DiffEdit)</p>
	 *
	 * @param diff
	 * @return
	 */
	public static List<DiffUnit> combineToDiffBlocks(final List<DiffEdit> diff, final boolean allowReplacements) {
		List<DiffUnit> diffBlocks = new ArrayList<>();

		for (int i = 0; i < diff.size(); i++) {
			DiffEdit diffEdit = diff.get(i);

			if (isNextDiffPartOfBlock(diff, i, allowReplacements)) {
				DiffType blockDiffType = diffEdit.getType();

				List<DiffEdit> edits = new ArrayList<>();
				edits.add(diffEdit);

				do {
					DiffEdit nextDiffEdit = diff.get(++i);
					edits.add(nextDiffEdit);
					if (blockDiffType != REPLACEMENT_BLOCK && !blockDiffType.equals(nextDiffEdit.getType())) {
						blockDiffType = REPLACEMENT_BLOCK;
					}
				} while (isNextDiffPartOfBlock(diff, i, allowReplacements));

				diffBlocks.add(new DiffBlock(blockDiffType, edits));
			} else {
				diffBlocks.add(diffEdit);
			}
		}

		return diffBlocks;
	}

	/**
	 * Indicates if the next DiffEdit can be part of the same block as the current DiffEdit.
	 *
	 * @param diff the <code>DiffEdit</code>s
	 * @param i the index of the current DiffEdit
	 * @param allowReplacements
	 * @return
	 */
	private static boolean isNextDiffPartOfBlock(final List<DiffEdit> diff, final int i,
			final boolean allowReplacements) {

		if (i + 1 >= diff.size()) {
			return false;
		}

		DiffEdit diffEdit = diff.get(i);
		DiffEdit nextDiffEdit = diff.get(i + 1);

		boolean isReplancement = false;

		if (!nextDiffEdit.getType().equals(diffEdit.getType())) {
			if (!allowReplacements) {
				return false;
			}

			isReplancement = canBePartOfReplacement(diffEdit) && canBePartOfReplacement(nextDiffEdit);

			if (!isReplancement) {
				return false;
			}
		}

		return hasConsecutiveLines(diffEdit, nextDiffEdit, isReplancement);
	}

	private static boolean canBePartOfReplacement(final DiffEdit diffEdit) {
		return diffEdit.isInsertOrDelete() || diffEdit.isSubstitution();
	}

	/**
	 *
	 * @param diffEdit
	 * @param nextDiffEdit
	 * @param isReplancement
	 * @return
	 */
	public static boolean hasConsecutiveLines(final DiffEdit diffEdit, final DiffEdit nextDiffEdit,
			final boolean isReplancement) {
		return isConsecutive(diffEdit.getLineNumber(), nextDiffEdit.getLineNumber(), isReplancement);
	}

	/**
	 *
	 * @param lineNumberPair
	 * @param nextLineNumberPair
	 * @param isReplancement
	 * @return
	 */
	public static boolean isConsecutive(final IntPair lineNumberPair, final IntPair nextLineNumberPair,
			final boolean isReplancement) {
		return isConsecutive(BEXSide.LEFT, lineNumberPair, nextLineNumberPair, isReplancement)
				&& isConsecutive(BEXSide.RIGHT, lineNumberPair, nextLineNumberPair, isReplancement);
	}

	/**
	 *
	 * @param side
	 * @param lineNumberPair
	 * @param nextLineNumberPair
	 * @param isReplacement
	 * @return
	 * @since 0.4
	 */
	public static boolean isConsecutive(final BEXSide side, final IntPair lineNumberPair,
			final IntPair nextLineNumberPair, final boolean isReplacement) {
		int lineNumber = lineNumberPair.get(side);
		int nextLineNumber = nextLineNumberPair.get(side);

		return (nextLineNumber == lineNumber + 1
				// Both are inserts or both are deletes (other side will dictate if consecutive)
				|| lineNumber == -1 && nextLineNumber == -1
				// For replacements, inserts and deletes could be next to each other
				// In this case, there's not enough information to determine if they are consecutive
				// Presume they are consecutive since they appear after eachother in the diff
				|| isReplacement && (lineNumber == -1 || nextLineNumber == -1));
	}

	public static void handleSplitLines(final List<DiffUnit> diffUnits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		for (int i = 0; i < diffUnits.size(); i++) {
			DiffUnit block = diffUnits.get(i);

			// Only consider if substitution / replacement block
			// (if already equal / normalized equal, nothing to do
			// (if insert / delete, nothing to do)
			if (!(block instanceof DiffBlock) || !block.isSubstitution()) {
				continue;
			}

			StringBuilder normalizedLeftTextBuilder = new StringBuilder();
			StringBuilder normalizedRightTextBuilder = new StringBuilder();

			// First handling simple case where entire block is identical text, just split into multiple lines
			// TODO: find example where parts of block are identical text (or similar text) and handle

			//			System.out.println("Maybe split lines?");
			boolean hasEntry = false;
			for (DiffEdit diffEdit : block.getEdits()) {
				if (diffEdit.shouldTreatAsNormalizedEqual()) {
					continue;
				}

				DiffNormalizedText normalizedText = normalize(diffEdit, normalizationFunction);
				//				System.out.println(diffEdit.toString(true));
				hasEntry = true;

				if (diffEdit.hasLeftLine()) {
					normalizedLeftTextBuilder.append(normalizedText.getLeft());
				}

				if (diffEdit.hasRightLine()) {
					normalizedRightTextBuilder.append(normalizedText.getRight());
				}
			}

			// TODO: see what impact normalizing the text again has
			// No impact based on how I implemented the normalization function

			// Identical text split across lines
			if (hasEntry && normalizedLeftTextBuilder.toString().equals(normalizedRightTextBuilder.toString())) {
				// Mark entire block as normalized
				diffUnits.set(i, new DiffBlock(BasicDiffType.NORMALIZE, block.getEdits()));
			}
		}
	}

	//	public static void ignoreBlankLines(final List<DiffEdit> diff) {
	//		// Ignore blank lines
	//		for (int i = 0; i < diff.size(); i++) {
	//			DiffEdit diffEdit = diff.get(i);
	//
	//			if (diffEdit.isInsertOrDelete() && diffEdit.getLeftText().trim().isEmpty()
	//					&& diffEdit.getRightText().trim().isEmpty()) {
	//				diff.set(i, new DiffEdit(BasicDiffType.IGNORE, diffEdit.getLeftLine(), diffEdit.getRightLine()));
	//			}
	//		}
	//	}

	/**
	 * Handle blank lines
	 *
	 * @param diffUnits
	 * @param normalizationFunction
	 */
	public static void handleBlankLines(final List<DiffUnit> diffUnits,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		for (int i = 0; i < diffUnits.size(); i++) {
			DiffUnit unit = diffUnits.get(i);

			// If all lines in the unit are blank, mark the entire unit as normalized equal
			boolean containsOnlyBlankLines = unit.getEdits()
					.stream()
					.map(d -> normalize(d, normalizationFunction))
					.allMatch(n -> n.getLeft().isEmpty() && n.getRight().isEmpty());

			if (containsOnlyBlankLines) {
				diffUnits.set(i, new DiffBlock(BasicDiffType.NORMALIZE, unit.getEdits()));
			}
		}
	}
}
