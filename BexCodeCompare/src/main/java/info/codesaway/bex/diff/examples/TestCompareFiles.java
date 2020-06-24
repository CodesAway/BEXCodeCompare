package info.codesaway.bex.diff.examples;

import static info.codesaway.bex.diff.substitution.SubstitutionType.SUBSTITUTION_CONTAINS;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_CAST;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_UNBOXING;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import info.codesaway.bex.diff.BasicDiffType;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffUnit;
import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionType;
import info.codesaway.bex.diff.substitution.java.RefactorEnhancedForLoop;
import info.codesaway.bex.util.Utilities;

public class TestCompareFiles {
	// Check out Eclipse code for refactor examples
	// ConvertLoopFix

	// TODO: hid the text "can you see the difference" in Java code
	// Show a regular diff which struggles and then show Bex leaving just the change we care about

	// TODO: enhance so creates before / after files which are aligned, so I can easily copy to Confluence
	// This way, I can easily make side-by-side compares without having to manually align the text
	// Essentially doing what WinMerge does, but adding in blank lines to new file
	// Also, has my smarter compare logic
	private static boolean showFullDiff = false;

	// Whether or not to show equal, normalized, and ignored lines
	private static boolean shouldShowEqualBlocks = false;

	// Whether to show differences which have no impact
	private static boolean showNoImpactChanges = false;

	// TODO: continue here
	// Moved line test
	// TODO: broke something, since doesn't work
	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/api/be/BEMbrStmt.java";

	// BaseAction has local changes but want to ignore ones which are normalized
	// (for example, handle enhanced for loop refactoring)
	// TODO: for import, if class name is the same but package is different, this kind of difference is important
	// For example, BaseAction was changed to use struts 1 stubs
	// Whereas, if imports were just added / removed, that's just noise in most cases
	//	private static final String relative = "/jClaretyArchitectureSource/src/com/covansys/jclarety/arch/web/base/BaseAction.java";

	//	private static final String relative = "/jClaretyEJBProject\\ejbModule\\com\\covansys\\jclarety\\optionalservicecredit\\app\\internal\\fabm\\FABMCostStatement.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/optionalservicecredit/app/internal/pf/PFCCostStatement.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/retirementapplprocessing/app/api/be/BEBefEmpDtls.java";

	// TODO: make remove casting logic better
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/fabm/FABMMbrAcctMaint.java";

	// TODO: handle added fields
	// Such as BELegalOrder.crt_order
	// Indicate on report if added, deleted, or modified
	// For example, in this cade, added field

	// TODO: good example with added method (need to be able to detect)
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/db/DBMailingSort.java";

	// TODO: has confusion about which methods are mapped to which due to how diff looks
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/batchrequest/app/internal/pf/PFCDynamicParameter.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/pf/PFCLegalOrders.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/wageandcontribution/app/api/be/BVWCCBEObj.java";

	// TODO: When parse Java code, handle cases where method signature changes
	// For example, BACntrbStrtDtChng has a single difference
	// It's because trunk fixed the weird extra parameter in architecture class for DBRqst.read(int, BERqst)
	// In this case, the Phase 1 code has the extra parameter passed, whereas the Phase 2 code doesn't
	// This should show as an unimportant change (no impact, since reviewed)

	// TODO: Another method with changed signature is PFCDynamicParameter.populatePymtTypList(List<List<String>>, List<List<String>>)
	// For some reason, TRSPhase1 version doesn't show in report (not sure why, would have expected it)

	// Only difference is trunk adds @SuppressWarnings annotation (no impact)
	// (mention in comments, so can filter on this new annotation)
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/ba/BAExtrctMbrAcctAnnlStmnt.java";

	//	private static final String relative = "\\jClaretyEJBProject\\ejbModule\\com\\covansys\\jclarety\\brp\\app\\internal\\pf\\PFCBenefitSummary.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/db/DBLevy.java";

	// Fixed bug in Moved Lines logic - found when comparing DBAcctTrans (wasn't verifying that moved lines were identical)
	//	private static final String relative = "/jClaretyEJBProject\\ejbModule\\com\\covansys\\jclarety\\account\\app\\internal\\db\\DBAcctTrans.java";
	//	private static final String relative = "(Test) DBAcctTrans.java";

	// Done! another good example of substitution (looks like should have no concern,
	// but need to recognize that they are all acceptable substitutions via normalization / extra new lines
	// File has no differences of impact
	// 1) Has substitution via normalization / extra new lines due to normalization "EventInformationCache.EVT_INFO_ID_"
	// 2) Has a commented out piece of code that changed (due to their find / replace which wasn't smart enough to ignore this)
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/ba/BAProcessVendorAddrFiles.java";

	// Done! Eclipse says there are no differences when ignore whitespace, but my code says there is a line which differs
	// (there is a blank line which was inserted)
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/client/internal/wm/BAPrintMbrAcctAnnlStmnt.java";

