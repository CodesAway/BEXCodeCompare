package info.codesaway.becr.parsing;

import java.util.Objects;

public class CodeInfoWithLineInfo {
	private final CodeInfo codeInfo;

	private final int extendedStartLine;
	private final int startLine;
	private final int endLine;

	/**
	 *
	 * @param codeInfo
	 * @param extendedStartLine
	 * @param startLine the start line (inclusive)
	 * @param endLine the end line (inclusive)
	 */
	public CodeInfoWithLineInfo(final CodeInfo codeInfo, final int extendedStartLine, final int startLine,
			final int endLine) {
		this.codeInfo = codeInfo;
		this.extendedStartLine = extendedStartLine;
		this.startLine = startLine;
		this.endLine = endLine;
	}

	public CodeInfo getCodeInfo() {
		return this.codeInfo;
	}

	public int getExtendedStartLine() {
		return this.extendedStartLine;
	}

	public int getStartLine() {
		return this.startLine;
	}

	public int getEndLine() {
		return this.endLine;
	}

	public boolean contains(final int line) {
		return line >= this.getStartLine() && line <= this.getEndLine();
	}

	public int getLineCount() {
		// Add 1 since inclusive on start and end
		return this.getEndLine() - this.getStartLine() + 1;
	}

	@Override
	public String toString() {
		if (this.getExtendedStartLine() == this.getStartLine()) {
			return String.format("%s: %d-%d", this.codeInfo, this.startLine, this.endLine);
		}

		return String.format("%s: %d/%d-%d", this.codeInfo, this.extendedStartLine, this.startLine, this.endLine);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.codeInfo == null) ? 0 : this.codeInfo.hashCode());
		result = prime * result + this.endLine;
		result = prime * result + this.extendedStartLine;
		result = prime * result + this.startLine;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		CodeInfoWithLineInfo other = (CodeInfoWithLineInfo) obj;
		return Objects.equals(this.codeInfo, other.codeInfo) && this.endLine == other.endLine
				&& this.extendedStartLine == other.extendedStartLine && this.startLine == other.startLine;
	}

}
