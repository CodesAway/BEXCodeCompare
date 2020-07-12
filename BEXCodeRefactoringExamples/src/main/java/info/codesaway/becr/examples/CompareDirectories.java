package info.codesaway.becr.examples;

import static info.codesaway.becr.util.ExcelUtilities.EXCEL_COLUMN_CHARACTER_MULTIPLIER;
import static info.codesaway.bex.BEXSide.LEFT;
import static info.codesaway.bex.BEXSide.RIGHT;
import static info.codesaway.bex.diff.substitution.SubstitutionType.LCS_MAX_OPERATOR;
import static info.codesaway.bex.diff.substitution.SubstitutionType.SUBSTITUTION_CONTAINS;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_CAST;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_DIAMOND_OPERATOR;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_FINAL_KEYWORD;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_SEMICOLON;
import static info.codesaway.bex.diff.substitution.java.JavaRefactorings.JAVA_UNBOXING;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import info.codesaway.becr.diff.CompareDirectoriesJoinedDetail;
import info.codesaway.becr.diff.CompareDirectoriesResult;
import info.codesaway.becr.diff.CompareJavaCodeInfo;
import info.codesaway.becr.diff.CorrespondingCode;
import info.codesaway.becr.diff.DifferencesResult;
import info.codesaway.becr.diff.FileType;
import info.codesaway.becr.diff.ImpactType;
import info.codesaway.becr.diff.PathChangeInfo;
import info.codesaway.becr.diff.PathChangeType;
import info.codesaway.becr.parsing.CodeInfo;
import info.codesaway.becr.parsing.CodeInfoWithLineInfo;
import info.codesaway.becr.parsing.CodeInfoWithSourceInfo;
import info.codesaway.becr.parsing.CodeType;
import info.codesaway.becr.parsing.FieldInfo;
import info.codesaway.becr.parsing.MethodSignature;
import info.codesaway.becr.parsing.ParsingUtilities;
import info.codesaway.becr.parsing.ProjectPath;
import info.codesaway.becr.util.ExcelUtilities;
import info.codesaway.bex.BEXListPair;
import info.codesaway.bex.BEXMapPair;
import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.MutableIntBEXPair;
import info.codesaway.bex.diff.BasicDiffType;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.DiffUnit;
import info.codesaway.bex.diff.myers.MyersLinearDiff;
import info.codesaway.bex.diff.patience.PatienceDiff;
import info.codesaway.bex.diff.substitution.SubstitutionType;
import info.codesaway.bex.diff.substitution.java.RefactorEnhancedForLoop;
import info.codesaway.bex.util.BEXUtilities;

// TODO: change this into a utility and separate out report generation from determining differences
// Use this as an example, but have the common functionality available for anyone to use how they want
public class CompareDirectories {
	/**
	 * If not null, copies the changed files to the specified pathname
	 * <ul>
	 * <li>Keeps the existing relative directory structure (makes easy to paste to overlay existing files)</li>
	 * <li>(creates any necessary directories in the copied location)</li>
	 * <li>Limited to changes listed on report</li>
	 * <li>Use null to not copy files</li>
	 * </ul>
	 */
	private static Path copyChangedFilesDestinationPath = null;

	private static Path copyChangedFilesSourcePath;

	private static int DIRECTORY_COLUMN;
	private static int FILENAME_COLUMN;
	private static int DIFFERENCES_COLUMN;
	private static int DELTAS_COLUMN;
	private static int PATHNAME_COLUMN;

	private static XSSFCellStyle WRAP_TEXT_CELL_STYLE;

	private static final Splitter lineSplitter = Splitter.onPattern("\r?+\n|\r");

	// Read from JAVA_HOME environment variable
	// TODO: also support specifying it
	private static String jrePathname = System.getenv("JAVA_HOME");

	private static boolean isTesting;

	// Settings for the compare
	private BiFunction<String, String, ASTParser> parserBiFunction = ParsingUtilities::getParser;
	private Set<String> ignoreProjects;
	private Predicate<Path> skipTextCompare;

	private final List<BiFunction<Path, BEXListPair<DiffLine>, SubstitutionType>> substitutionTypes = new ArrayList<>();
	private boolean excludeLCSMaxSubstitution = false;

	// Flatten reporting of directories
	// Only report if contains files or contains nothing (meaning deepest level)
	// This way, for example if created a/b/c subfolders, would only report "a/b/c" versus also reporting "a", "a/b"
	// However, if directory a/b/c had no files or directories would still report
	// TODO: name something clearer
	private boolean flattenReportingOfAddedDirectories = true;

	public CompareDirectories() {
		this.addSubstitutionTypes(JAVA_SEMICOLON, SUBSTITUTION_CONTAINS, IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE,
				JAVA_UNBOXING, JAVA_CAST, JAVA_FINAL_KEYWORD, JAVA_DIAMOND_OPERATOR);
		this.addSubstitutionTypes(RefactorEnhancedForLoop::new);
	}

	/**
	 * Sets the parser used when parsing Java files
	 * @param parserBiFunction BiFunction accepting the workspace pathname and JRE pathname
	 * @return <code>this</code> object
	 */
	public CompareDirectories parserBiFunction(final BiFunction<String, String, ASTParser> parserBiFunction) {
		this.parserBiFunction = parserBiFunction;
		return this;
	}

	/**
	 * @param ignoreProjects
	 * @return <code>this</code> object
	 */
	public CompareDirectories ignoreProjects(final Set<String> ignoreProjects) {
		this.ignoreProjects = ignoreProjects;
		return this;
	}

	/**
	 * @param skipTextCompare
	 * @return <code>this</code> object
	 */
	public CompareDirectories skipTextCompare(final Predicate<Path> skipTextCompare) {
		this.skipTextCompare = skipTextCompare;
		return this;
	}

	/**
	 * @param excludeLCSMaxSubstitution
	 * @return <code>this</code> object
	 */
	public CompareDirectories excludeLCSMaxSubstitution(final boolean excludeLCSMaxSubstitution) {
		this.excludeLCSMaxSubstitution = excludeLCSMaxSubstitution;
		return this;
	}

	/**
	 * @param flattenReportingOfAddedDirectories
	 * @return <code>this</code> object
	 */
	public CompareDirectories flattenReportingOfAddedDirectories(final boolean flattenReportingOfAddedDirectories) {
		this.flattenReportingOfAddedDirectories = flattenReportingOfAddedDirectories;
		return this;
	}

	/**
	 * @param substitutionTypeSuppliers
	 * @return <code>this</code> object
	 */
	@SafeVarargs
	public final CompareDirectories addSubstitutionTypes(
			final Supplier<SubstitutionType>... substitutionTypeSuppliers) {
		for (Supplier<SubstitutionType> substitutionTypeSupplier : substitutionTypeSuppliers) {
			this.substitutionTypes.add((p, l) -> substitutionTypeSupplier.get());
		}
		return this;
	}

	/**
	 * @param substitutionTypeBiFunctions <code>BiFunction</code> taking relative Path and list of lines of the file to be compared
	 * @return <code>this</code> object
	 */
	@SafeVarargs
	public final CompareDirectories addSubstitutionTypes(
			final BiFunction<Path, BEXListPair<DiffLine>, SubstitutionType>... substitutionTypeBiFunctions) {
		for (BiFunction<Path, BEXListPair<DiffLine>, SubstitutionType> substitutionTypeBiFunction : substitutionTypeBiFunctions) {
			this.substitutionTypes.add(substitutionTypeBiFunction);
		}
		return this;
	}

	/**
	 * @param substitutionTypes
	 * @return <code>this</code> object
	 */
	public final CompareDirectories addSubstitutionTypes(final SubstitutionType... substitutionTypes) {
		for (SubstitutionType substitutionType : substitutionTypes) {
			this.substitutionTypes.add((p, l) -> substitutionType);
		}
		return this;
	}

	/**
	 * @param substitutionTypes
	 * @return <code>this</code> object
	 */
	public final CompareDirectories addSubstitutionTypes(final List<SubstitutionType> substitutionTypes) {
		for (SubstitutionType substitutionType : substitutionTypes) {
			this.substitutionTypes.add((p, l) -> substitutionType);
		}
		return this;
	}

