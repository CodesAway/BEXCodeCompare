package info.codesaway.becr.diff;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import info.codesaway.becr.parsing.CodeInfoWithLineInfo;
import info.codesaway.bex.BEXPair;
import info.codesaway.bex.diff.DiffType;

public final class CompareDirectoriesJoinedDetail {
	private final BEXPair<CodeInfoWithLineInfo> code;

	private ImpactType impact;

	private int extendedDifferenceCount;
	private int differenceCount;
	private int commentLinesCount;
	private int blankLinesCount;

	private final Map<DiffType, Integer> lineChanges = new LinkedHashMap<>();

	// Used for modified blocks
	private long modifiedDifferences;
	private long modifiedDeltas;

	private PathChangeType pathChangeType;

	/**
	 * Track notes, use Set so don't insert duplicate notes
	 */
	private final Set<String> notes = new LinkedHashSet<>();

	/**
	 * @param leftCode the "source" code info (may be null for added or unknown blocks)
	 * @param rightCode the "destination" code info (may be null for deleted or unknown blocks)
	 */
	public CompareDirectoriesJoinedDetail(final CodeInfoWithLineInfo leftCode, final CodeInfoWithLineInfo rightCode) {
		this(new BEXPair<>(leftCode, rightCode));
	}

	public CompareDirectoriesJoinedDetail(final BEXPair<CodeInfoWithLineInfo> code) {
		this.code = code;
	}

	public BEXPair<CodeInfoWithLineInfo> getCode() {
		return this.code;
	}

	public CodeInfoWithLineInfo getLeftCode() {
		return this.code.getLeft();
	}

	public CodeInfoWithLineInfo getRightCode() {
		return this.code.getRight();
	}

	public void addExtendedDifference() {
		this.extendedDifferenceCount++;
	}

	public void addDifference() {
		this.differenceCount++;
	}

	public void addDifference(final boolean isInExtendedLines) {
		if (isInExtendedLines) {
			this.addExtendedDifference();
		} else {
			this.addDifference();
		}
	}

	public int getExtendedDifferenceCount() {
		return this.extendedDifferenceCount;
	}

	public int getDifferenceCount() {
		return this.differenceCount;
	}

	public void addCommentLine() {
		this.commentLinesCount++;
	}

	public int getCommentLinesCount() {
		return this.commentLinesCount;
	}

	public void addBlankLine() {
		this.blankLinesCount++;
	}

	public int getBlankLinesCount() {
		return this.blankLinesCount;
	}

	public void resetCounts() {
		this.commentLinesCount = 0;
		this.differenceCount = 0;
		this.extendedDifferenceCount = 0;
	}

	public void addNote(final String note) {
		this.notes.add(note);
	}

	public Set<String> getNotes() {
		return this.notes;
	}

	public ImpactType getImpact() {
		return this.impact;
	}

	public boolean isImpactBlank() {
		return this.getImpact() == null;
	}

	public void setImpact(final ImpactType impact) {
		this.impact = impact;
	}

	@Override
	public String toString() {
		return String.format("%s (differences %d, %d)", this.code.toString("%s -> %s"),
				this.extendedDifferenceCount, this.differenceCount);
	}

	public long getModifiedDifferences() {
		return this.modifiedDifferences;
	}

	public void setModifiedDifferences(final long modifiedDifferences) {
		this.modifiedDifferences = modifiedDifferences;
	}

	public long getModifiedDeltas() {
		return this.modifiedDeltas;
	}

	public void setModifiedDeltas(final long modifiedDeltas) {
		this.modifiedDeltas = modifiedDeltas;
	}

	public void addLineChange(final DiffType lineDiffType) {
		if (lineDiffType != null) {
			Integer count = this.lineChanges.get(lineDiffType);
			Integer newCount = count == null ? 1 : count + 1;
			this.lineChanges.put(lineDiffType, newCount);
		}
	}

	/**
	 *
	 * @return a map from the DiffType to the number of times it occurs
	 */
	public Map<DiffType, Integer> getLineChanges() {
		return this.lineChanges;
	}

	public PathChangeType getPathChangeType() {
		return this.pathChangeType;
	}

	public void setPathChangeType(final PathChangeType pathChangeType) {
		this.pathChangeType = pathChangeType;
	}
}