	// TODO: good simple example of where a substitution should instead be a normalization, since same text, but on different lines due to changes
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/benefitamtadj/app/internal/db/DBOvrPymntDtlsLnk.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/db/DBAcctStat.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/benefitcalculating/app/api/be/BVEstimateDetails.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/benefitamtadj/app/api/be/BEOvrPymntDtlsLnk.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/refunds/app/api/be/BEPymtAddr.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/officeautomation/app/internal/fabm/FABMLevyAttachesLtr.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/admin/app/api/db/DBEarningRates.java";

	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/organization/app/api/be/BEEmpr.java";
	//	private static final String relative = "jClaretyEJBProject\\ejbModule\\com\\covansys\\jclarety\\account\\app\\api\\be\\BEAcctTrans.java";
	//    private static final String relative = "jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/fabm/FABMMbrAcctMaint.java";
	//	private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/api/be/BELegalOrder.java";
	//    private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/ba/BACalcAndPostInt.java";

	// BVBeny is unused in Phase 1
	// Two methods were commented out in Phase 2 (is this class used in Phase 2?)
	//    private static final String relative = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/benefitamtadj/app/api/be/BVBeny.java";

	//	private static final String root1 = "C:\\Workspaces\\JBoss";
	//	private static final String root2 = "C:\\Workspaces\\trunk";

	private static final String root1 = "C:\\Workspaces\\Testing\\TRSPhase1";
	private static final String root2 = "C:\\Workspaces\\TRSPhase1";

	//	private static final String root1 = "C:\\Workspaces\\Testing\\TRSPhase1";
	//	private static final String root2 = "C:\\Workspaces\\JBoss";
	//	private static final String root2 = "C:\\Workspaces\\Testing\\Trunk";

