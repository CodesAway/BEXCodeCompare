package info.codesaway.bexcodecompare.diff;

import java.util.List;
import java.util.function.BiFunction;

import info.codesaway.bexcodecompare.util.RegexUtilities;
import info.codesaway.bexcodecompare.util.Utilities;
import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

public class CompareDirectories {
	public static final BiFunction<String, String, DiffNormalizedText> NORMALIZATION_FUNCTION = CompareDirectories::normalizeLines;

	private static final ThreadLocal<Matcher> MULTIPLE_WHITESPACE_MATCHER = RegexUtilities.getThreadLocalMatcher("\\s++");

	private static final ThreadLocal<Matcher> NORMALIZE_WHITESPACE_MATCHER = RegexUtilities.getThreadLocalMatcher("\\b \\B|\\B \\b|\\B \\B");
	
	/**
	 * Pattern to check which lines to ignore (if the line matches the pattern, it should be ignored)
	 */
	// 8/16/2019 - tried to ignore blank lines but makes some diffs more confusing, such as new methods, like in DBAdminAppeal
	// Trying to handle extra blank lines line in BAPrintMbrAcctAnnlStmnt
	// Instead done in DiffAlgorithm.handleBlankLines
	private static final Pattern IGNORE_LINES_PATTERN = Pattern.compile("import .*;");

	private static final ThreadLocal<Matcher> IGNORE_LINES_MATCHER = ThreadLocal
			.withInitial(() -> IGNORE_LINES_PATTERN.matcher(""));


	/**
	 * Normalization function used to compare code
	 *
	 * @param line1
	 * @param line2
	 * @return
	 */
	private static DiffNormalizedText normalizeLines(final String line1, final String line2) {
		String normalizedText1 = line1.trim();
		String normalizedText2 = line2.trim();

		//		normalizedText1 = handleNormalizationSwitch(normalizedText1);
		//		normalizedText2 = handleNormalizationSwitch(normalizedText2);

		// Normalize whitespace

		// Replace multiple whitespace with a single space
		normalizedText1 = MULTIPLE_WHITESPACE_MATCHER.get().reset(normalizedText1).replaceAll(" ");
		normalizedText2 = MULTIPLE_WHITESPACE_MATCHER.get().reset(normalizedText2).replaceAll(" ");

		normalizedText1 = NORMALIZE_WHITESPACE_MATCHER.get().reset(normalizedText1).replaceAll("");
		normalizedText2 = NORMALIZE_WHITESPACE_MATCHER.get().reset(normalizedText2).replaceAll("");

		return new DiffNormalizedText(normalizedText1, normalizedText2);
	}
	
	/**
	 * Indicate certain lines as should be ignored when counting number of differences between files
	 *
	 * @param diffEdits
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    04/01/2019  Initial coding
	 *</pre>***********************************************************************************
	 */
	public static void handleIgnoredLines(final List<DiffEdit> diffEdits) {
		Matcher ignoreLinesMatcher = IGNORE_LINES_MATCHER.get();

		for (int i = 0; i < diffEdits.size(); i++) {
			DiffEdit diffEdit = diffEdits.get(i);

			if (!Utilities.in(diffEdit.getType(), DiffTypeEnum.EQUAL, DiffTypeEnum.NORMALIZE)) {
				// If line isn't equal, ignore certain lines
				if (ignoreLinesMatcher.reset(diffEdit.getText()).matches()) {
					diffEdits.set(i, new DiffEdit(DiffTypeEnum.IGNORE, diffEdit.getOldLine(), diffEdit.getNewLine()));
				}
			}
		}
	}
}
