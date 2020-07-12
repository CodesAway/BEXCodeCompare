package info.codesaway.becr.diff;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import info.codesaway.becr.parsing.ProjectPath;
import info.codesaway.bex.BEXListPair;

public class CompareDirectoriesResult {
	private final List<PathChangeInfo> pathChanges;
	private final BEXListPair<ProjectPath> javaPaths;
	private final Map<Path, DifferencesResult> javaPathDiffMap;

	public CompareDirectoriesResult(final List<PathChangeInfo> pathChanges, final BEXListPair<ProjectPath> javaPaths,
			final Map<Path, DifferencesResult> javaPathDiffMap) {
		this.pathChanges = pathChanges;
		this.javaPaths = javaPaths;
		this.javaPathDiffMap = javaPathDiffMap;
	}

	public List<PathChangeInfo> getPathChanges() {
		return this.pathChanges;
	}

	public BEXListPair<ProjectPath> getJavaPaths() {
		return this.javaPaths;
	}

	public Map<Path, DifferencesResult> getJavaPathDiffMap() {
		return this.javaPathDiffMap;
	}
}
