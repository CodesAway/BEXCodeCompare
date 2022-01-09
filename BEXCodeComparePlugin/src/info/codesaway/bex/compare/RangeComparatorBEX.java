package info.codesaway.bex.compare;

import static info.codesaway.bex.BEXPairs.bexPair;
import static info.codesaway.bex.BEXSide.BEX_SIDES;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.IntBEXRange.closedOpen;
import static info.codesaway.bex.diff.BasicDiffType.DELETE;
import static info.codesaway.bex.diff.BasicDiffType.EQUAL;
import static info.codesaway.bex.diff.BasicDiffType.IGNORE;
import static info.codesaway.bex.diff.BasicDiffType.INSERT;
import static info.codesaway.bex.diff.BasicDiffType.NORMALIZE;
import static info.codesaway.bex.diff.BasicDiffType.REPLACEMENT_BLOCK;
import static info.codesaway.bex.diff.substitution.SubstitutionType.SUBSTITUTION_CONTAINS;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_CAST;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_DIAMOND_OPERATOR;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_FINAL_KEYWORD;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_SEMICOLON;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_UNBOXING;
import static info.codesaway.bex.util.BEXUtilities.not;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import info.codesaway.bex.Activator;
import info.codesaway.bex.BEXListPair;
import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXPairValue;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.Indexed;
import info.codesaway.bex.IntBEXRange;
import info.codesaway.bex.IntPair;
import info.codesaway.bex.diff.DiffBlock;
import info.codesaway.bex.diff.DiffChange;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.DiffUnit;
import info.codesaway.bex.diff.NormalizationFunction;
import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.substitution.SubstitutionType;
import info.codesaway.bex.diff.substitution.java.EnhancedForLoopRefactoring;
import info.codesaway.bex.parsing.BEXString;
import info.codesaway.bex.parsing.ParsingState;
import info.codesaway.bex.views.BEXView;
import info.codesaway.eclipse.compare.internal.DocLineComparator;
import info.codesaway.eclipse.compare.rangedifferencer.AbstractRangeDifferenceFactory;
import info.codesaway.eclipse.compare.rangedifferencer.RangeDifference;

public final class RangeComparatorBEX {
	private final BEXPair<DocLineComparator> comparator;
	private final boolean ignoreWhitespace;
	private final boolean isMirrored;

	private List<DiffUnit> diffBlocks;
	private Map<DiffUnit, CompareResultType> isChangedMap;

	public static RangeDifference[] findDifferences(final AbstractRangeDifferenceFactory factory,
			final IProgressMonitor pm, final DocLineComparator left, final DocLineComparator right,
			final boolean isMirrored, final boolean updateView) {
		RangeComparatorBEX bex = new RangeComparatorBEX(left, right, isMirrored);
		SubMonitor monitor = SubMonitor.convert(pm, "BEX Code Compare", 100);
		try {
			// TODO: how to use monitor?
			bex.computeDifferences(monitor.newChild(95), updateView);
			return bex.getDifferences(monitor.newChild(5), factory, updateView);
		} finally {
			if (pm != null) {
				pm.done();
			}
		}
	}

	public RangeComparatorBEX(final DocLineComparator comparator1, final DocLineComparator comparator2,
			final boolean isMirrored) {
		this.comparator = bexPair(comparator1, comparator2);
		this.ignoreWhitespace = comparator1.isIgnoreWhitespace() || comparator2.isIgnoreWhitespace();
		this.isMirrored = isMirrored;
	}

