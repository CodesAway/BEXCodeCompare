package info.codesaway.becr.parsing;

import java.util.Comparator;
import java.util.Objects;

import info.codesaway.util.regex.Pattern;

public final class CodeInfoWithSourceInfo implements Comparable<CodeInfoWithSourceInfo>, CodeInfo {

	private final String project;
	private final String packageName;
	private final String javaFilenameWithoutExtension;

	private final CodeInfo codeInfo;

	private final String info;

	private final String sourcePathname;

	public CodeInfoWithSourceInfo(final String project, final String packageName,
			final String javaFilenameWithoutExtension,
			final CodeInfo codeInfo, final String info, final String sourcePathname) {
		this.project = project;
		this.packageName = packageName;
		this.javaFilenameWithoutExtension = javaFilenameWithoutExtension;

		this.codeInfo = codeInfo;

		this.info = info;

		this.sourcePathname = sourcePathname;
	}

	public String getProject() {
		return this.project;
	}

	public String getPackageName() {
		return this.packageName;
	}

	// Changed on 2/27/2020 to return fully qualified name
	// (necessary since CodeInfo interface expects this method to return the fully qualified name)
	// (don't want to break my own API)
	@Override
	public String getClassName() {
		if (this.codeInfo != null) {
			return this.codeInfo.getClassName();
		}

		return this.packageName + "." + this.javaFilenameWithoutExtension;
	}

	public String getJavaFilenameWithoutExtension() {
		return this.javaFilenameWithoutExtension;
	}

	public String getQualifiedClassName() {
		return this.packageName + "." + this.javaFilenameWithoutExtension;
	}

	public MethodSignature getMethodSignature() {
		if (this.isMethod()) {
			return (MethodSignature) this.codeInfo;
		} else {
			return null;
		}
	}

	public FieldInfo getFieldInfo() {
		if (this.isField()) {
			return (FieldInfo) this.codeInfo;
		} else {
			return null;
		}
	}

	public ClassInfo getClassInfo() {
		if (this.isClass()) {
			return (ClassInfo) this.codeInfo;
		} else {
			return null;
		}
	}

	/**
	 * Indicates if this represents a class, interface, enum, or annotation
	 * @return
	 */
	public boolean isClass() {
		return this.codeInfo instanceof ClassInfo;
	}

	public boolean isMethod() {
		return this.codeInfo instanceof MethodSignature;
	}

	public boolean isField() {
		return this.codeInfo instanceof FieldInfo;
	}

	public String getInfo() {
		return this.info;
	}

	@Override
	public String getLineInfo() {
		if (this.codeInfo != null) {
			return this.codeInfo.getLineInfo();
		} else {
			return "";
		}
	}

	@Override
	public CodeType getCodeType() {
		if (this.codeInfo != null) {
			return this.codeInfo.getCodeType();
		} else {
			return CodeType.UNKNOWN;
		}
	}

	@Override
	public String getModifiers() {
		if (this.codeInfo != null) {
			return this.codeInfo.getModifiers();
		} else {
			return "";
		}
	}

	// Methods used for comparator

	@Override
	public String getSignature() {
		if (this.codeInfo != null) {
			return this.codeInfo.getSignature();
		} else {
			return "";
		}
	}

	@Override
	public String getShortClassName() {
		if (this.codeInfo != null) {
			return this.codeInfo.getShortClassName();
		} else {
			return "";
		}
	}

	private String getFieldName() {
		if (this.isField()) {
			return this.getFieldInfo().getName();
		} else {
			return "";
		}
	}

	public String getSourcePathname() {
		return this.sourcePathname;
	}

	@Override
	public int getStartLine() {
		if (this.codeInfo != null) {
			return this.codeInfo.getStartLine();
		} else {
			return 0;
		}
	}

	@Override
	public boolean isDeprecated() {
		if (this.codeInfo != null) {
			return this.codeInfo.isDeprecated();
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(final CodeInfoWithSourceInfo other) {
		return Comparator.comparing(CodeInfoWithSourceInfo::getProject)
				.thenComparing(CodeInfoWithSourceInfo::getPackageName)
				// Case insensitive order for classname (similar to Windows file structure)
				.thenComparing(CodeInfoWithSourceInfo::getJavaFilenameWithoutExtension, String.CASE_INSENSITIVE_ORDER)
				// Added just in case has two classes in the same folder with different casing
				.thenComparing(CodeInfoWithSourceInfo::getJavaFilenameWithoutExtension)
				.thenComparing(CodeInfoWithSourceInfo::getLineInfo, Pattern.getNaturalComparator())
				// Sort using the info (which has line information)
				.thenComparing(CodeInfoWithSourceInfo::getInfo, Pattern.getNaturalComparator())
				.thenComparing(CodeInfoWithSourceInfo::getSignature)
				.thenComparing(CodeInfoWithSourceInfo::getFieldName)
				// 7/8/2020 (as the final tie break, compare the source pathname)
				.thenComparing(CodeInfoWithSourceInfo::getSourcePathname)
				.compare(this, other);
	}

	@Override
	public String toString() {
		String text;
		if (this.isClass()) {
			text = this.getClassInfo().getQualifiedName();
		} else if (this.isMethod()) {
			text = this.getMethodSignature().getSignatureWithClass();
		} else if (this.isField()) {
			text = this.getFieldInfo().getQualifiedName();
		} else {
			text = super.toString();
		}

		return String.format("(%s) %s", this.getCodeType(), text);
	}

	@Override
	public boolean shouldCheck() {
		if (this.codeInfo != null) {
			return this.codeInfo.shouldCheck();
		} else {
			return true;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.codeInfo, this.info, this.javaFilenameWithoutExtension, this.packageName, this.project,
				this.sourcePathname);
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
		CodeInfoWithSourceInfo other = (CodeInfoWithSourceInfo) obj;
		return Objects.equals(this.codeInfo, other.codeInfo) && Objects.equals(this.info, other.info)
				&& Objects.equals(this.javaFilenameWithoutExtension, other.javaFilenameWithoutExtension)
				&& Objects.equals(this.packageName, other.packageName) && Objects.equals(this.project, other.project)
				&& Objects.equals(this.sourcePathname, other.sourcePathname);
	}
}