	/**
	 * @param workspace
	 * @param projectSheetNameMap
	 * @throws IOException
	 */
	public void compare(final BEXPair<String> workspace,
			final Map<String, String> projectSheetNameMap)
			throws IOException {
		long startTime = System.currentTimeMillis();

		//		isTesting = optionalTestingFile.isPresent();
		//		String testingFile = optionalTestingFile.orElse("");

		BEXPair<ASTParser> parser = workspace.map(w -> this.parserBiFunction.apply(w, jrePathname));

		String excelReportFilename = String.format("Compare Test%s.xlsx", isTesting ? " (Testing)" : "");

		Path excelReportPath = Paths.get("G:/", excelReportFilename);

		// TODO: add logic to detect type of change
		//  This will be a common difference, so detect common differences
		//  Then can identify these differences as no impact

		BEXPair<Path> rootPath = workspace.map(Paths::get);

		// Store location where should copy files from
		// (copy from right rootPath, which is thought as the "after" changes location)
		// (leave null if shouldn't copy files)
		copyChangedFilesSourcePath = copyChangedFilesDestinationPath != null ? rootPath.getRight() : null;

		// TODO: might sort using Pattern.getNaturalComparator()
		// (This way the help files are in the correct order numerically, versus lexographically

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFCellStyle wrapTextCellStyle = workbook.createCellStyle();
			wrapTextCellStyle.setWrapText(true);
			WRAP_TEXT_CELL_STYLE = wrapTextCellStyle;

			XSSFCellStyle hyperlinkCellStyle = workbook.createCellStyle();
			Font hyperlinkFont = workbook.createFont();
			hyperlinkFont.setUnderline(Font.U_SINGLE);
			hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
			hyperlinkCellStyle.setFont(hyperlinkFont);

			List<String> headerColumnNames = new ArrayList<>();

			int projectColumn = headerColumnNames.size();
			headerColumnNames.add("Project");

			// Initialize column numbers and names
			int directoryColumn = headerColumnNames.size();
			headerColumnNames.add("Directory");
			DIRECTORY_COLUMN = directoryColumn;

			int filenameColumn = headerColumnNames.size();
			headerColumnNames.add("Filename");
			FILENAME_COLUMN = filenameColumn;

			//			int extensionColumn = headerColumnNames.size();
			headerColumnNames.add("Ext");

			int fileTypeColumn = headerColumnNames.size();
			headerColumnNames.add("File Type");

			int differencesChangeTypeColumn = headerColumnNames.size();
			headerColumnNames.add("Change");

			int differencesColumn = headerColumnNames.size();
			headerColumnNames.add("Differences");
			DIFFERENCES_COLUMN = differencesColumn;

			int deltasColumn = headerColumnNames.size();
			headerColumnNames.add("Deltas");
			DELTAS_COLUMN = deltasColumn;

			int pathnameColumn = headerColumnNames.size();
			headerColumnNames.add("Pathname");
			PATHNAME_COLUMN = pathnameColumn;

			int lastColumn = headerColumnNames.size() - 1;

			// Details header columns
			List<String> detailsHeaderColumnNames = new ArrayList<>();

			// Initialize column numbers and names
			int packageColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Package");

			int classColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Class");

			int detailsChangeTypeColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Change");

			int typeColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Type");

			//			int impactColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Impact");

			int modifiersColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Modifiers");

			int returnColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Return / Class");

			int methodColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Method / Field");

			int infoColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Info");

			int detailsDifferencesColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Differences");

			int detailsDeltasColumn = detailsHeaderColumnNames.size();
			detailsHeaderColumnNames.add("Deltas");

			int lastDetailsColumn = detailsHeaderColumnNames.size() - 1;

			// Create sheets
			String sheetName = "Differences";

			XSSFSheet sheet = workbook.getSheet(sheetName);

			// First time using sheet
			if (sheet == null) {
				sheet = workbook.createSheet(sheetName);

				ExcelUtilities.createHeaderRow(sheet, 0, headerColumnNames);
			}

			// TODO: support creating specific project sheets at the front
			// (so to show them in a certain order versus alphabetical)
			// For example, to put the commonly changed stuff first
			//			XSSFSheet combinedEJBWebSheet = workbook.createSheet(combinedSheetName);
			//			ParsingUtilities.createHeaderRow(combinedSheet, 0, detailsHeaderColumnNames);

			// TODO: for files in added directories, don't list each file on own line
			// Instead, list all files as info for the added directory
			// This way if added a new package with 10 classes, would have 1 line showing the new package, listing the 10 classes
			// (versus having one line for the new package and 10 extra lines for each of the new classes)

			CompareDirectoriesResult compareDirectoriesResult = this.findDifferences(rootPath);

			for (PathChangeInfo change : compareDirectoriesResult.getPathChanges()) {
				addDifference(sheet, change);
			}

			BEXListPair<ProjectPath> javaPaths = compareDirectoriesResult.getJavaPaths();
			Map<Path, DifferencesResult> diffs = compareDirectoriesResult.getJavaPathDiffMap();

			if (!javaPaths.isLeftEmpty()) {
				// Has Java files that I want to parse
				//				BEXPair<String[]> javaPaths = javaPaths.map(f -> f.stream()
				//						.map(ProjectPath::getPathname)
				//						.toArray(String[]::new));

				System.out.println("Parsing Java code");
				BEXMapPair<String, CompareJavaCodeInfo> results = parse(parser, javaPaths, diffs);

				System.out.println("Analyzing Java code");

				// For each pair of paths
				for (int i = 0; i < javaPaths.rightSize(); i++) {

					if ((i + 1) % 10000 == 0) {
						System.out.printf("Analyzing Java code: Path %d of %d%n", i + 1, javaPaths.rightSize());
					}

					BEXPair<ProjectPath> javaPath = javaPaths.get(i);

					String project = javaPath.getRight().getProject();

					String className = javaPath.getRight().getName();
					className = className.substring(0, className.length() - ".java".length());

					BEXPair<RangeMap<Integer, CodeInfoWithLineInfo>> extendedRanges = new BEXPair<>(
							TreeRangeMap::create);
					BEXPair<RangeMap<Integer, CodeInfoWithLineInfo>> ranges = new BEXPair<>(TreeRangeMap::create);

					BEXPair<CompareJavaCodeInfo> result = results.get(javaPath.map(ProjectPath::getPathname));

					String packageName = result.getRight().getPackageName();

					for (CodeInfoWithLineInfo detail : result.getLeft().getDetails()) {
						Range<Integer> extendedRange = Range.closed(detail.getExtendedStartLine(),
								detail.getEndLine());
						Range<Integer> range = Range.closed(detail.getStartLine(), detail.getEndLine());

						// Has class defined within a method
						if (!extendedRanges.getLeft().subRangeMap(extendedRange).asMapOfRanges().isEmpty()) {
							continue;
						}

						if (!ranges.getLeft().subRangeMap(range).asMapOfRanges().isEmpty()) {
							//											continue;
							throw new AssertionError(
									String.format("Range has overlap in %s: %s", javaPath.getLeft(), range));
						}

						extendedRanges.getLeft().put(extendedRange, detail);
						ranges.getLeft().put(range, detail);
					}

					for (CodeInfoWithLineInfo detail : result.getRight().getDetails()) {
						Range<Integer> extendedRange = Range.closed(detail.getExtendedStartLine(),
								detail.getEndLine());
						Range<Integer> range = Range.closed(detail.getStartLine(), detail.getEndLine());

						if (!extendedRanges.getRight().subRangeMap(extendedRange).asMapOfRanges().isEmpty()) {
							continue;
						}

						if (!ranges.getRight().subRangeMap(range).asMapOfRanges().isEmpty()) {
							throw new AssertionError(
									String.format("Range has overlap in %s: %s", javaPath.getRight(), range));
						}

						extendedRanges.getRight().put(extendedRange, detail);
						ranges.getRight().put(range, detail);
					}

					// TODO: how to handle moved lines?
					// For example if moved within same method
					// What if moved between methods? (treat comments different than actual code?)

					DifferencesResult differencesResult = diffs.get(javaPath.getRight().getPath());

					List<DiffUnit> diffBlocks = differencesResult.getDiffBlocks();

					// Map from source to destination the various code
					// Use to detect added / removed / modified methods / fields / etc.

					// First look at equal and normalized equal blocks
					// (these will tell me what stuff was modified)
					// For example, method signature changes, but parts of method code is the same
					// Versus, an added / removed method wouldn't be part of an equals block
					BiMap<CodeInfoWithLineInfo, CodeInfoWithLineInfo> mapCodeBlocks = HashBiMap.create();

					Multiset<CorrespondingCode> correspondingCodeCounts = LinkedHashMultiset.create();
					Multiset<CorrespondingCode> matchingCodeCounts = LinkedHashMultiset.create();

					for (DiffUnit unit : diffBlocks) {
						DiffType type = unit.getType();

						// Also allow substitution
						boolean checkForCorrespondingCode = false;

						checkForCorrespondingCode |= BEXUtilities.in(type, BasicDiffType.EQUAL,
								BasicDiffType.NORMALIZE);

						// 11/20/2019 - if substitution, also include these lines
						// (handle cases where comment out large blocks of code)
						checkForCorrespondingCode |= type.isSubstitution() && !type.isMove();

						if (!checkForCorrespondingCode) {
							continue;
						}

						for (DiffEdit edit : unit.getEdits()) {
							// Ignore blank lines
							if (edit.getText().trim().isEmpty()) {
								continue;
							}

							CodeInfoWithLineInfo leftDetail = ranges.getLeft().get(edit.getLeftLineNumber());

							if (leftDetail == null) {
								continue;
							}

							CodeInfoWithLineInfo rightDetail = ranges.getRight().get(edit.getRightLineNumber());

							if (rightDetail == null) {
								continue;
							}

							CorrespondingCode correspondingCode = new CorrespondingCode(leftDetail, rightDetail);

							// Mark line as corresponding
							// Will then see which to join based on how many lines have the same corresponding code
							correspondingCodeCounts.add(correspondingCode);

							if (BEXUtilities.in(type, BasicDiffType.EQUAL,
									BasicDiffType.NORMALIZE)) {
								matchingCodeCounts.add(correspondingCode);
							}
						}
					}

					Map<CodeInfoWithLineInfo, Integer> leftBlankLineCounts = new HashMap<>();
					Map<CodeInfoWithLineInfo, Integer> rightBlankLineCounts = new HashMap<>();

					Map<CodeInfoWithLineInfo, Integer> leftCorrespondingCode = new HashMap<>();
					Map<CodeInfoWithLineInfo, Integer> rightCorrespondingCode = new HashMap<>();

					// Find corresponding code
					// (must have more than 50% match to be considered for this first pass)
					// TODO: change this to be greedy algorithm so will match highest corresponding counts first
					for (Multiset.Entry<CorrespondingCode> entry : correspondingCodeCounts.entrySet()) {
						CorrespondingCode correspondingCode = entry.getElement();

						// 12/4/2019 - ensure has at least one matching line to consider corresponding
						// (this way if all lines are substitution, don't consider corresponding)
						if (!matchingCodeCounts.contains(correspondingCode)) {
							continue;
						}

						CodeInfoWithLineInfo leftCode = correspondingCode.getLeft();
						CodeInfoWithLineInfo rightCode = correspondingCode.getRight();

						// Used to ignore blank lines from line counts
						int leftBlankLines = leftBlankLineCounts.computeIfAbsent(leftCode,
								c -> countBlankLines(differencesResult.getLeftLines(), c));

						int rightBlankLines = rightBlankLineCounts.computeIfAbsent(rightCode,
								c -> countBlankLines(differencesResult.getRightLines(), c));

						int leftLines = leftCode.getLineCount() - leftBlankLines;
						int rightLines = rightCode.getLineCount() - rightBlankLines;

						// TODO: there's a potential that 2 methods match
						// (such as if split one method into two)
						// In this case, choose the one with the most matching lines
						// Then, match method with closer signature
						// 5/31/2020 - change to use max, since getting false positives with short methods
						int lines = Math.max(leftLines, rightLines);

						if (entry.getCount() > 0.50 * lines) {
							boolean haveUsedLeftCode = mapCodeBlocks.containsKey(leftCode);
							boolean haveUsedRightCode = mapCodeBlocks.containsValue(rightCode);

							if (haveUsedLeftCode && haveUsedRightCode) {
								// TODO: how to handle, for now, just skip
								throw new IllegalStateException();
								//								continue;
							}

							if (haveUsedLeftCode) {
								// Determine whether this entry has more matching lines
								Integer existingCount = leftCorrespondingCode.get(leftCode);

								if (entry.getCount() > existingCount) {
									// This new entry has more matching rows, so use it instead of the current value
									mapCodeBlocks.remove(leftCode);
								} else {
									// Existing match is better, so keep it
									continue;
								}
							} else if (haveUsedRightCode) {
								// Determine whether this entry has more matching lines
								Integer existingCount = rightCorrespondingCode.get(rightCode);

								if (entry.getCount() > existingCount) {
									// This new entry has more matching rows, so use it instead of the current value
									mapCodeBlocks.inverse().remove(rightCode);
								} else {
									// Existing match is better, so keep it
									continue;
								}
							}

							// If matches on more than 50%, consider a match
							mapCodeBlocks.put(leftCode, rightCode);

							leftCorrespondingCode.put(leftCode, entry.getCount());
							rightCorrespondingCode.put(rightCode, entry.getCount());
						}
					}

					// For remaining ones, if signature matches, consider a match
					// 1) This way, handles if refactor method to add parameter, but also add method with original signature
					// a) The refactored method (with the added parameter) would be matched up to the original method (due to equal lines of code)
					// b) Then, the added method with the original signature would show as being added
					// 2) Or, if the method doesn't share at least 50% of the code, but the signature remained the same
					for (Multiset.Entry<CorrespondingCode> entry : correspondingCodeCounts.entrySet()) {
						CorrespondingCode correspondingCode = entry.getElement();
						CodeInfoWithLineInfo leftCode = correspondingCode.getLeft();
						CodeInfoWithLineInfo rightCode = correspondingCode.getRight();

						if (mapCodeBlocks.containsKey(leftCode)) {
							continue;
						}

						if (mapCodeBlocks.containsValue(rightCode)) {
							continue;
						}

						if (leftCode.getCodeInfo().equals(rightCode.getCodeInfo())) {
							mapCodeBlocks.put(leftCode, rightCode);

							if (isTesting) {
								System.out.println("Same signature! " + leftCode
										+ " -> "
										+ rightCode);
							}
						}
					}

					if (isTesting) {
						System.out.println();
						System.out.println("Corresponding code counts:");
						correspondingCodeCounts.entrySet().forEach(System.out::println);

						System.out.println();
						System.out.println("Corresponding blocks:");
						mapCodeBlocks.entrySet()
								.stream()
								.sorted((a, b) -> Integer.compare(a.getKey().getStartLine(), b.getKey().getStartLine()))
								.forEach(e -> System.out.println(
										(!e.getKey().getCodeInfo().equals(e.getValue().getCodeInfo()) ? "(Different) "
												: "") + e.getKey()
												+ " -> "
												+ e.getValue()));
					}
					//							.collect(toList());

					//					System.out.println();

					List<CodeInfoWithLineInfo> deletedBlocks = result.getLeft()
							.getDetails()
							.stream()
							// Ignore if already found the corresponding match
							.filter(d -> !mapCodeBlocks.containsKey(d))
							.collect(toList());

					List<CodeInfoWithLineInfo> addedBlocks = result.getRight()
							.getDetails()
							.stream()
							// Ignore if already found the corresponding match
							.filter(d -> !mapCodeBlocks.containsValue(d))
							.collect(toList());

					if (isTesting && !deletedBlocks.isEmpty()) {
						System.out.println();
						System.out.println("Deleted blocks");
						deletedBlocks.forEach(System.out::println);
					}

					if (isTesting && !addedBlocks.isEmpty()) {
						System.out.println();
						System.out.println("Added blocks");
						addedBlocks.forEach(System.out::println);
					}

					// Check for blocks which are marked as both added and deleted
					// Mark these as corresponding
					Map<CodeInfo, CodeInfoWithLineInfo> deletedBlocksMap = new HashMap<>();

					for (CodeInfoWithLineInfo deletedBlock : deletedBlocks) {
						deletedBlocksMap.put(deletedBlock.getCodeInfo(), deletedBlock);
					}

					for (Iterator<CodeInfoWithLineInfo> iterator = addedBlocks.iterator(); iterator.hasNext();) {
						CodeInfoWithLineInfo addedBlock = iterator.next();
						CodeInfoWithLineInfo deletedBlock = deletedBlocksMap.get(addedBlock.getCodeInfo());

						if (deletedBlock != null) {
							// There is a corresponding deleted block (same signature) as the added block
							mapCodeBlocks.put(deletedBlock, addedBlock);

							// Remove added block
							iterator.remove();

							// Removed deleted block
							deletedBlocks.remove(deletedBlock);

							// Note: not updating deleted / added blocks since not using for anything

							if (isTesting) {
								System.out.println("Found another match: " + deletedBlock
										+ " -> "
										+ addedBlock);
							}
						}
					}

					List<CompareDirectoriesJoinedDetail> changes = new ArrayList<>();

					// Add Unknown change
					// (show first, before other changes, since these likely should be investigated to improve my code's logic)
					CompareDirectoriesJoinedDetail unknownChange = new CompareDirectoriesJoinedDetail(null, null);
					changes.add(unknownChange);

					BiMap<CodeInfoWithLineInfo, CodeInfoWithLineInfo> inverseMapCodeBlocks = mapCodeBlocks.inverse();

					// From from changed code to joint change
					BEXMapPair<CodeInfoWithLineInfo, CompareDirectoriesJoinedDetail> changesMapPair = new BEXMapPair<>(
							HashMap::new);

					int indexDeletedBlock = 0;
					// Note: uses null to represent there are no more deleted blocks
					CodeInfoWithLineInfo deletedBlock = indexDeletedBlock < deletedBlocks.size()
							? deletedBlocks.get(indexDeletedBlock)
							: null;

					// Iterate over "destination" code, since will have modified / added code
					// TODO: (then add in deleted code where it fits)
					for (CodeInfoWithLineInfo rightCode : result.getRight().getDetails()) {
						// leftCode may be null, which means that the code was added
						CodeInfoWithLineInfo leftCode = inverseMapCodeBlocks.get(rightCode);

						// 8/23/2019 changed from 'if' to 'while' to handle multiple deleted blocks in a row
						while (deletedBlock != null && leftCode != null
								&& deletedBlock.getStartLine() < leftCode.getStartLine()) {
							// The deleted block comes before the next modified block
							// Add the deleted block

							if (isTesting) {
								System.out.printf("Added deleted block %s before %s%n", deletedBlock, rightCode);
							}

							addChange(changes, changesMapPair, deletedBlock, null);

							indexDeletedBlock++;
							deletedBlock = indexDeletedBlock < deletedBlocks.size()
									? deletedBlocks.get(indexDeletedBlock)
									: null;
						}

						addChange(changes, changesMapPair, leftCode, rightCode);
					}

					// Add any remaining deleted blocks
					for (; indexDeletedBlock < deletedBlocks.size(); indexDeletedBlock++) {
						deletedBlock = deletedBlocks.get(indexDeletedBlock);

						addChange(changes, changesMapPair, deletedBlock, null);
					}

					// For each delta, determine where the changes are

					// TODO: how to handle moved lines?
					// How to handle if moved lines are in different methods?

					// Track how many differences and deltas are in each CodeInfo (extended range vs regular range)
					for (DiffUnit unit : diffBlocks) {
						if (BEXUtilities.in(unit.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
								BasicDiffType.IGNORE)) {
							continue;
						}

						ListMultimap<String, String> unknownChangeLines = MultimapBuilder.treeKeys()
								.arrayListValues()
								.build();

						for (DiffEdit edit : unit.getEdits()) {
							// Determine which change block corresponds to this edit
							// TODO: how to handle if in different ones (such as moved lines or lines seen in different blocks?)

							CodeInfoWithLineInfo leftCode = null;
							CodeInfoWithLineInfo rightCode = null;

							boolean isInExtendedLeftLines = false;
							boolean isInExtendedRightLines = false;

							if (edit.hasLeftLine()) {
								int line = edit.getLeftLineNumber();
								leftCode = ranges.getLeft().get(line);

								if (leftCode == null) {
									leftCode = extendedRanges.getLeft().get(line);

									if (leftCode != null) {
										isInExtendedLeftLines = true;
									}
								}
							}

							if (edit.hasRightLine()) {
								int line = edit.getRightLineNumber();
								rightCode = ranges.getRight().get(line);

								if (rightCode == null) {
									rightCode = extendedRanges.getRight().get(line);

									if (rightCode != null) {
										isInExtendedRightLines = true;
									}
								}
							}

							BEXPair<CompareDirectoriesJoinedDetail> change = changesMapPair.get(leftCode, rightCode);

							// XXX: continue here
							// Handle refactoring and ability to group changes together
							// For example, changing for loop from indexed to enhanced for loop
							// Renaming local variable
							// Other common refactoring
							// In this case, the code in normalized equal, since it has the same functionality

							//							LineChangeType lineChangeType = getJavaLineChangeType(edit);

							//							if (isTesting && lineChangeType == LineChangeType.UNKNOWN && change1 != null
							//									&& change2 != null) {
							//								// For modified methods, output testing info
							//
							//								System.out.println("getJavaLineChangeType: "
							//										+ (edit.getType().isSubstitution() ? System.lineSeparator() : "")
							//										+ edit.toString(true));
							//							}

							// TODO: indicate if the change is a commented out line

							if (change.testAndBoth(Objects::isNull)) {
								// Skip blank lines not associated to a code block
								if (edit.getLeftText().trim().isEmpty() && edit.getRightText().trim().isEmpty()) {
									continue;
								}

								if (isTesting) {
									System.out.println("Unknown difference (here): "
											+ (edit.isSubstitution() ? System.lineSeparator() : "")
											+ edit.toString(true));
								}

								// Add to unknown code change
								//								checkChange(edit, unknownChange, false, lineChangeType);

								String editSymbol = String.valueOf(edit.getSymbol());

								if (edit.hasLeftLine() && edit.hasRightLine()) {
									String lineInfo = edit.getLeftLineNumber() + " -> "
											+ edit.getRightLineNumber();

									// Add note with line numbers, so can easily find what changed
									unknownChange.addNote(editSymbol + " " + lineInfo);
								} else {
									String lineInfo = edit.getLineNumberString(edit.getFirstSide());
									unknownChangeLines.put(editSymbol, lineInfo);
								}

							} else if (change.test(Objects::equals)) {
								// Same change

								boolean isInExtendedLines;

								if (isInExtendedLeftLines == isInExtendedRightLines) {
									isInExtendedLines = isInExtendedLeftLines;
								} else if (edit.isMove()) {
									// If move and one sees it as part of extended lines, mark as part of extended lines
									isInExtendedLines = true;
								} else {
									// Changed method signature and was able to find substitution with commented out code
									// In this case, just presume that the substitutino is not in the extended lines, since one of them is not in the extended area
									isInExtendedLines = false;
									//														System.out.println(codeInfo + "\t" + bothCodeInfo);
									//														throw new AssertionError(
									//																"Difference is in both extended lines and regular lines: "
									//																		+ edit);
								}

								checkChange(edit, change.getLeft(), isInExtendedLines);
							} else if (change.getRight() == null) {
								// Deleted code
								checkChange(edit, change.getLeft(), isInExtendedLeftLines);
							} else if (change.getLeft() == null) {
								// Added code
								checkChange(edit, change.getRight(), isInExtendedRightLines);
							} else {
								boolean handleLeftChange = true;
								boolean handleRightChange = true;

								// TODO: Handle commented out code
								// (indicate where comment out or uncomment code)
								//								if (lineChangeType == LineChangeType.COMMENTED_OUT) {
								//									// "Destination" commented out code
								//									// Don't need to handle change 2
								//									handleChange2 = false;
								//
								//									if (isTesting) {
								//										System.out.println("Commented out line: " + edit);
								//									}
								//								} else if (lineChangeType == LineChangeType.UNCOMMENTED) {
								//									// "Source" has code commented out
								//									// Don't need to handle change 1
								//									handleChange1 = false;
								//
								//									if (isTesting) {
								//										System.out.println("Uncommented line: " + edit);
								//									}
								//								}

								// Modified code, but in different blocks
								// TODO: how to handle?

								// For now, just track the diff in each independent change block
								if (handleLeftChange) {
									checkChange(edit, change.getLeft(), isInExtendedLeftLines);
								}

								if (handleRightChange) {
									checkChange(edit, change.getRight(), isInExtendedRightLines);
								}

								if (isTesting && handleLeftChange && handleRightChange) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
									}
									System.err.println("DiffEdit spans multiple change blocks:");
									System.err.println(edit.toString(true));
									System.err.printf("Handle left  change? %s: %s%n", handleLeftChange,
											change.getLeft());
									System.err.printf("Handle right change? %s: %s%n", handleRightChange,
											change.getRight());
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
									}
								}
								//								throw new AssertionError(
								//										"Unsure how to handle diff:" + System.lineSeparator() + edit.toString(true));
							}
						}

						for (Entry<String, Collection<String>> entry : unknownChangeLines.asMap().entrySet()) {
							String symbol = entry.getKey();

							String lineInfo = entry.getValue().stream().collect(Collectors.joining(", "));

							unknownChange.addNote(symbol + " " + lineInfo);
						}
					}