	private void computeDifferences(final SubMonitor newChild, final boolean updateView) {
		// Fix for issue #117
		if (this.comparator.testAndBoth(c -> c.getText().isEmpty())) {
			// This occurs when switch from BEX compare to another compare (such as Java compare)
			// In this case, don't need to do anything, so short-circuit

			// Initial values with empty data (to prevent NullPointerException in getDifferences)
			this.diffBlocks = Collections.emptyList();
			return;
		}

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = this.ignoreWhitespace
				? DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION
				: DiffHelper.NO_NORMALIZATION_FUNCTION;

		boolean ignoreComments = Activator.ignoreComments();

		BEXPair<BEXString> bexString = ignoreComments
				? this.comparator
						.map(DocLineComparator::getText)
						.map(BEXString::new)
				: null;

		// Create NormalizationFunction which ignores comments
		// For now, only using comment-aware NormalizationFunction for handling split lines
		// (should other code use this normalizer? - see if benefits diff compare)
		NormalizationFunction normalizationIgnoresComments = NormalizationFunction.indexedNormalization((l, r) -> {
			if (bexString != null) {
				BEXPair<IntBEXRange> lineRange = new BEXPairValue<>(this.determineLineRange(l, LEFT),
						this.determineLineRange(r, RIGHT));

				//				System.out.println("Line range: " + lineRange);

				return BEX_SIDES
						.map(side -> this.normalizeLine(lineRange.get(side), bexString.get(side)))
						.apply(normalizationFunction);
			}

			return normalizationFunction.apply(l.getValue(), r.getValue());
		});

		BEXListPair<DiffLine> lines = new BEXListPair<>(this.comparator.map(
				c -> IntStream.range(0, c.getRangeCount())
						.mapToObj(i -> new DiffLine(i + 1 + c.getLineOffset(), c.extract(i, false)))
						.collect(toList())));

		List<DiffEdit> diff = PatienceDiff.diff(lines.getLeft(), lines.getRight(), normalizationFunction,
				MyersLinearDiff.with(normalizationFunction));

		if (!this.isMirrored) {
			for (int i = 0; i < diff.size(); i++) {
				DiffEdit diffEdit = diff.get(i);

				if (!diffEdit.isInsertOrDelete()) {
					continue;
				}

				DiffType type = (diffEdit.getType() == INSERT ? DELETE : INSERT);
				DiffEdit newDiffEdit = new DiffEdit(type, diffEdit.getLeftLine(), diffEdit.getRightLine());

				diff.set(i, newDiffEdit);
			}
		}

		boolean shouldUseEnhancedCompare = Activator.shouldUseEnhancedCompare();

		if (shouldUseEnhancedCompare) {
			// Look first for common refactorings, so can group changes together
			// Issue #107 - moved JAVA_FINAL_KEYWORD before SUBSTITUTION_CONTAINS
			// so that adding / removing the final keyword is seen as a non-important change
			DiffHelper.handleSubstitution(diff, normalizationFunction, JAVA_SEMICOLON, JAVA_FINAL_KEYWORD,
					SUBSTITUTION_CONTAINS, new EnhancedForLoopRefactoring(), IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE,
					JAVA_UNBOXING, JAVA_CAST, JAVA_DIAMOND_OPERATOR, SubstitutionType.LCS_MAX_OPERATOR);
		} else {
			DiffHelper.handleSubstitution(diff, normalizationFunction, SUBSTITUTION_CONTAINS,
					new EnhancedForLoopRefactoring(), IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE,
					SubstitutionType.LCS_MAX_OPERATOR);
		}

		// Do separately, so LCS max can find better matches and do only run LCS min on leftovers
		DiffHelper.handleSubstitution(diff, normalizationFunction, SubstitutionType.LCS_MIN_OPERATOR);

		// Mark comments as ignored line, so can group together
		//		if (ignoreComments && bexString != null) {
		//			for (int i = 0; i < diff.size(); i++) {
		//				DiffEdit diffEdit = diff.get(i);
		//
		//				if (in(diffEdit.getType(), EQUAL, NORMALIZE)) {
		//					continue;
		//				}
		//
		//				boolean shouldIgnoreDiff = normalizationIgnoresComments.normalize(diffEdit).hasEqualText();
		//
		//				if (shouldIgnoreDiff) {
		//					diff.set(i, new DiffEdit(IGNORE, diffEdit.getLeftLine(), diffEdit.getRightLine()));
		//				}
		//			}
		//		}

		// Pass BiPredicate which indicates if can combine
		// (if true, okay to combine using existing logic)
		// (if false, should never combine, even if current logic would allow)
		// This way, can ensure we don't combine unimportant refactorings in the same change as important changes
		List<DiffUnit> diffBlocks = DiffHelper.combineToDiffBlocks(diff, true,
				// Fixes issue #103 - only combine if both diffs either should or should not
				// (what was happening was it was combining non-important changes with important changes)
				// (this caused Eclipse to show more differences than expected)
				(x, y) -> x.shouldTreatAsNormalizedEqual() == y.shouldTreatAsNormalizedEqual());

		Set<IntPair> ignoresLines = new HashSet<>();

		if (shouldUseEnhancedCompare && this.ignoreWhitespace) {
			DiffHelper.handleSplitLines(diffBlocks, normalizationFunction);

			if (ignoreComments) {
				ArrayList<DiffUnit> diffBlocksCopy = new ArrayList<>(diffBlocks);

				// If ignore comments, run split line logic through normalization which ignores comments
				// (if block type would be NORMALIZE when ignore comments, ignore all lines in block
				// (however, still show block in BEX view)
				// (this way, doesn't show in code compare, since not important, but shows in BEX view, since still a change)
				DiffHelper.handleSplitLines(diffBlocksCopy, normalizationIgnoresComments);

				for (DiffUnit diffUnit : diffBlocksCopy) {
					if (diffUnit.getType() == NORMALIZE) {
						for (DiffEdit diffEdit : diffUnit.getEdits()) {
							ignoresLines.add(diffEdit.getLineNumber());
						}
					}
				}
			}
		}

		// Handle ignoring blank lines or comments (if option is enabled)
		if (this.ignoreWhitespace || ignoreComments) {
			for (int i = 0; i < diffBlocks.size(); i++) {
				DiffUnit diffBlock = diffBlocks.get(i);
				List<DiffEdit> diffEdits = diffBlock.getEdits();
				// Null unless need to edit, then will be initialized with copy of diffEdits
				List<DiffEdit> edits = null;

				for (int j = 0; j < diffEdits.size(); j++) {
					DiffEdit diffEdit = diffEdits.get(j);

					if (diffEdit.getType() == IGNORE) {
						continue;
					}

					String leftTextTrimmed = diffEdit.getLeftText().trim();
					String rightTexTrimmed = diffEdit.getRightText().trim();

					boolean shouldIgnoreDiff = this.ignoreWhitespace
							&& diffEdit.isInsertOrDelete()
							&& leftTextTrimmed.isEmpty()
							&& rightTexTrimmed.isEmpty();

					if (!shouldIgnoreDiff && ignoresLines.contains(diffEdit.getLineNumber())) {
						shouldIgnoreDiff = true;
					}

					if (!shouldIgnoreDiff && normalizationIgnoresComments.normalize(diffEdit).hasEqualText()) {
						shouldIgnoreDiff = true;
					}

					if (shouldIgnoreDiff) {
						if (edits == null) {
							// Take a copy, so can change it
							edits = new ArrayList<>(diffEdits);
						}

						edits.set(j, new DiffEdit(IGNORE, diffEdit.getLeftLine(), diffEdit.getRightLine()));
					}
				}

				if (edits != null) {
					diffBlocks.set(i, new DiffBlock(diffBlock.getType(), edits));
				}
			}
		}

		DiffHelper.handleBlankLines(diffBlocks, normalizationFunction);

		List<DiffChange<BEXChangeInfo>> changes = new ArrayList<>();

		int index = 0;

		List<DiffUnit> whitespaceOnlyChanges = new ArrayList<>();
		List<DiffUnit> importOnlyChanges = new ArrayList<>();

		List<DiffUnit> currentChanges = new ArrayList<>();
		DiffType currentChangeType = null;
		boolean isCurrentChangeImportant = false;

		this.diffBlocks = diffBlocks;
		this.isChangedMap = new HashMap<>();

		for (DiffUnit diffBlock : diffBlocks) {
			DiffType blockType = diffBlock.getType();

			if (blockType == EQUAL) {
				this.isChangedMap.put(diffBlock, CompareResultType.EQUAL);
				continue;
			} else if (blockType == NORMALIZE && this.ignoreWhitespace) {
				this.isChangedMap.put(diffBlock, CompareResultType.EQUAL);
				continue;
			}

			if (!this.ignoreWhitespace) {
				// Check if lines are equal if ignore whitespace
				// (if so, add to whitespace only changes)

				// TODO: verify don't have the arguments accidently reversed
				boolean isOnlyWhitespaceChange = diffBlock.getEdits()
						.stream()
						.allMatch(e -> DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION.apply(e.getLeftText(),
								e.getRightText()).hasEqualText());

				if (isOnlyWhitespaceChange) {
					whitespaceOnlyChanges.add(diffBlock);
					this.isChangedMap.put(diffBlock, CompareResultType.CHANGE);
					continue;
				}
			}

			// All changes in block are import statements
			List<DiffEdit> nonIgnoredDiffEdits = diffBlock.getEdits()
					.stream()
					.filter(not(DiffUnit::shouldIgnore))
					.collect(toList());

			// TODO: if all differences are ignored, indicate block is not important
			// (should block not show as a change?)

			boolean isOnlyImportChange = !nonIgnoredDiffEdits.isEmpty() && nonIgnoredDiffEdits
					.stream()
					.allMatch(e -> DiffHelper.IMPORT_MATCHER.get().reset(e.getText()).find());

			if (isOnlyImportChange) {
				importOnlyChanges.add(diffBlock);
				this.isChangedMap.put(diffBlock, CompareResultType.CHANGE);
				continue;
			}

			DiffType type = diffBlock.getType();
			boolean hasChange;

			// Fix for issue #116
			//			if (!shouldUseEnhancedCompare) {
			//				hasChange = true;
			//			}
			if (nonIgnoredDiffEdits.isEmpty()) {
				hasChange = false;
			} else {
				hasChange = true;

				boolean noChangeSubstitution = diffBlock.getEdits()
						.stream()
						.map(DiffEdit::getType)
						.allMatch(DiffType::shouldTreatAsNormalizedEqual);

				if (noChangeSubstitution) {
					hasChange = false;
				}
			}

			this.isChangedMap.put(diffBlock, hasChange ? CompareResultType.CHANGE : CompareResultType.NOCHANGE);

			if (updateView) {
				boolean partOfCurrentChanges = type == currentChangeType && !DiffType.isBasicDiffType(type);

				if (partOfCurrentChanges) {
					currentChanges.add(diffBlock);

					if (hasChange) {
						isCurrentChangeImportant = true;
					}
				} else {
					if (!currentChanges.isEmpty()) {
						BEXChangeInfo info = new BEXChangeInfo(isCurrentChangeImportant, ++index);
						changes.add(new DiffChange<>(currentChangeType, currentChanges, info));
						currentChanges.clear();
					}

					// Reset values
					currentChangeType = type;
					currentChanges.add(diffBlock);
					isCurrentChangeImportant = hasChange;
				}
			}
		}

		if (updateView) {
			if (!currentChanges.isEmpty()) {
				BEXChangeInfo info = new BEXChangeInfo(isCurrentChangeImportant, ++index);
				changes.add(new DiffChange<>(currentChangeType, currentChanges, info));
			}

			if (!importOnlyChanges.isEmpty()) {
				DiffChange<BEXChangeInfo> importOnlyChange = new DiffChange<>(REPLACEMENT_BLOCK, importOnlyChanges,
						new BEXChangeInfo(false, "Import Declarations"));
				changes.add(0, importOnlyChange);
			}

			// If has whitespace only change, add to start of list
			if (!whitespaceOnlyChanges.isEmpty()) {
				DiffChange<BEXChangeInfo> whitespaceOnlyChange = new DiffChange<>(NORMALIZE, whitespaceOnlyChanges,
						new BEXChangeInfo(false, "Whitespace only change"));
				changes.add(0, whitespaceOnlyChange);
			}

			BEXView.show(changes);
		}
	}

