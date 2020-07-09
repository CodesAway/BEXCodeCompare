package info.codesaway.becr.diff;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXListPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffLine;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffUnit;

public class DifferencesResult {
	private final Path relativePath;
	private final BEXListPair<DiffLine> lines;
	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;
	private final List<DiffEdit> diff;
	private final List<DiffUnit> diffBlocks;

	public DifferencesResult(final Path relativePath, final BEXListPair<DiffLine> lines,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction, final List<DiffEdit> diff,
			final List<DiffUnit> diffBlocks) {
		this.relativePath = relativePath;
		this.lines = lines;
		this.normalizationFunction = normalizationFunction;

		this.diff = diff;
		this.diffBlocks = diffBlocks;
	}

	public Path getRelativePath() {
		return this.relativePath;
	}

	public List<DiffLine> getLeftLines() {
		return this.lines.getLeft();
	}

	public List<DiffLine> getRightLines() {
		return this.lines.getRight();
	}

	public BEXListPair<DiffLine> getLines() {
		return this.lines;
	}

	public List<DiffLine> getLines(final BEXSide side) {
		return this.lines.get(side);
	}

	public BiFunction<String, String, DiffNormalizedText> getNormalizationFunction() {
		return this.normalizationFunction;
	}

	public List<DiffEdit> getDiff() {
		return this.diff;
	}

	public List<DiffUnit> getDiffBlocks() {
		return this.diffBlocks;
	}
}
