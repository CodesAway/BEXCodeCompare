package info.codesaway.becr.parsing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;

import info.codesaway.bex.util.Utilities;

public class ParsingUtilities {
	static final String lineSeparator = System.getProperty("line.separator");

	/**
	 * Cannot instantiate utility class
	 *
	 * @throws UnsupportedOperationException
	 */
	private ParsingUtilities() {
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 * @param stringBuilder
	 *        the StringBuilder
	 * @param text
	 *        the text to append
	 * @return the inputed StringBuilder
	 * @throws IOException
	 */
	public static StringBuilder writeLine(final StringBuilder stringBuilder, final CharSequence text) {
		return stringBuilder.append(text).append(lineSeparator);
	}

	// /**
	// * Write HTML line
	// *
	// * <p>
	// * <code>line</code>, followed by <code>&lt;br&gt;</code>, followed by the line separator (<code>\r\n</code> for
	// * Windows)
	// *
	// * @param writer
	// * @param line
	// * @return
	// * @throws IOException
	// */
	// public static Writer writeHtmlLine(final Writer writer, final String line)
	// throws IOException
	// {
	// writer.write(line);
	// writer.write("<br>");
	// writer.write(lineSeparator);
	// return writer;
	// }

	// Used when creating excel reports
	/**
	 * Min column width (to prevent column header being only large enough for the arrow, and no clickable text)
	 */
	public static final int EXCEL_COLUMN_CHARACTER_MULTIPLIER = 256;

	private static final int MIN_COLUMN_WIDTH = 5 * EXCEL_COLUMN_CHARACTER_MULTIPLIER;
	private static final int MAX_COLUMN_WIDTH = 255 * EXCEL_COLUMN_CHARACTER_MULTIPLIER;

	// Calculated from difference between autosize in Excel with and without arrow
	private static final int WIDTH_ARROW_BUTTON = 620;

	/**
	 * Resizes the columns to fit the contents, while not allowing the resizing to make the columns shorter.
	 *
	 * @param excelSheet
	 * @param fromColumn
	 *        start of range of columns to autosize
	 * @param toColumn
	 *        end of range of columns to autosize (inclusive)
	 */
	public static void autosizeColumnsFromSheet(final Sheet excelSheet, final int fromColumn, final int toColumn) {
		autosizeColumnsFromSheet(excelSheet, fromColumn, toColumn, -1);
	}

	/**
	 * Resizes the columns to fit the contents.
	 *
	 * <ul>
	 * <li>If <code>smartResizing</code> &lt; 0, this method will not allow resizing to make the columns shorter.</li>
	 * <li>If <code>smartResizing</code> = 0, this method will resize the columns solely based on the contents of the
	 * columns
	 * <li>If <code>smartResizing</code> is &gt; 0 then the column can be resized to be smaller provided that the new
	 * column width is at least <code>smartResizing</code> width smaller than the current width.
	 * </ul>
	 *
	 * @param excelSheet
	 * @param fromColumn
	 *        start of range of columns to autosize
	 * @param toColumn
	 *        end of range of columns to autosize (inclusive)
	 */
	// Based on source:
	// http://stackoverflow.com/a/17587756
	public static void autosizeColumnsFromSheet(final Sheet excelSheet, final int fromColumn, final int toColumn,
			final int smartResizing) {
		for (int i = fromColumn; i <= toColumn; i++) {
			int previousWidth = excelSheet.getColumnWidth(i);
			excelSheet.autoSizeColumn(i);
			int autoSizeWidth = excelSheet.getColumnWidth(i);

			if (excelSheet.getRow(0) != null) {
				// Only include the width of the arrow if the header row is part of the width calculation
				autoSizeWidth += WIDTH_ARROW_BUTTON;
			}

			int newWidth = Math.min(autoSizeWidth, MAX_COLUMN_WIDTH);

			if (smartResizing < 0) {
				if (newWidth < previousWidth) {
					// Don't allow resizing to make smaller

					// Accounts for fact that the excel file only keeps 100 rows in memory,
					// so the headers (which are usually long) don't get factored into
					// the width when finished with the sheet

					newWidth = previousWidth;
				}
			} else if (smartResizing > 0) {
				if (newWidth < previousWidth && previousWidth < newWidth + smartResizing * 256) {
					// Keep previous width if new width is smaller than previous width
					// (allowing for a decrease up to X width - prevents really long column names from hogging space)
					// (such as if actual column contents are small - like tax number)
					newWidth = previousWidth;
				}
			}
			// else smartResizing = 0 and smart resizing is disabled (uses the default auto size result)

			if (newWidth < MIN_COLUMN_WIDTH) {
				newWidth = MIN_COLUMN_WIDTH;
			}

			excelSheet.setColumnWidth(i, newWidth);
		}
	}

	public static Row addRow(final Sheet sheet, final String... columnValues) {
		Row row = sheet.createRow(sheet.getLastRowNum() + 1);

		for (int i = 0; i < columnValues.length; i++) {
			Cell cell = row.createCell(i);
			cell.setCellValue(columnValues[i]);
		}

		return row;
	}

	/**
	 * Creates the header row
	 *
	 * @param sheet
	 *        the sheet
	 * @param row
	 *        the row
	 * @param headerColumnNames
	 *        the header column names
	 */
	public static void createHeaderRow(final Sheet sheet, final int row, final List<String> headerColumnNames) {
		Row headerRow = sheet.createRow(row);

		for (int i = 0; i < headerColumnNames.size(); i++) {
			String columnName = headerColumnNames.get(i);

			Cell headerCell = headerRow.createCell(i);
			headerCell.setCellValue(columnName);
		}
	}

	/**
	 * Make excel reports pretty
	 *
	 * <ul>
	 * <li>Add filtering for sheet</li>
	 * <li>Freeze the header row (first row)</li>
	 * <li>Auto-size columns</li>
	 * </ul>
	 * .
	 *
	 * @param sheet
	 *        the sheet
	 * @param lastColumn
	 *        the 0-based index for the last column populated in the sheet
	 */
	public static void makeReportsPretty(final Sheet sheet, final int lastColumn) {
		// Add filtering
		// (start on first row, so exclude title row)
		sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, lastColumn));