	private String normalizeLine(final IntBEXRange lineRange, final BEXString bexString) {
		if (lineRange.isEmpty() || lineRange.getStart() < 0 || lineRange.getEnd() < 0) {
			return "";
		}

		//		System.out.println("Normalize line range: " + lineRange);

		BEXString bexText = bexString.substring(lineRange);
		int offset = bexText.getOffset();

		StringBuilder resultBuilder = new StringBuilder(bexText.length());

		for (int i = 0; i < bexText.length(); i++) {
			ParsingState state = bexString.getTextStateMap().get(i + offset);
			if (state != null && state.isComment()) {
				continue;
			} else {
				resultBuilder.append(bexText.charAt(i));
			}
		}

		String result = resultBuilder.toString();

		if (this.ignoreWhitespace) {
			result = result.trim();
		}

		return result;
	}

	//	private IntBEXRange determineLineRange(final DiffEdit diffEdit, final BEXSide side) {
	//		int lineNumber = diffEdit.getLineNumber(side) - 1 - this.comparator.get(side).getLineOffset();
	//		String text = diffEdit.getText(side);
	//		int tokenStart = this.comparator.get(side).getTokenStart(lineNumber);
	//
	//		return this.determineLineRange(text, tokenStart);
	//	}

	private IntBEXRange determineLineRange(final Indexed<String> indexedText, final BEXSide side) {
		if (indexedText.getIndex() == -1) {
			// Return an empty range
			return IntBEXRange.closedOpen(-1, -1);
		}

		int lineNumber = indexedText.getIndex() - 1 - this.comparator.get(side).getLineOffset();
		String text = indexedText.getValue();
		int tokenStart = this.comparator.get(side).getTokenStart(lineNumber);

		int lineEnd = tokenStart + text.length();

		return closedOpen(tokenStart, lineEnd);

		//		System.out.printf("Determine line range %d starting %d%n", lineNumber, tokenStart);
		//		System.out.printf("%s\t%s%n", indexedText, this.comparator.get(side).getLineOffset());

		//		return this.determineLineRange(text, tokenStart);
	}

