package info.codesaway.becr.comparedirectories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import info.codesaway.becr.parsing.ProjectPath;
import info.codesaway.bex.BEXListPair;

public final class CompareDirectoriesResult {
	private final CompareDirectoriesDifferencesResult compareDirectoriesDifferencesResult;
	private final BEXListPair<ProjectPath> javaPaths;
	private final Map<ProjectPath, CompareJavaCodeInfo> javaParseResults;
	private final Map<ProjectPath, List<CompareDirectoriesJoinedDetail>> javaChanges;

	public CompareDirectoriesResult(final CompareDirectoriesDifferencesResult compareDirectoriesDifferencesResult) {
		this(compareDirectoriesDifferencesResult, new BEXListPair<>(ArrayList::new), Collections.emptyMap(),
				Collections.emptyMap());
	}

	public CompareDirectoriesResult(final CompareDirectoriesDifferencesResult compareDirectoriesDifferencesResult,
			final BEXListPair<ProjectPath> javaPaths, final Map<ProjectPath, CompareJavaCodeInfo> javaParseResults,
			final Map<ProjectPath, List<CompareDirectoriesJoinedDetail>> javaChanges) {
		this.compareDirectoriesDifferencesResult = compareDirectoriesDifferencesResult;
		this.javaPaths = javaPaths;
		this.javaParseResults = javaParseResults;
		this.javaChanges = javaChanges;
	}

	public CompareDirectoriesDifferencesResult getCompareDirectoriesDifferencesResult() {
		return this.compareDirectoriesDifferencesResult;
	}

	public BEXListPair<ProjectPath> getJavaPaths() {
		return this.javaPaths;
	}

	public Map<ProjectPath, CompareJavaCodeInfo> getJavaParseResults() {
		return this.javaParseResults;
	}

	public Map<ProjectPath, List<CompareDirectoriesJoinedDetail>> getJavaChanges() {
		return this.javaChanges;
	}
}