					for (Iterator<CompareDirectoriesJoinedDetail> iterator = changes.iterator(); iterator.hasNext();) {
						CompareDirectoriesJoinedDetail change = iterator.next();

						if (change.getExtendedDifferenceCount() == 0 && change.getDifferenceCount() == 0) {
							// There was no change

							iterator.remove();
							continue;
						}
					}

					// For each modified item, compare the "source" and "destination" code
					for (CompareDirectoriesJoinedDetail change : changes) {
						//						if (change.getExtendedDifferenceCount() == 0 && change.getDifferenceCount() == 0) {
						//							// There was no change
						//							continue;
						//						}

						if (change.getLeftCode() == null || change.getRightCode() == null) {
							// ADDED, DELETED, or UNKNOWN code
							continue;
						}

						// Modified code

						// Do compare of the actual code range and get differences / deltas
						// If no deltas, set difference counts to 0, so won't show on report

						// TODO: Otherwise, use difference counts on details report??
						BEXPair<Range<Integer>> range = change.getCode()
								.map(c -> Range.closed(c.getExtendedStartLine(), c.getEndLine()));

						BEXListPair<DiffLine> lines = BEXListPair.from(side -> differencesResult.getLines(side)
								.stream()
								.filter(l -> range.get(side).contains(l.getNumber()))
								.collect(toList()));

						// Rerun differences for code in the specific range
						DifferencesResult rangeDifferencesResult = this.getDifferences(
								differencesResult.getRelativePath(),
								lines, differencesResult.getNormalizationFunction());

						long differences = getDifferenceCount(rangeDifferencesResult);
						long deltas = getDeltaCount(rangeDifferencesResult);

						if (isTesting) {
							System.out.printf("Diff: %s\t%s\t%s%n", change, differences, deltas);
						}

						if (deltas == 0) {
							// Ignore differences
							change.resetCounts();
						} else {
							change.setModifiedDifferences(differences);
							change.setModifiedDeltas(deltas);
						}
					}