	//	private IntBEXRange determineLineRange(final String text, final int tokenStart) {
	//		// Ignore leading and trailing whitespace
	//		int relativeInclusiveStart = 0;
	//		while (relativeInclusiveStart < text.length() && Character.isWhitespace(text.charAt(relativeInclusiveStart))) {
	//			relativeInclusiveStart++;
	//		}
	//
	//		int relativeInclusiveEnd = text.length() - 1;
	//		while (relativeInclusiveEnd > relativeInclusiveStart
	//				&& Character.isWhitespace(text.charAt(relativeInclusiveEnd))) {
	//			relativeInclusiveEnd--;
	//		}
	//
	//		// Inclusive on both ends
	//		int lineStart = tokenStart;
	//		// Don't include the line terminator for the line end
	//		int lineEnd = lineStart + relativeInclusiveEnd;
	//
	//		// Intentionally done after set lineEnd
	//		lineStart += relativeInclusiveStart;
	//
	//		if (lineStart > lineEnd) {
	//			// Entire line is whitespace
	//			lineStart = lineEnd;
	//		}
	//
	//		return IntBEXRange.closed(lineStart, lineEnd);
	//	}

	private RangeDifference[] getDifferences(final SubMonitor subMonitor,
			final AbstractRangeDifferenceFactory factory, final boolean includeNoChangeDiff) {
		try {
			List<RangeDifference> differences = new ArrayList<>();

			int priorRight = 0;
			int priorLeft = 0;

			for (DiffUnit diffUnit : this.diffBlocks) {
				List<DiffEdit> diffEdits = diffUnit.getEdits();

				// Subtract 1 to convert from 1-based line (used in displayed line number) to 0-based (used by factory)
				int rightStart = diffEdits.stream()
						.filter(DiffEdit::hasRightLine)
						.mapToInt(DiffEdit::getRightLineNumber)
						.min()
						.orElse(0) - 1;

				int leftStart = diffEdits.stream()
						.filter(DiffEdit::hasLeftLine)
						.mapToInt(DiffEdit::getLeftLineNumber)
						.min()
						.orElse(0) - 1;

				int rightLength;
				int leftLength;

				// My algorithm says line -1 if there's no corresponding
				// To correctly show the lines in Eclipse, want to instead show the last line
				// (so can indicate where the insert / delete occurred)
				if (rightStart != -1) {
					int maxRight = diffEdits.stream()
							.filter(DiffEdit::hasRightLine)
							.filter(not(DiffUnit::isMove))
							.mapToInt(DiffEdit::getRightLineNumber)
							.max()
							.orElse(0) - 1;

					priorRight = maxRight;
					rightLength = maxRight - rightStart + 1;
				} else {
					rightStart = priorRight + 1;
					rightLength = 0;
				}

				if (leftStart != -1) {
					int maxLeft = diffEdits.stream()
							.filter(DiffEdit::hasLeftLine)
							.filter(not(DiffUnit::isMove))
							.mapToInt(DiffEdit::getLeftLineNumber)
							.max()
							.orElse(0) - 1;

					priorLeft = maxLeft;
					leftLength = maxLeft - leftStart + 1;
				} else {
					leftStart = priorLeft + 1;
					leftLength = 0;
				}

				CompareResultType compareResultType = this.isChangedMap.get(diffUnit);

				// TODO: trying to work around bug in 3-way where shows
				if (compareResultType == CompareResultType.NOCHANGE && !includeNoChangeDiff) {
					continue;
				}

				if (compareResultType != CompareResultType.EQUAL) {
					int kind = compareResultType == CompareResultType.CHANGE
							? RangeDifference.CHANGE
							: RangeDifference.NOCHANGE;

					// Remove offset based on comparator offset
					// (so difference part shows correctly in compare view)

					rightStart -= this.comparator.getRight().getLineOffset();
					leftStart -= this.comparator.getLeft().getLineOffset();

					differences.add(factory.createRangeDifference(kind, rightStart, rightLength,
							leftStart, leftLength));
				}
			}

			return differences.toArray(new RangeDifference[differences.size()]);
		} finally {
			subMonitor.done();
		}
	}

	private void worked(final SubMonitor subMonitor, final int work) {
		if (subMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		subMonitor.worked(work);
	}
}