	@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "Testing")
	public static void main(final String[] args) throws IOException {
		Path path1 = Paths.get(root1, relative);
		Path path2 = Paths.get(root2, relative);

		long startTime = System.currentTimeMillis();

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION;

		System.out.println(relative);
		System.out.println("Path1: " + path1);
		System.out.println("Path2: " + path2);

		//        String text1 = "ABCABBA";
		//        String text2 = "CBABAC";
		//
		//        List<DiffLine> lines1 = splitCharacters(text1);
		//        List<DiffLine> lines2 = splitCharacters(text2);
		//
		//        System.out.println(lines1.stream().map(DiffLine::getText).collect(Collectors.toList()));
		//        System.out.println(lines2.stream().map(DiffLine::getText).collect(Collectors.toList()));

		List<String> original = Files.readAllLines(path1, StandardCharsets.ISO_8859_1);
		List<String> revised = Files.readAllLines(path2, StandardCharsets.ISO_8859_1);

		List<DiffLine> lines1 = IntStream.range(0, original.size())
				.mapToObj(i -> new DiffLine(i + 1, original.get(i)))
				.collect(Collectors.toList());
		List<DiffLine> lines2 = IntStream.range(0, revised.size())
				.mapToObj(i -> new DiffLine(i + 1, revised.get(i)))
				.collect(Collectors.toList());

		//		lines1.stream().filter(l -> l.getNumber() == 4034).forEach(System.out::println);
		//		lines2.stream().filter(l -> l.getNumber() == 11232).forEach(System.out::println);

		//        System.out.println("Using MyersLinearDiff");
		//        List<DiffEdit> diff = MyersLinearDiff.diff(lines1, lines2);

		System.out.println("Using PatienceDiff, fallback is MyersLinearDiff");

		List<DiffEdit> diff = PatienceDiff.diff(lines1, lines2, normalizationFunction,
				MyersLinearDiff.with(normalizationFunction));

		//		diff.stream().filter(d -> d.getOldLineNumber() == 8199).forEach(System.out::println);
		//		diff.stream().filter(d -> d.getNewLineNumber() == 7866).forEach(System.out::println);

		// TODO: wonky replacement when add LCS, not using refactoring

		// Look first for common refactorings, so can group changes together
		DiffHelper.handleSubstitution(diff, normalizationFunction, SUBSTITUTION_CONTAINS,
				new RefactorEnhancedForLoop(), JAVA_UNBOXING, JAVA_CAST
				// LCS sometimes causes wonky refactoring, since done before run before RefactorEnhancedForLoop
				//				);
				// Keep classname, so can easily import when comment out / uncomment
				, SubstitutionType.LCS_MAX_OPERATOR, SubstitutionType.LCS_MIN_OPERATOR);

		// TODO: handle IMPORT_SAME_CLASS_DIFFERENT_PACKAGE
		// Read through diffs and identify which are imports and extract the class name

		// Look for substitutions
		//		DiffHelper.handleSubstitution(diff, normalizationFunction, LcsSubstitutionType.LCS_MAX_OPERATOR);

		// TODO: support handling moved lines before substitution
		// In this case, need to
		DiffHelper.handleMovedLines(diff, normalizationFunction);

		// 8/16/2019 - Handle other substitutions
		//		DiffHelper.handleSubstitution(diff, normalizationFunction, LcsSubstitutionType.LCS_MIN_OPERATOR,
		//				new RefactorEnhancedForLoop());

		// TODO: make ignoring imports smarter - based on the class name
		// If same class, treat as substitution versus ignored line
		// TODO: move to DiffHelper and make into more helpful utility
		//		CompareDirectories.handleIgnoredLines(diff);

		List<DiffUnit> diffBlocks = DiffHelper.combineToDiffBlocks(diff, true);

		DiffHelper.handleSplitLines(diffBlocks, normalizationFunction);

		DiffHelper.handleBlankLines(diffBlocks, normalizationFunction);

		long differenceCount = diff.stream()
				.filter(d -> !Utilities.in(d.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
						BasicDiffType.IGNORE))
				.count();

		// Count only the number of blocks which have differences
		long deltas = diffBlocks.stream()
				.filter(d -> !Utilities.in(d.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
						BasicDiffType.IGNORE))
				.count();

		System.out.println("Differences: " + differenceCount);
		System.out.println("Deltas:" + deltas);

		// FABMMbrAcctMaint
		// Diff count: 62885 (serial, 51 seconds)
		// Diff count: 62885 (parallel, threshold 100, 36 seconds)
		// Diff count: 62885 (parallel, threshold 1,000, 36 seconds)
		// Diff count: 62885 (parallel, threshold 10,000, 36 seconds)
		// Diff count: 62885 (parallel, threshold 100,000, 50 seconds - always running in serial)

		// PatienceDiff 35 seconds (using recursive MyersLinearDiff)
		// PatienceDiff recursive 35 seconds (using recursive MyersLinearDiff)

		// Count number of differences (ignore if lines are equal)
		//        System.out.println("Diff count: " + diff.stream().filter(d -> d.getType() != DiffType.EQUAL).count());

		//		if (deltas < 280) {
		//		if (deltas <= 125) {
		int diffBlockNumber = 0;

		for (DiffUnit diffUnit : diffBlocks) {
			if (!shouldShowEqualBlocks && !showFullDiff
					&& Utilities.in(diffUnit.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
							BasicDiffType.IGNORE)) {
				continue;
			}

			// During testing, don't show Refactored blocks that are already handled
			// TODO: Add casting refactoring (use logic from line change)
			if (diffUnit.getType() instanceof RefactoringDiffType) {
				continue;
			}

			boolean showBlockChange = true;

			if (showNoImpactChanges || showFullDiff) {
				showBlockChange = true;
			} else {
				// Check to see if all changes in the block have no changes
				showBlockChange = diffUnit.getEdits()
						.stream()
						// Check if any change would be show, since the impact isn't NONE
						.anyMatch(TestCompareFiles::shouldShowChange);
				//						.anyMatch(e -> CompareDirectories.getJavaLineChangeType(e)
				//								.getImpactType() != ImpactType.NONE);
			}

			if (showBlockChange) {
				System.out.printf("%s %s%s%n", diffUnit.getClass(), diffUnit.getType(),
						(Utilities.in(diffUnit.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
								BasicDiffType.IGNORE) ? ""
										: " Group " + String.valueOf(++diffBlockNumber)));
			}

			if (!showFullDiff) {
				if (deltas > 40 && Utilities.in(diffUnit.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
						BasicDiffType.IGNORE) || diffBlockNumber >= 90) {
					continue;
				}
			}

			for (DiffEdit diffEdit : diffUnit.getEdits()) {
				//				LineChangeType changeType = CompareDirectories.getJavaLineChangeType(diffEdit);

				boolean showChange = showNoImpactChanges
						|| showFullDiff
						|| shouldShowChange(diffEdit);
				//						|| changeType.getImpactType() != ImpactType.NONE;

				if (showChange) {
					System.out.println(diffEdit.toString(true));
					//					if (diffEdit.getType().isSubstitution()) {
					//						System.out.println(changeType + ":");
					//						System.out.println(diffEdit.toString(true));
					//					} else {
					//						System.out.println(changeType + ": "
					//								+ diffEdit.toString(true));
					//					}
				}

				//                    String tag = diffEdit.getType().getTag();
				//
				//                    // Gets the line number if present or the empty string if absent
				//                    String lineNumber1 = diffEdit.getOldLine().map(d -> String.valueOf(d.getNumber())).orElse("");
				//                    String lineNumber2 = diffEdit.getNewLine().map(d -> String.valueOf(d.getNumber())).orElse("");
				//
				//                    // Format with line numbers
				//                    System.out.printf("%s%4s%4s    %s%n", tag, lineNumber1, lineNumber2, diffEdit.getText());
			}
		}

		System.out.println();

		long endTime = System.currentTimeMillis();
		System.out.printf("It took %s seconds", (endTime - startTime) / 1000);
	}

	private static boolean shouldShowChange(final DiffEdit diffEdit) {
		return true;
	}
}