					// For each change, output to spreadsheet

					for (CompareDirectoriesJoinedDetail change : changes) {
						//										System.out.println(
						//												change.getCodeInfo() + "\t" + change.getExtendedDifferenceCount() + "\t"
						//														+ change.getDifferenceCount());

						if (change.getExtendedDifferenceCount() == 0 && change.getDifferenceCount() == 0) {
							// There was no change
							change.setImpact(ImpactType.NONE);
							continue;
						}

						StringJoiner info = new StringJoiner(System.lineSeparator());

						for (String note : change.getNotes()) {
							info.add(note);
						}

						final PathChangeType changeType;
						final CodeInfoWithLineInfo codeInfo;

						if (change.getLeftCode() == null) {
							// Added or unknown code block

							if (change.getRightCode() == null) {
								// Unknown code block
								// TODO: could set based on whether unknown code was added / deleted or a combination
								changeType = PathChangeType.MODIFIED;
								codeInfo = null;
							} else {
								changeType = PathChangeType.ADDED;
								codeInfo = change.getRightCode();
							}
						} else {
							// Modified or deleted code block
							assert change.getLeftCode() != null;

							if (change.getRightCode() == null) {
								// Deleted code block
								changeType = PathChangeType.DELETED;
								codeInfo = change.getLeftCode();

								if (isTesting) {
									System.out.println(
											"Deleted change: " + change
													+ "\t"
													+ change.getLineChanges().entrySet());
								}

								//								int commentedOutLinesCount = change.getLineChanges()
								//										.count(LineChangeType.COMMENTED_OUT);

								//								if (commentedOutLinesCount > 0) {
								//									// Add 1 since inclusive on both sides
								//									int totalBodyLines = change.getCode1().getEndLine()
								//											- change.getCode1().getStartLine() + 1;
								//
								//									if (commentedOutLinesCount == totalBodyLines) {
								//										info.add("Commented out entire body");
								//									} else {
								//										info.add("Commented out part of body");
								//									}
								//								}
							} else {
								changeType = PathChangeType.MODIFIED;
								// Get "destination" code block, in case changed (such as method signature change)
								codeInfo = change.getRightCode();

								if (isTesting) {
									System.out.println(
											"Modified change: " + change
													+ "\t"
													+ change.getLineChanges().entrySet());
								}
							}
						}

						change.setPathChangeType(changeType);

						CodeInfoWithSourceInfo codeInfoWithSourceInfo = new CodeInfoWithSourceInfo(project,
								packageName,
								// Don't need to specify source pathname (last parameter), since not used for CompareDirectories
								className, codeInfo == null ? null : codeInfo.getCodeInfo(), "", null);

						// Will be null if not method

						String modifiers = codeInfoWithSourceInfo.getModifiers();

						String returnValue = getReturnValue(codeInfoWithSourceInfo);
						String signature = getSignature(codeInfoWithSourceInfo);

						if (codeInfoWithSourceInfo.isMethod()) {
							MethodSignature methodSignature = codeInfoWithSourceInfo
									.getMethodSignature();
							returnValue = methodSignature.getReturnValue();
							signature = methodSignature.getSignature();
						} else if (codeInfoWithSourceInfo.isField()) {
							FieldInfo fieldInfo = codeInfoWithSourceInfo.getFieldInfo();
							returnValue = fieldInfo.getType();
							signature = fieldInfo.getName();
						} else {
							returnValue = null;
							signature = null;
						}

						CodeType codeType = codeInfoWithSourceInfo.getCodeType();

						// Ignore unknown changes (where change.getLeftCode() == null)
						if (changeType == PathChangeType.MODIFIED && change.getLeftCode() != null) {
							// Get info for "source" code
							// If changed modifiers, return value, or, signature indicate on report
							CodeInfoWithSourceInfo leftCodeInfoWithSourceInfo = new CodeInfoWithSourceInfo(project,
									packageName,
									// Don't need to specify source pathname (last parameter), since not used for CompareDirectories
									className, change.getLeftCode().getCodeInfo(), "", null);

							String leftModifiers = leftCodeInfoWithSourceInfo.getModifiers();
							boolean hasModifiersChanged = !Objects.equals(leftModifiers, modifiers);

							String leftReturnValue = getReturnValue(leftCodeInfoWithSourceInfo);
							boolean hasReturnValueChanged = !Objects.equals(leftReturnValue, returnValue);

							String leftSignature = getSignature(leftCodeInfoWithSourceInfo);
							boolean hasSignatureChanged = !Objects.equals(leftSignature, signature);

							if (hasModifiersChanged || hasReturnValueChanged || hasSignatureChanged) {
								info.add("Refactored");

								if (hasModifiersChanged) {
									info.add(leftModifiers + " -> "
											+ modifiers);
								}

								if (hasReturnValueChanged) {
									info.add(leftReturnValue + " -> "
											+ returnValue);
								}

								if (hasSignatureChanged) {
									info.add(leftSignature + " -> "
											+ signature);
								}
							}
						}

						if (change.getDifferenceCount() == 0) {
							info.add("Has no change in actual body");
						}

						if (change.getExtendedDifferenceCount() != 0) {
							info.add("Has change before " + codeType.toString().toLowerCase());
						}

						boolean allComments = change
								.getCommentLinesCount() == change.getExtendedDifferenceCount()
										+ change.getDifferenceCount();

						if (allComments) {
							info.add("All changed lines are comments");

							if (change.isImpactBlank()) {
								change.setImpact(ImpactType.NONE);
							}
						}

						boolean allBlankLines = change.getBlankLinesCount() == change.getExtendedDifferenceCount()
								+ change.getDifferenceCount();

						if (allBlankLines) {
							info.add("All changed lines are blank lines");

							if (change.isImpactBlank()) {
								change.setImpact(ImpactType.NONE);
							}
						}

						boolean allCommentsOrBlankLines = change.getBlankLinesCount() != 0
								&& change.getCommentLinesCount() != 0
								&& change.getCommentLinesCount()
										+ change.getBlankLinesCount() == change.getExtendedDifferenceCount()
												+ change.getDifferenceCount();

						if (allCommentsOrBlankLines) {
							info.add("All changed lines are comments or blank lines");

							if (change.isImpactBlank()) {
								change.setImpact(ImpactType.NONE);
							}
						}

						if (change.isImpactBlank() && changeType == PathChangeType.ADDED && codeType == CodeType.FIELD
								&& Objects.equals(returnValue, "long")
								&& Objects.equals(signature, "serialVersionUID")) {
							change.setImpact(ImpactType.NONE);
						}

						if (change.isImpactBlank() && !change.getLineChanges().isEmpty()) {
							if (isTesting) {
								System.out.println(change.getLineChanges());
							}
							boolean isLowImpactChange = change.getLineChanges()
									.keySet()
									.stream()
									.allMatch(DiffType::shouldTreatAsNormalizedEqual);

							if (isLowImpactChange) {
								change.setImpact(ImpactType.LOW);
								info.add("Max impact of changed lines is low");
							}
						}

						//						if (change.isImpactBlank()) {
						//							ImpactType maxImpactType = change.getLineChanges()
						//									.stream()
						//									.map(c -> c.getImpactType())
						//									.max(Comparator.comparing(ImpactType::getImpact))
						//									.orElse(ImpactType.UNKNOWN);
						//
						//							if (maxImpactType == ImpactType.NONE) {
						//								change.setImpact(ImpactType.NONE);
						//
						//								info.add("None of the line changes have any impact");
						//							} else if (maxImpactType == ImpactType.LOW) {
						//								change.setImpact(ImpactType.LOW);
						//
						//								info.add("Max impact of changed lines is low");
						//							}
						//						}

						int shortMethodLineCount = 5;

						if (change.isImpactBlank() && changeType == PathChangeType.ADDED
								&& codeInfoWithSourceInfo.isMethod()
								&& Objects.requireNonNull(codeInfo).getLineCount() <= shortMethodLineCount) {
							change.setImpact(ImpactType.LOW);
							info.add("Short method with " + codeInfo.getLineCount()
									+ " lines");
						}

						if (change.isImpactBlank() && changeType == PathChangeType.MODIFIED
								&& codeInfoWithSourceInfo.isMethod()
								&& codeInfo != null
								&& codeInfo.getLineCount() <= shortMethodLineCount
								&& change.getLeftCode().getLineCount() <= shortMethodLineCount) {
							change.setImpact(ImpactType.LOW);
							info.add("Short method with " + codeInfo.getLineCount()
									+ " lines");
						}

						boolean showDeltas = changeType == PathChangeType.MODIFIED && change.getLeftCode() != null
								&& change.getRightCode() != null;

						String differences = (showDeltas ? String.valueOf(change.getModifiedDifferences()) : "");
						String deltas = (showDeltas ? String.valueOf(change.getModifiedDeltas()) : "");

						ImpactType impact = change.getImpact();
						String impactValue = (impact == null ? null : impact.toString());

						String infoText = info.toString();

						if (!infoText.isEmpty() && (infoText.charAt(0) == '+' || infoText.charAt(0) == '-')) {
							// Make Excel happy, so doesn't think cell is a formula and complain
							infoText = "'" + infoText;
						}

						// TODO: Combine EJB and WebProjects together (unless Excel is fussy)
						String detailsSheetName = project;
						//						String detailsSheetName = "Details";

						String projectSpecificDetailsSheetName = projectSheetNameMap.get(project);

						if (projectSpecificDetailsSheetName != null) {
							detailsSheetName = projectSpecificDetailsSheetName;
						}

						XSSFSheet detailsSheet = workbook.getSheet(detailsSheetName);

						// First time using sheet
						if (detailsSheet == null) {
							detailsSheet = workbook.createSheet(detailsSheetName);

							ExcelUtilities.createHeaderRow(detailsSheet, 0, detailsHeaderColumnNames);
						}

						Row row = ExcelUtilities.addRow(detailsSheet, packageName, className,
								changeType.toString(), codeType.toString(), impactValue, modifiers, returnValue,
								signature, infoText, differences, deltas);

						// Wrap text for method and info column
						row.getCell(packageColumn).setCellStyle(wrapTextCellStyle);
						row.getCell(classColumn).setCellStyle(wrapTextCellStyle);

						if (modifiers != null) {
							row.getCell(modifiersColumn).setCellStyle(wrapTextCellStyle);
						}

						if (returnValue != null) {
							row.getCell(returnColumn).setCellStyle(wrapTextCellStyle);

						}

						if (signature != null) {
							row.getCell(methodColumn).setCellStyle(wrapTextCellStyle);
						}

						row.getCell(infoColumn).setCellStyle(wrapTextCellStyle);
					}
				}
			}

			System.out.println("Formatting reports");

			// ParsingUtilities.makeReportsPretty(sheet, lastColumn);

			// Add filtering
			// (start on first row, so exclude title row)
			sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, lastColumn));

			// Freeze header row
			sheet.createFreezePane(0, 1);

			// Don't autosize columns due to issue processing so many rows
			// Autosize columns
			// ParsingUtilities.autosizeColumnsFromSheet(sheet, 0, lastColumn);

			sheet.setColumnWidth(projectColumn, 33 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

			sheet.setColumnWidth(directoryColumn, 33 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

			sheet.setColumnWidth(filenameColumn, 21 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

			sheet.setColumnWidth(fileTypeColumn, 12 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
			sheet.setColumnWidth(differencesChangeTypeColumn, 10 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
			sheet.setColumnWidth(differencesColumn, 15 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
			sheet.setColumnWidth(deltasColumn, 15 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

			sheet.setColumnWidth(pathnameColumn, 33 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

			// Format all the sheets except the first Differences sheet
			for (Sheet sheet0 : workbook) {
				if (BEXUtilities.in(sheet0.getSheetName(), sheetName)) {
					continue;
				}

				// Add filtering
				// (start on first row, so exclude title row)
				sheet0.setAutoFilter(new CellRangeAddress(0, sheet0.getLastRowNum(), 0, lastDetailsColumn));

				// Freeze header row
				sheet0.createFreezePane(0, 1);

				// Don't autosize columns due to issue processing so many rows
				// Autosize columns
				// ParsingUtilities.autosizeColumnsFromSheet(sheet, 0, lastColumn);

				// Format remaining two columns (others are specified below)
				sheet0.setColumnWidth(classColumn, 21 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				sheet0.setColumnWidth(detailsChangeTypeColumn, 10 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				// Allows CONSTRUCTOR to fully display
				sheet0.setColumnWidth(typeColumn, 14 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				sheet0.setColumnWidth(modifiersColumn, 12 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				sheet0.setColumnWidth(packageColumn, 21 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				sheet0.setColumnWidth(returnColumn, 20 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
				sheet0.setColumnWidth(methodColumn, 50 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
				sheet0.setColumnWidth(infoColumn, 95 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				sheet0.setColumnWidth(detailsDifferencesColumn, 15 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);
				sheet0.setColumnWidth(detailsDeltasColumn, 15 * EXCEL_COLUMN_CHARACTER_MULTIPLIER);

				//				sheet0.setColumnWidth(linesColumn, 7 * ParsingUtilities.EXCEL_COLUMN_CHARACTER_MULTIPLIER);
			}

			System.out.println("Writing report at: " + excelReportPath);
			try (OutputStream outputStream = Files.newOutputStream(excelReportPath)) {
				workbook.write(outputStream);
				System.out.println("Report saved at: " + excelReportPath);

				if (copyChangedFilesDestinationPath != null) {
					Path source = excelReportPath;
					Path destination = copyChangedFilesDestinationPath.resolve(excelReportFilename);

					Files.copy(source, destination);
					System.out.println("Saved a copy of the report to: " + destination);
				}
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.printf("It took %d seconds.", (endTime - startTime) / 1000);
	}

	private static String getReturnValue(final CodeInfoWithSourceInfo codeInfoWithSourceInfo) {
		if (codeInfoWithSourceInfo.isMethod()) {
			MethodSignature methodSignature = codeInfoWithSourceInfo
					.getMethodSignature();

			return methodSignature.getReturnValue();
		} else if (codeInfoWithSourceInfo.isField()) {
			FieldInfo fieldInfo = codeInfoWithSourceInfo.getFieldInfo();

			return fieldInfo.getType();
		} else {
			return null;
		}
	}

	private static String getSignature(final CodeInfoWithSourceInfo codeInfoWithSourceInfo) {
		if (codeInfoWithSourceInfo.isMethod()) {
			MethodSignature methodSignature = codeInfoWithSourceInfo
					.getMethodSignature();

			return methodSignature.getSignature();
		} else if (codeInfoWithSourceInfo.isField()) {
			FieldInfo fieldInfo = codeInfoWithSourceInfo.getFieldInfo();

			return fieldInfo.getName();
		} else {
			return null;
		}
	}

	private static int getDifferenceCount(final DifferencesResult differencesResult) {
		return (int) differencesResult.getDiff()
				.stream()
				.filter(
						d -> !BEXUtilities.in(d.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
								BasicDiffType.IGNORE))
				.count();
	}

	private static int getDeltaCount(final DifferencesResult differencesResult) {
		return (int) differencesResult.getDiffBlocks()
				.stream()
				.filter(
						d -> !BEXUtilities.in(d.getType(), BasicDiffType.EQUAL, BasicDiffType.NORMALIZE,
								BasicDiffType.IGNORE))
				.count();
	}

	/**
	 * Gets the differences
	 *
	 * <p><b>NOTE</b>: this method may be overridden to implement your own functionality</p>
	 * @param relativePath the relative path for file to compare
	 * @param lines the contents of the file
	 * @param normalizationFunction the normalization function to use
	 * @return the DifferencesResult
	 */
	public DifferencesResult getDifferences(final Path relativePath, final BEXListPair<DiffLine> lines,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		List<DiffEdit> diff = PatienceDiff.diff(lines.getLeft(), lines.getRight(), normalizationFunction,
				MyersLinearDiff.with(normalizationFunction));

		List<SubstitutionType> substitutionTypes = this.substitutionTypes.stream()
				.map(s -> s.apply(relativePath, lines))
				.collect(toList());

		if (!this.excludeLCSMaxSubstitution) {
			substitutionTypes.add(LCS_MAX_OPERATOR);
		}

		// Look first for common refactorings, so can group changes together
		DiffHelper.handleSubstitution(diff, normalizationFunction, substitutionTypes.toArray(new SubstitutionType[0]));

		// Do separately, so LCS max can find better matches and do only run LCS min on leftovers
		DiffHelper.handleSubstitution(diff, normalizationFunction, SubstitutionType.LCS_MIN_OPERATOR);

		// TODO: handle split lines BEFORE handle moved lines
		// (sometimes was seeing a move, which prevented it from recognizing as split lines)

		//		DiffHelper.handleMovedLines(diff, normalizationFunction);

		//		DiffHelper.ignoreBlankLines(diff);

		List<DiffUnit> diffBlocks = DiffHelper.combineToDiffBlocks(diff, true);

		DiffHelper.handleSplitLines(diffBlocks, normalizationFunction);

		DiffHelper.handleBlankLines(diffBlocks, normalizationFunction);

		return new DifferencesResult(relativePath, lines, normalizationFunction, diff, diffBlocks);
	}

	private static void addChange(final List<CompareDirectoriesJoinedDetail> changes,
			final BEXMapPair<CodeInfoWithLineInfo, CompareDirectoriesJoinedDetail> changesMapPair,
			final CodeInfoWithLineInfo leftCode, final CodeInfoWithLineInfo rightCode) {

		CompareDirectoriesJoinedDetail change = new CompareDirectoriesJoinedDetail(leftCode, rightCode);

		changes.add(change);

		if (leftCode != null) {
			changesMapPair.getLeft().put(leftCode, change);
		}

		if (rightCode != null) {
			changesMapPair.getRight().put(rightCode, change);
		}
	}

	private static void checkChange(final DiffEdit edit, final CompareDirectoriesJoinedDetail change,
			final boolean isInExtendedLines) {
		//			final boolean isInExtendedLines, final LineChangeType lineChangeType) {

		DiffType diffType = edit.getType();
		change.addDifference(isInExtendedLines);
		change.addLineChange(diffType);

		boolean shouldShowChange = !edit.shouldTreatAsNormalizedEqual();

		if (shouldShowChange) {
			change.addNote(edit.toString(true));
		} else {
			change.addNote(diffType.toString());
		}

		// If deprecated method, want to indicate this as a line change, so if method body didn't change, can mark change as having no impact
		//		if (!isInExtendedLines || lineChangeType.getImpactType() == ImpactType.NONE) {
		//			change.addLineChange(lineChangeType);
		//		}

		if (isTesting) {
			System.out.println(edit.toString(true));
		}

		if (edit.isInsertOrDelete() || edit.isMove()) {
			String text = edit.getText().trim();

			if (text.startsWith("//")) {
				change.addCommentLine();
			} else if (text.isEmpty()) {
				change.addBlankLine();
			}
		}
	}

	private boolean shouldReportAdd(final Path path, final FileType type) {
		if (type == FileType.DIRECTORY) {
			if (this.flattenReportingOfAddedDirectories) {
				// Only report if contains files (versus directories) or has no contents (deepest level)
				// Report result if there are any files in the directory
				try {
					if (Files.list(path).anyMatch(x -> true)) {
						return Files.list(path)
								.anyMatch(Files::isRegularFile);
					}
				} catch (IOException e) {
				}
			}
		}

		// Report result unless otherwise specified
		return true;
	}

	/**
	 * Indicates if the path should be checked for the compare
	 *
	 * <p><b>NOTE</b>: this method may be overridden to implement your own functionality</p>
	 * @param path the path to check
	 * @return <code>true</code> if the path should be checked as part of the compare
	 */
	public boolean shouldCheckPath(final Path path) {
		Path fileNamePath = path.getFileName();

		if (fileNamePath == null) {
			return false;
		}

		String filename = fileNamePath.toString();

		// Ignore .class files
		if (filename.endsWith(".class")) {
			return false;
		}

		int pathPartCount = path.getNameCount();

		for (int i = 0; i < pathPartCount; i++) {
			String part = path.getName(i).toString();

			// Ignore if part starts with a '.' (such as .svn)
			if (part.startsWith(".")) {
				return false;
			}

			if (this.ignoreProjects.contains(part)) {
				return false;
			}
		}

		return true;
	}

	public static String readFileContents(final Path path) throws IOException {
		return new String(Files.readAllBytes(path));
	}

	/**
	 *
	 *
	 * @param relativePath path relative to workspace
	 * @return
	 */
	private static String getProject(final Path relativePath) {
		Path parent = relativePath.getParent();

		String project = (parent == null ? "" : parent.getName(0).toString());

		return project;
	}

	/**
	 *
	 *
	 * @param relativePath path relative to workspace
	 * @return
	 */
	private static String getDirectory(final Path relativePath) {
		Path parent = relativePath.getParent();

		String directory = (parent == null || parent.getNameCount() == 1
				? ""
				: parent.subpath(1, parent.getNameCount()).toString());

		return directory;
	}

	/**
	 *
	 *
	 * @param relativePath path relative to workspace
	 * @return
	 */
	private static String getFilename(final Path relativePath) {
		Path pathFilename = relativePath.getFileName();

		String filename = (pathFilename == null ? "" : pathFilename.toString());

		return filename;
	}

	/**
	 *
	 *
	 * @param text
	 * @return
	 */
	private static List<DiffLine> splitLines(final String text) {
		List<String> textLines = lineSplitter.splitToList(text);

		return IntStream.range(0, textLines.size())
				.mapToObj(i -> new DiffLine(i + 1, textLines.get(i)))
				//				.mapToObj(i -> new DiffLine(i, textLines.get(i)))
				.collect(Collectors.toList());
	}

	private static BEXMapPair<String, CompareJavaCodeInfo> parse(final BEXPair<ASTParser> parser,
			final BEXListPair<ProjectPath> javaPaths, final Map<Path, DifferencesResult> diffs) {

		return new BEXMapPair<>(parse(LEFT, parser, javaPaths, diffs), parse(RIGHT, parser, javaPaths, diffs));
	}

	private static Map<String, CompareJavaCodeInfo> parse(final BEXSide side, final BEXPair<ASTParser> parserPair,
			final BEXListPair<ProjectPath> javaPaths, final Map<Path, DifferencesResult> diffs) {
		Map<String, CompareJavaCodeInfo> results = new ConcurrentHashMap<>();

		ASTParser parser = parserPair.get(side);

		String[] sourcePathnames = javaPaths.get(side).stream().map(ProjectPath::getPathname).toArray(String[]::new);

		// TODO: multi-thread using ExecutorService
		ExecutorService executorService = Executors.newWorkStealingPool();

		parser.createASTs(sourcePathnames, null, new String[0],
				new FileASTRequestor() {
					@Override
					public void acceptAST(final String sourcePathname,
							final CompilationUnit cu) {

						//						System.out.println("Parsing " + sourcePathname);

						executorService.execute(() -> {
							Path path = Paths.get(sourcePathname);
							CompareDirectoriesVisitor visitor = new CompareDirectoriesVisitor(cu,
									diffs.get(path).getLines(side));
							cu.accept(visitor);

							String packageName = "";

							PackageDeclaration packageDeclaration = cu.getPackage();

							if (packageDeclaration != null) {
								packageName = packageDeclaration.getName().toString();
							}

							results.put(sourcePathname,
									new CompareJavaCodeInfo(packageName, visitor.getDetails()));
						});
					}
				}, null);

		// https://www.baeldung.com/java-executor-wait-for-threads
		// Set to 60 minutes just in case it takes a while
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException ex) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}

		return results;
	}

	private static int countBlankLines(final List<DiffLine> lines, final CodeInfoWithLineInfo code) {
		return (int) lines.stream()
				// Count number of blank lines
				.filter(l -> code.contains(l.getNumber()))
				.filter(l -> l.getText().trim().isEmpty())
				.count();
	}

	public CompareDirectoriesResult findDifferences(final BEXPair<Path> rootPath)
			throws IOException {
		BEXListPair<Path> paths = new BEXListPair<>(rootPath.mapThrows(r -> Files.walk(r)
				// Run in parallel for performance boost
				.parallel()
				.filter(this::shouldCheckPath)
				// Sort so can iterate over and find differences
				.sorted()
				.collect(toList())));

		List<PathChangeInfo> pathChanges = new Vector<>();

		BEXListPair<ProjectPath> javaPaths = new BEXListPair<>(Vector::new);

		// Map from path to list of differences
		// (path will be the destination path)
		Map<Path, DifferencesResult> javaPathDiffMap = new ConcurrentHashMap<>();

		MutableIntBEXPair index = new MutableIntBEXPair();

		ExecutorService executorService = Executors.newWorkStealingPool();

		while (index.getLeft() < paths.getLeft().size() && index.getRight() < paths.getRight().size()) {
			BEXPair<Path> path = paths.get(index);
			BEXPair<FileType> fileType = path.map(FileType::determineFileType);

			BEXPair<Path> relativePath = new BEXPair<>(side -> rootPath.get(side).relativize(path.get(side)));

			int compare = relativePath.applyAsInt(Path::compareTo);
			BEXSide side = compare < 0 ? LEFT : RIGHT;

			// Progress output
			// Check if remaining is 0 or 1
			// Since both left index and right index could increase together, check 0 and 1 remaining, so don't miss a progress output
			// (if only checked remainder 0, could have remainder 1 for a long time and not get progress indicator)
			if ((index.getLeft() + index.getRight()) % 1000 <= 1) {
				System.out.printf("Checking %s%n", relativePath.get(side));
			}

			if (compare < 0) {
				// Left path is before right path
				// This means left path does not exist in the right directory
				// (that is, the path was deleted)
				index.incrementAndGet(side);
				executorService.execute(
						() -> pathChanges.add(createDifference(side, relativePath, fileType, PathChangeType.DELETED)));
			} else if (compare > 0) {
				// Left path is after right path
				// This means right path does not exist in the left directory
				// (that is, the path was added)
				index.incrementAndGet(side);

				if (this.shouldReportAdd(path.get(side), fileType.get(side))) {
					executorService.execute(() -> pathChanges
							.add(createDifference(side, relativePath, fileType, PathChangeType.ADDED)));
				}
			} else {
				// Same name
				index.increment();

				// Verify type matches
				if (fileType.test(Objects::equals)) {
					if (fileType.get(side) == FileType.FILE) {
						executorService.execute(() -> this.determineFileChanges(path, relativePath.get(side),
								pathChanges, javaPaths, javaPathDiffMap));

						// XXX: add back function to test specific path for differences
						// (implement outside of this, by filtering the paths)
						//						if (isTesting && !file.get(side).getName().equals(testingFile)) {
						//							continue;
						//						}

					}

					// Note: If directory, doesn't matter that match, only need to compare files
				} else {
					// Type differs
					// TODO: write to report as difference
					System.err.printf("'%s'\t'%s' (%s)\t'%s' (%s)%n", relativePath.get(side),
							path.getLeft(), fileType.getLeft(),
							path.getRight(), fileType.getRight());

					// Show as add and delete

					// Show directory last, so groups directory and any subfolder's files together
					if (fileType.getLeft() == FileType.DIRECTORY) {
						executorService.execute(() -> {
							pathChanges.add(createDifference(RIGHT, relativePath, fileType, PathChangeType.ADDED));
							pathChanges.add(createDifference(LEFT, relativePath, fileType, PathChangeType.DELETED));
						});
					} else {
						executorService.execute(() -> {
							pathChanges.add(createDifference(LEFT, relativePath, fileType, PathChangeType.DELETED));
							pathChanges.add(createDifference(RIGHT, relativePath, fileType, PathChangeType.ADDED));
						});
					}
				}
			}
		}

		// https://www.baeldung.com/java-executor-wait-for-threads
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException ex) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}

		// Sort them, to ensure consistent ordering, even though multi-threaded
		pathChanges.sort(Comparator.comparing(PathChangeInfo::getRelativePath));

		javaPaths.acceptBoth(
				l -> l.sort(Comparator.comparing(ProjectPath::getProject).thenComparing(ProjectPath::getPath)));

		// Handle rest of paths
		// * Any remaining in leftPaths were deleted (since they don't appear in rightPaths)
		// * Any remaining in rightPaths were added (since they don't appear in leftPaths)

		// Only one side (if any) will have extra paths
		// (since the above loop wouldn't have exited if they both had extra paths)
		BEXSide side = index.getLeft() < paths.getLeft().size() ? LEFT : RIGHT;
		// TODO: support added stuff is on the left side?
		PathChangeType pathChangeType = side == LEFT ? PathChangeType.DELETED : PathChangeType.ADDED;

		while (index.get(side) < paths.get(side).size()) {
			Path path = paths.get(side).get(index.getAndIncrement(side));
			FileType fileType = FileType.determineFileType(path);

			if (!isTesting) {
				System.out.printf("Extra %s: '%s' (%s)%n", pathChangeType, path, fileType);
			}

			boolean shouldInclude = pathChangeType == PathChangeType.DELETED || this.shouldReportAdd(path, fileType);

			if (shouldInclude) {
				Path relativePath = rootPath.getLeft().relativize(path);

				pathChanges.add(createDifference(relativePath, fileType, pathChangeType));
			}
		}

		return new CompareDirectoriesResult(pathChanges, javaPaths, javaPathDiffMap);
	}

	private void determineFileChanges(final BEXPair<Path> path, final Path relativePath,
			final List<PathChangeInfo> pathChanges, final BEXListPair<ProjectPath> javaPaths,
			final Map<Path, DifferencesResult> javaPathDiffMap) {
		// Path exists in both workspaces

		BEXPair<String> text;
		try {
			text = path.mapThrows(CompareDirectories::readFileContents);
		} catch (IOException e) {
			// If not able to read file, add as difference
			pathChanges.add(createDifference(relativePath, FileType.FILE, PathChangeType.MODIFIED));
			return;
		}

		if (!text.test(Object::equals)) {
			// File was modified
			if (!isTesting) {
				System.out.printf("Modified '%s' (%s)%n", relativePath, FileType.FILE);
			}

			DifferencesResult differencesResult;
			int differenceCount;
			int deltas;

			if (this.skipTextCompare.test(path.getRight())) {
				// Do not compare text, but still indicate difference
				differencesResult = null;
				// Use negates so don't show counts on report (since didn't calculate counts)
				differenceCount = -1;
				deltas = -1;
			} else {
				// Get differences
				// (TODO: do something with the differences)
				differencesResult = this.getDifferences(relativePath,
						new BEXListPair<>(text.map(CompareDirectories::splitLines)),
						DiffHelper.WHITESPACE_NORMALIZATION_FUNCTION);

				differenceCount = getDifferenceCount(differencesResult);

				// Count only the number of blocks which have differences
				deltas = getDeltaCount(differencesResult);
			}

			pathChanges.add(createDifference(relativePath, FileType.FILE, PathChangeType.MODIFIED,
					differenceCount, deltas));

			if (deltas > 0 && path.getRight().toString().endsWith(".java")) {
				// If has differences, parse Java code so can split into various methods
				String project = getProject(relativePath);

				BEXPair<ProjectPath> projectPath = path.map(f -> new ProjectPath(project, f));

				// Track paths so can parse all at once (otherwise need to reset settings, which kills performance)
				javaPaths.add(projectPath);

				// 8/28/2019 Put both left path and right path in map to help read diff / line information when parsing code
				path.acceptBoth(f -> javaPathDiffMap.put(f, differencesResult));
			}
		}
	}

	private static PathChangeInfo createDifference(final BEXSide side, final BEXPair<Path> relativePath,
			final BEXPair<FileType> fileType, final PathChangeType changeType) {
		return createDifference(relativePath.get(side), fileType.get(side), changeType);
	}

	private static PathChangeInfo createDifference(final Path relativePath, final FileType fileType,
			final PathChangeType changeType) {
		// Pass negative number so don't include count
		return createDifference(relativePath, fileType, changeType, -1, -1);
	}

	/**
	 *
	 * @param relativePath
	 * @param fileType
	 * @param changeType
	 * @param differenceCount the number of differences (or negative to not include count)
	 * @return
	 */
	private static PathChangeInfo createDifference(final Path relativePath, final FileType fileType,
			final PathChangeType changeType, final int differenceCount, final int deltaCount) {

		if (isTesting) {
			System.out.println("Add difference: " + relativePath);
		}

		String project = getProject(relativePath);
		String directory = getDirectory(relativePath);
		String filename = getFilename(relativePath);

		// Get extension
		String extension;
		int lastPeriod = filename.lastIndexOf('.');

		if (lastPeriod == -1 || fileType != FileType.FILE) {
			extension = "";
		} else {
			extension = filename.substring(lastPeriod + 1);
			// Remove extension from filename
			filename = filename.substring(0, lastPeriod);
		}

		return new PathChangeInfo(relativePath, project, directory, filename, extension, fileType, changeType,
				differenceCount,
				deltaCount);
	}

	/**
	 *
	 *
	 * @param sheet
	 * @return
	 */
	private static Row addDifference(final XSSFSheet sheet, final PathChangeInfo change) {
		if (isTesting) {
			System.out.println("Add difference: " + change.getRelativePath());
		}

		// Add most values, others added below
		Row row = ExcelUtilities.addRow(sheet, change.getProject(), change.getDirectory(),
				change.getFilenameWithoutExtension(), change.getExtension(), change.getFileType().toString(),
				change.getPathChangeType().toString());

		row.getCell(DIRECTORY_COLUMN).setCellStyle(WRAP_TEXT_CELL_STYLE);
		row.getCell(FILENAME_COLUMN).setCellStyle(WRAP_TEXT_CELL_STYLE);

		int differenceCount = change.getDifferenceCount();
		Cell differencesCell = row.createCell(DIFFERENCES_COLUMN);

		if (differenceCount >= 0) {
			differencesCell.setCellValue(differenceCount);
		}

		int deltaCount = change.getDeltaCount();
		Cell deltasCell = row.createCell(DELTAS_COLUMN);

		if (deltaCount >= 0) {
			deltasCell.setCellValue(deltaCount);
		}

		Cell pathnameCell = row.createCell(PATHNAME_COLUMN);

		pathnameCell.setCellValue(change.getRelativePath().toString());
		pathnameCell.setCellStyle(WRAP_TEXT_CELL_STYLE);

		// TODO: separate out the copying of the changes files to another directory
		//		if (CompareDirectories.copyChangedFilesSourcePath != null
		//				&& Utilities.in(change.getFileChangeType(), FileChangeType.ADDED, FileChangeType.MODIFIED)) {
		//			Path source = copyChangedFilesSourcePath.resolve(path);
		//			Path destination = copyChangedFilesDestinationPath.resolve(path);
		//
		//			if (!Files.isDirectory(destination)) {
		//				// Copy file from source to destination
		//				try {
		//					Path parent = destination.getParent();
		//
		//					if (parent != null) {
		//						Files.createDirectories(parent);
		//					}
		//					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING,
		//							StandardCopyOption.COPY_ATTRIBUTES);
		//					System.out.printf("Copied file%n"
		//							+ "\tfrom \"%s\"%n"
		//							+ "\tto \"%s\"%n", source, destination);
		//				} catch (IOException e) {
		//					// Continue, since not end of world if cannot copy file
		//					System.err.printf("Could not copy file%n"
		//							+ "\t\from \"%s\"%n"
		//							+ "\tto \"%s\"%n", source, destination);
		//					e.printStackTrace();
		//				}
		//			}
		//		}

		return row;
	}
}
