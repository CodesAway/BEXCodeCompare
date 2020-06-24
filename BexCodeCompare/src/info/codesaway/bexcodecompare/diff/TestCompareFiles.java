package info.codesaway.bexcodecompare.diff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.codesaway.bexcodecompare.diff.myers.MyersLinearDiff;
import info.codesaway.bexcodecompare.diff.patience.PatienceDiff;
import info.codesaway.bexcodecompare.util.Utilities;

public class TestCompareFiles {
	// TODO: enhance so creates before / after files which are aligned, so I can easily copy to Confluence
	// This way, I can easily make side-by-side compares without having to manually align the text
	// Essentially doing what WinMerge does, but adding in blank lines to new file
	// Also, has my smarter compare logic
	private static boolean showFullDiff = false;

	// Whether or not to show equal, normalized, and ignored lines
	private static boolean shouldShowEqualBlocks = false;

	// Whether to show differences which have no impact
	private static boolean showNoImpactChanges = false;

	// BaseAction has local changes but want to ignore ones which are normalized
	// (for example, handle enhanced for loop refactoring)
	private static final String relative = "/jClaretyArchitectureSource/src/com/covansys/jclarety/arch/web/base/BaseAction.java";

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

	private static final Path path1 = Paths.get(root1, relative);
	private static final Path path2 = Paths.get(root2, relative);

