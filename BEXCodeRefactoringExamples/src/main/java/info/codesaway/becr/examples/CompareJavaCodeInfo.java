package info.codesaway.becr.examples;

import java.util.List;

import info.codesaway.becr.parsing.CodeInfoWithLineInfo;

public final class CompareJavaCodeInfo {
	private final String packageName;
	private final List<CodeInfoWithLineInfo> details;

	public CompareJavaCodeInfo(final String packageName, final List<CodeInfoWithLineInfo> details) {
		this.packageName = packageName;
		this.details = details;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public List<CodeInfoWithLineInfo> getDetails() {
		return this.details;
	}
}