		// Freeze header row
		sheet.createFreezePane(0, 1);

		// Autosize columns
		ParsingUtilities.autosizeColumnsFromSheet(sheet, 0, lastColumn);
	}

	/**
	 * Gets the excel column name from the column number
	 *
	 * @param column column number (starting with 1)
	 * @return excel column name (A for column 1, Z for column 26, AA for column 27)
	 */
	public static String getExcelColumnName(final int column) {
		if (column < 1) {
			throw new IllegalArgumentException("Column number must be positive: " + column);
		}

		// Essentially converting to base 26 number with A=1 and Z=26
		StringBuilder result = new StringBuilder();
		int value = column;
		int base = 26;

		while (value > 0) {
			int remainder = (value - 1) % base;

			result.insert(0, (char) ('A' + remainder));

			value = (value - 1) / base;
		}

		return result.toString();
	}

	/**
	 * Gets the excel column number from the column name
	 *
	 * @param column column name
	 * @return excel column number (1 for column A, 26 for column Z, 27 for column AA)
	 */
	public static int getExcelColumnNumber(final String columnName) {
		int result = 0;

		for (int i = 0; i < columnName.length(); i++) {
			char c = columnName.charAt(i);

			if (c >= 'A' && c <= 'Z') {
				result = result * 26 + (c - 'A' + 1);
			} else {
				throw new IllegalArgumentException("Invalid column name: " + columnName);
			}
		}

		return result;
	}

	/**
	 * Gets the line number for the specified position in <code>compilationUnit</oced>
	 *
	 * @param compilationUnit
	 * @param position
	 * @return
	 */
	public static int getLineNumber(final CompilationUnit compilationUnit, final int position) {
		int lineNumber = compilationUnit.getLineNumber(position);

		if (lineNumber != -1) {
			return lineNumber;
		}

		// Handle case where node ends at end of file
		// (getLineNumber returns -1, since outside of file)
		// In this case, get the line number of the last character
		lineNumber = compilationUnit.getLineNumber(position - 1);

		return lineNumber;
	}

	/**
	 *
	 *
	 * @param text
	 * @return
	 */
	public static String changeTabsToSpaces(final String text) {
		return changeTabsToSpaces(text, 4);
	}

	/**
	 *
	 *
	 * @param text
	 * @return
	 */
	public static String changeTabsToSpaces(final String text, final int spaces) {
		if (!text.contains("\t")) {
			return text;
		}

		// Replace tabs with spaces (up to 4 spaces for each tab)
		// TODO: should reset after line terminator?
		// (if so, introduce int cursor to track where on line)
		StringBuilder textResult = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (c == '\t') {
				// Replace tabs with spaces (up to 4 spaces for each tab)
				textResult.append(repeat(" ", spaces - textResult.length() % spaces));
			} else {
				textResult.append(c);
			}
		}

		return textResult.toString();
	}

	private static String repeat(final String text, final int count) {
		if (count == 0) {
			return "";
		} else if (count == 1) {
			return text;
		}

		// TODO: change to use binary value, so sublinear performance
		// (doesn't matter for our needs)
		StringBuilder result = new StringBuilder(text.length() * count);

		for (int i = 1; i <= count; i++) {
			result.append(text);
		}

		return result.toString();
	}

	// Suggestion for a utility method, once we go to Java 8
	// (makes it easy to negate a predicate; why this isn't in core Java is anyone's guess)
	// Added in Java 11 to Predicate class
	public static <T> Predicate<T> not(final Predicate<T> predicate) {
		return predicate.negate();
	}

	public static ASTParser getParser(final String workspace, final String jrePathname) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setBindingsRecovery(true);
		// TODO: not sure if needed, but setting
		parser.setStatementsRecovery(true);

		Map<String, String> options = JavaCore.getOptions();

		// Required to correctly read enums
		// http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FASTParser.html
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);

		parser.setCompilerOptions(options);

		// parser.setUnitName(unitName);

		// http://www.programcreek.com/2014/01/how-to-resolve-bindings-when-using-eclipse-jdt-astparser/
		// Doesn't seem like this is needed (classpath is set though)
		String[] sources = { "" };

		String[] classpath = getClasspath(workspace, jrePathname, true).toArray(new String[0]);

		// Previous reference said last parameter should be false, but the above link says to use true
		// (not sure the difference)
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, false);

		return parser;
	}

	/**
	 *
	 *
	 * @param workspace
	 * @param jrePathname JRE pathname (not needed is not including jars)
	 * @param includeJars
	 * @return
	 */
	public static List<String> getClasspath(final String workspace, final String jrePathname,
			final boolean includeJars) {
		List<String> classpath = new ArrayList<>();

		if (!new File(workspace).isDirectory()) {
			throw new IllegalArgumentException("Workspace is not a valid directory: " + workspace);
		}

		// TODO: add checks to confirm the workspace directory is valid
		// (such as ensuring a specific project is there)
		//		if (!new File(workspace, "\\MyProject\\Folder").isDirectory()) {
		//			throw new IllegalArgumentException("Workspace is not a valid workspace: " + workspace);
		//		}

		if (includeJars) {
			// If this is a JDK, get jre subfolder
			String useJrePathname = jrePathname;

			File jreFileTest = new File(jrePathname, "jre");
			if (jreFileTest.isDirectory()) {
				useJrePathname = jreFileTest.getPath();
			}

			Path jrePath = Paths.get(useJrePathname, "lib\\rt.jar");

			if (!Files.exists(jrePath)) {
				throw new IllegalArgumentException("JRE pathname is not a valid JRE directory: " + jrePathname);
			}

			classpath.add(jrePath.toString());

			// Eclipse parser doesn't seem to like wildcard on classpath

			// TODO: add any workspace / project specific JARs
		}

		// Seems to work like this (add each location to classpath)
		// Allows resolving bindings
		// Must list source folder, otherwise does not work
		// (presumably related to folder structure used for package name)
		// TODO: add your project specific source
		//		classpath.add(workspace + "\\MyProject\\src");

		// 2/27/2020 Remove entries that don't exist (prevent InvalidStateException)
		classpath.removeIf(c -> !new File(c).exists());

		return classpath;
	}

	public static String joining(final Collection<?> collection, final String delimiter) {
		return joining(collection.stream(), delimiter);
	}

	public static String joining(final Stream<?> stream, final String delimiter) {
		return stream.map(Object::toString).collect(Collectors.joining(delimiter));
	}

	public static String getAnnotationsAndModifiers(final IBinding binding, final String... ignoreThese) {
		StringBuilder modifiers = new StringBuilder();

		String annotations = joining(Stream.of(binding.getAnnotations())
				.filter(a -> !Utilities.in(a.getAnnotationType().getQualifiedName(), (Object[]) ignoreThese)), " ");

		modifiers.append(annotations);

		if (modifiers.length() != 0) {
			modifiers.append(' ');
		}

		modifiers.append(Flags.toString(binding.getModifiers()));

		return modifiers.toString();
	}
}