	public static void main(final String[] args) throws IOException {
		long startTime = System.currentTimeMillis();

		BiFunction<String, String, DiffNormalizedText> normalizationFunction = CompareDirectories.NORMALIZATION_FUNCTION;

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

		List<DiffLine> lines1 = IntStream.range(0, original.size()).mapToObj(i -> new DiffLine(i + 1, original.get(i)))
				.collect(Collectors.toList());
		List<DiffLine> lines2 = IntStream.range(0, revised.size()).mapToObj(i -> new DiffLine(i + 1, revised.get(i)))
				.collect(Collectors.toList());

		//		lines1.stream().filter(l -> l.getNumber() == 4034).forEach(System.out::println);
		//		lines2.stream().filter(l -> l.getNumber() == 11232).forEach(System.out::println);

		//        System.out.println("Using MyersLinearDiff");
		//        List<DiffEdit> diff = MyersLinearDiff.diff(lines1, lines2);

		System.out.println("Using PatienceDiff, fallback is MyersLinearDiff");

		BiFunction<List<DiffLine>, List<DiffLine>, List<DiffEdit>> fallbackAlgorithm = (a, b) -> MyersLinearDiff.diff(a,
				b, normalizationFunction);

		List<DiffEdit> diff = PatienceDiff.diff(lines1, lines2, fallbackAlgorithm, normalizationFunction);

		//		diff.stream().filter(d -> d.getOldLineNumber() == 8199).forEach(System.out::println);
		//		diff.stream().filter(d -> d.getNewLineNumber() == 7866).forEach(System.out::println);

		// Look for substitutions
		DiffAlgorithm.handleSubstitution(diff, normalizationFunction, SubstitutionType.SIMPLE);

		DiffAlgorithm.handleMovedLines(diff);

		// 8/16/2019 - Handle other substitutions
		DiffAlgorithm.handleSubstitution(diff, normalizationFunction, SubstitutionType.ANY);

		CompareDirectories.handleIgnoredLines(diff);

		List<DiffUnit> diffBlocks = DiffAlgorithm.combineToDiffBlocks(diff, true);

		DiffAlgorithm.handleSplitLines(diffBlocks, normalizationFunction);

		DiffAlgorithm.handleBlankLines(diffBlocks, normalizationFunction);

		long differenceCount = diff.stream().filter(
				d -> !Utilities.in(d.getType(), DiffTypeEnum.EQUAL, DiffTypeEnum.NORMALIZE, DiffTypeEnum.IGNORE))
				.count();

		// Count only the number of blocks which have differences
		long deltas = diffBlocks.stream().filter(
				d -> !Utilities.in(d.getType(), DiffTypeEnum.EQUAL, DiffTypeEnum.NORMALIZE, DiffTypeEnum.IGNORE))
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
			if (!shouldShowEqualBlocks && !showFullDiff && Utilities.in(diffUnit.getType(), DiffTypeEnum.EQUAL,
					DiffTypeEnum.NORMALIZE, DiffTypeEnum.IGNORE)) {
				continue;
			}

			boolean showBlockChange;

			if (showNoImpactChanges || showFullDiff) {
				showBlockChange = true;
			} else {
				showBlockChange = false;
//				// Check to see if all changes in the block have no changes
//				showBlockChange = diffUnit.getEdits().stream()
//						// Check if any change would be show, since the impact isn't NONE
//						.anyMatch(e -> CompareDirectories.getJavaLineChangeType(e).getImpactType() != ImpactType.NONE);
			}

			if (showBlockChange) {
				System.out.printf("%s %s %s%n", diffUnit.getClass(), diffUnit.getType(),
						(Utilities.in(diffUnit.getType(), DiffTypeEnum.EQUAL, DiffTypeEnum.NORMALIZE,
								DiffTypeEnum.IGNORE) ? "" : " Group " + String.valueOf(++diffBlockNumber)));
			}

			if (!showFullDiff) {
				if (deltas > 40 && Utilities.in(diffUnit.getType(), DiffTypeEnum.EQUAL, DiffTypeEnum.NORMALIZE,
						DiffTypeEnum.IGNORE) || diffBlockNumber >= 90) {
					continue;
				}
			}

			for (DiffEdit diffEdit : diffUnit.getEdits()) {
//				LineChangeType changeType = CompareDirectories.getJavaLineChangeType(diffEdit);

				boolean showChange = showNoImpactChanges || showFullDiff;
//						|| changeType.getImpactType() != ImpactType.NONE;

				if (showChange) {
//					if (diffEdit.getType().isSubstitution()) {
//						System.out.println(changeType + ":");
//						System.out.println(diffEdit.toString(true));
//					} else {
//						System.out.println(changeType + ": " + diffEdit.toString(true));
//					}
					System.out.println(diffEdit.toString());
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

		//            for (DiffEdit diffEdit : diff) {
		//                String tag = diffEdit.getType().getTag();
		//
		//                // Gets the line number if present or the empty string if absent
		//                String lineNumber1 = diffEdit.getOldLine().map(d -> String.valueOf(d.getNumber())).orElse("");
		//                String lineNumber2 = diffEdit.getNewLine().map(d -> String.valueOf(d.getNumber())).orElse("");
		//
		//                System.out.printf("%s%4s%4s    %s%n", tag, lineNumber1, lineNumber2, diffEdit.getText());
		//            }
		//		}

		// TODO: only output if has differences
		if (deltas <= 35) {
			// Output the Substitution / Replacement blocks full substitution info so can write logic to handle some additional scenarios

			List<DiffUnit> substitutionBlocks = diffBlocks.stream().filter(d -> d.getType().isSubstitution())
					.collect(Collectors.toList());

			for (DiffUnit block : substitutionBlocks) {
				System.out.println();

				if (block instanceof DiffBlock) {
					// Only output if block
					// (if only a single statement then wouldn't need to check if combined lines would be similar to each other)

					StringBuilder normalizedOldText = new StringBuilder();
					StringBuilder normalizedNewText = new StringBuilder();

					// First handling simple case where entire block is identical text, just split into multiple lines
					// TODO: find example where parts of block are identical text (or similar text) and handle
					for (DiffEdit diffEdit : block.getEdits()) {
						char tag = diffEdit.getType().getTag();

						// Gets the line number if present or the empty string if absent
						String lineNumber1 = diffEdit.getOldLine().map(d -> String.valueOf(d.getNumber())).orElse("");
						String lineNumber2 = diffEdit.getNewLine().map(d -> String.valueOf(d.getNumber())).orElse("");

						String text1 = diffEdit.getOldText();
						String text2 = diffEdit.getNewText();

						DiffNormalizedText normalizedText = normalizationFunction.apply(text1, text2);

						String normalizedText1 = normalizedText.getText1();
						String normalizedText2 = normalizedText.getText2();

						// Format with line numbers
						if (diffEdit.getOldLine().isPresent()) {
							normalizedOldText.append(normalizedText1);
							System.out.println(String.format("%s%6s%6s    %s", tag, lineNumber1, "", normalizedText1));
						}

						if (diffEdit.getNewLine().isPresent()) {
							normalizedNewText.append(normalizedText2);
							System.out.println(String.format("%s%6s%6s    %s", tag, "", lineNumber2, normalizedText2));
						}
					}

					if (normalizedOldText.toString().equals(normalizedNewText.toString())) {
						System.out.println("Identical text split across lines!");
						// Should mark entire block as normalized
					} else {
						System.out.println(normalizedOldText);
						System.out.println(normalizedNewText);
					}
				}
			}
		}

		System.out.println();

		long endTime = System.currentTimeMillis();
		System.out.printf("It took %s seconds", (endTime - startTime) / 1000);
	}

	/*
	private static List<DiffLine> splitCharacters(final String text) {
	    List<DiffLine> lines = new ArrayList<>();
	
	    for (int i = 0; i < text.length(); i++) {
	        lines.add(new DiffLine(i + 1, text.substring(i, i + 1)));
	    }
	
	    return lines;
	}
	*/
}
