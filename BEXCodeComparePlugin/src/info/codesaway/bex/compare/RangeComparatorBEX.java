package info.codesaway.bex.compare;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import info.codesaway.bex.Activator;
import info.codesaway.bex.diff.DiffBlock;
import info.codesaway.bex.diff.DiffChange;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.DiffUnit;
import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.substitution.SubstitutionType;
import info.codesaway.bex.diff.substitution.java.EnhancedForLoopRefactoring;
import info.codesaway.bex.views.BEXView;
import info.codesaway.eclipse.compare.internal.DocLineComparator;
import info.codesaway.eclipse.compare.rangedifferencer.AbstractRangeDifferenceFactory;
import info.codesaway.eclipse.compare.rangedifferencer.RangeDifference;

public class RangeComparatorBEX {
	private final DocLineComparator comparator1, comparator2;
	private final boolean ignoreWhitespace;
	private final boolean isMirrored;

	private List<DiffUnit> diffBlocks;
	private Map<DiffUnit, CompareResultType> isChangedMap;

	public static RangeDifference[] findDifferences(final AbstractRangeDifferenceFactory factory,
			final IProgressMonitor pm, final DocLineComparator left, final DocLineComparator right,
			final boolean isMirrored, final boolean updateView) {
		RangeComparatorBEX bex = new RangeComparatorBEX(left, right, isMirrored);
		SubMonitor monitor = SubMonitor.convert(pm, Messages.RangeComparatorLCS_0, 100);
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
		this.comparator1 = comparator1;
		this.comparator2 = comparator2;
		this.ignoreWhitespace = comparator1.isIgnoreWhitespace() || comparator2.isIgnoreWhitespace();
		this.isMirrored = isMirrored;
	}

	private void computeDifferences(final SubMonitor newChild, final boolean updateView) {

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = this.ignoreWhitespace
				? DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION
				: DiffHelper.NO_NORMALIZATION_FUNCTION;

		List<DiffLine> leftLines = IntStream.range(0, this.comparator1.getRangeCount())
				.mapToObj(i -> new DiffLine(i + 1 + this.comparator1.getLineOffset(),
						this.comparator1.extract(i, false)))
				.collect(toList());

		List<DiffLine> rightLines = IntStream.range(0, this.comparator2.getRangeCount())
				// Add 1 to line number, so matches displayed line numbers
				.mapToObj(i -> new DiffLine(i + 1 + this.comparator2.getLineOffset(),
						this.comparator2.extract(i, false)))
				.collect(toList());

		List<DiffEdit> diff = PatienceDiff.diff(leftLines, rightLines, normalizationFunction,
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

		// TODO: when and how should comments be ignored?
		// This inserting / deleting comments can be ignored
		// However, commenting out / uncommenting code I think is more important
		// Or, if the line was changed but was commented out before and after think can ignore
		// TODO: how to detect if line is comments (simple case is line starts with '//')
		// Block comments are more challenging
		// Might need to run full file check to identify lines which are comments
		// Can use logic from CASTLE Searching to identify commented out code versus regular code
		boolean ignoreComments = Activator.ignoreComments();

		if (shouldUseEnhancedCompare) {
			// Look first for common refactorings, so can group changes together
			DiffHelper.handleSubstitution(diff, normalizationFunction, JAVA_SEMICOLON, SUBSTITUTION_CONTAINS,
					new EnhancedForLoopRefactoring(), IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE, JAVA_UNBOXING, JAVA_CAST,
					JAVA_FINAL_KEYWORD, JAVA_DIAMOND_OPERATOR,
					SubstitutionType.LCS_MAX_OPERATOR);
		} else {
			DiffHelper.handleSubstitution(diff, normalizationFunction, SUBSTITUTION_CONTAINS,
					new EnhancedForLoopRefactoring(), IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE,
					SubstitutionType.LCS_MAX_OPERATOR);
		}

		// Do separately, so LCS max can find better matches and do only run LCS min on leftovers
		DiffHelper.handleSubstitution(diff, normalizationFunction, SubstitutionType.LCS_MIN_OPERATOR);

		// 7/8/2020 - don't allow replacements for DiffBlock (should yield better diff in Eclipse)
		// (should be helpful when ignore comments)
		// TODO: though, makes ignoring split line differences challenging
		// TODO: write logic that recognizes split lines even if not in blocks
		List<DiffUnit> diffBlocks = DiffHelper.combineToDiffBlocks(diff, true);
		//		List<DiffUnit> diffBlocks = DiffHelper.combineToDiffBlocks(diff, false);

		// Handle ignoring blank lines or comments (if option is enabled)
		if (this.ignoreWhitespace || ignoreComments) {
			for (int i = 0; i < diffBlocks.size(); i++) {
				DiffUnit diffBlock = diffBlocks.get(i);
				List<DiffEdit> diffEdits = diffBlock.getEdits();
				// Null unless need to edit, then will be initialized with copy of diffEdits
				List<DiffEdit> edits = null;

				for (int j = 0; j < diffEdits.size(); j++) {
					DiffEdit diffEdit = diffEdits.get(j);
					String leftTextTrimmed = diffEdit.getLeftText().trim();
					String rightTexTrimmed = diffEdit.getRightText().trim();

					boolean shouldIgnoreDiff = this.ignoreWhitespace
							&& diffEdit.isInsertOrDelete()
							&& leftTextTrimmed.isEmpty()
							&& rightTexTrimmed.isEmpty();

					if (!shouldIgnoreDiff && ignoreComments) {
						// Check if both sides are comments
						boolean isLineComment = true;

						// TODO: add support for recognizing block comments as well
						if (diffEdit.hasLeftLine()) {
							isLineComment &= leftTextTrimmed.startsWith("//");
						}

						if (diffEdit.hasRightLine()) {
							isLineComment &= rightTexTrimmed.startsWith("//");
						}

						if (isLineComment) {
							shouldIgnoreDiff = true;
						}
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

		if (shouldUseEnhancedCompare && this.ignoreWhitespace) {
			DiffHelper.handleSplitLines(diffBlocks, normalizationFunction);
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

			if (!shouldUseEnhancedCompare) {
				hasChange = true;
			} else if (nonIgnoredDiffEdits.isEmpty()) {
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
						BEXChangeInfo info = new BEXChangeInfo(isCurrentChangeImportant, "Change " + (++index));
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
				BEXChangeInfo info = new BEXChangeInfo(isCurrentChangeImportant, "Change " + (++index));
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

					rightStart -= this.comparator2.getLineOffset();
					leftStart -= this.comparator1.getLineOffset();

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