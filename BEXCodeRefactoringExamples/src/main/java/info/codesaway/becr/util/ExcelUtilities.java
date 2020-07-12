package info.codesaway.becr.util;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public final class ExcelUtilities {
	private ExcelUtilities() {
		throw new UnsupportedOperationException();
	}

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
		autosizeColumnsFromSheet(sheet, 0, lastColumn);
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
}
