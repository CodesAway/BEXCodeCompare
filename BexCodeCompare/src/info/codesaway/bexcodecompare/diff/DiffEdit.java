package info.codesaway.bexcodecompare.diff;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class DiffEdit implements DiffUnit {
	private final DiffType type;
	private final Optional<DiffLine> oldLine;
	private final Optional<DiffLine> newLine;

	/**
	 * Value to use with optional if there is no line (that is, it's absent)
	 *
	 * <p>This is used to help the readability of the code</p>
	 */
	static final DiffLine ABSENT_LINE = new DiffLine(-1, "");

	/**
	 *
	 * @param type
	 * @param oldLine old line (may be <code>null</code>)
	 * @param newLine new line (may be <code>null</code>
	 */
	public DiffEdit(final DiffType type, final DiffLine oldLine, final DiffLine newLine) {
		this(type, Optional.ofNullable(oldLine), Optional.ofNullable(newLine));
	}

	public DiffEdit(final DiffType type, final Optional<DiffLine> oldLine, final Optional<DiffLine> newLine) {
		Preconditions.checkArgument(oldLine.isPresent() || newLine.isPresent(),
				"Either old line or new line is required.");

		this.type = type;
		this.oldLine = oldLine;
		this.newLine = newLine;
	}

	@Override
	public DiffType getType() {
		return this.type;
	}

	public Optional<DiffLine> getOldLine() {
		return this.oldLine;
	}

	public Optional<DiffLine> getNewLine() {
		return this.newLine;
	}

	/**
	 * Indicates if has old line
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/17/2019	Initial code
	 *</pre>***********************************************************************************
	 */
	public boolean hasOldLine() {
		return this.getOldLine().isPresent();
	}

	/**
	 * Indicates if has new line
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/17/2019	Initial code
	 *</pre>***********************************************************************************
	 */
	public boolean hasNewLine() {
		return this.getNewLine().isPresent();
	}

	/**
	 * Gets the line number for the old line
	 *
	 * @return the line number of the old line (or -1 if there is no old line, such as for an inserted line)
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public int getOldLineNumber() {
		return this.getOldLine().orElse(ABSENT_LINE).getNumber();
	}

	/**
	 * Gets the line number for the new line
	 *
	 * @return the line number of the new line (or -1 if there is no new line, such as for a deleted line)
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public int getNewLineNumber() {
		return this.getNewLine().orElse(ABSENT_LINE).getNumber();
	}

	public String getText() {
		// Either old line or new line is required
		return this.getOldLine().orElse(this.getNewLine().orElse(ABSENT_LINE)).getText();
	}

	/**
	 * Gets the text for the old line
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Initial code
	 *</pre>***********************************************************************************
	 */
	public String getOldText() {
		return this.getOldLine().orElse(ABSENT_LINE).getText();
	}

	/**
	 * Gets the text for the new line
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/16/2019	Initial code
	 *</pre>***********************************************************************************
	 */
	public String getNewText() {
		return this.getNewLine().orElse(ABSENT_LINE).getText();
	}

	@Override
	public List<DiffEdit> getEdits() {
		return ImmutableList.of(this);
	}

	@Override
	public String toString() {
		return this.toString(false);
	}

	/**
	 *
	 *
	 * @param shouldHandleSubstitutionSpecial
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T					Amy Brennan-Luna	08/21/2019	Initial coding
	 * 															Added option to handle substitution special, to show both before and after on separate lines
	 *</pre>***********************************************************************************
	 */
	public String toString(final boolean shouldHandleSubstitutionSpecial) {
		char tag = this.getType().getTag();

		// Gets the line number if present or the empty string if absent
		String lineNumber1 = this.getOldLine().map(d -> String.valueOf(d.getNumber())).orElse("");
		String lineNumber2 = this.getNewLine().map(d -> String.valueOf(d.getNumber())).orElse("");

		if (shouldHandleSubstitutionSpecial && this.getType().isSubstitution()) {
			// Format with line numbers
			return String.format("%s%6s%6s    %s%n" + "%s%6s%6s    %s", tag, lineNumber1, "", this.getOldText(), tag,
					"", lineNumber2, this.getNewText());
		}

		// Format with line numbers
		return String.format("%s%6s%6s    %s", tag, lineNumber1, lineNumber2, this.getText());
	}
}
