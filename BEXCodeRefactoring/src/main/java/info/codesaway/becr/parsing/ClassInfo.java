package info.codesaway.becr.parsing;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Used for classes, interfaces, enums, and annotations
 */
public class ClassInfo implements CodeInfo {
	private final String qualifiedName;
	private final String shortName;

	private final CodeType codeType;

	private final int startLine;
	private final String lineInfo;

	private final boolean isDeprecated;

	private final String modifiers;

	private final boolean shouldCheck;

	/**
	 *
	 * @param typeBinding the type binding
	 */
	public ClassInfo(final ITypeBinding typeBinding) {
		this(typeBinding, 0, true);
	}

	/**
	 *
	 * @param typeBinding the type binding
	 * @param startLine the start line
	 * @param shouldCheck indicates whether this object should be checked (up to client to determine how to use)
	 */
	public ClassInfo(final ITypeBinding typeBinding, final int startLine,
			final boolean shouldCheck) {
		this(typeBinding, startLine, "", shouldCheck);
	}

	/**
	 *
	 * @param typeBinding the type binding
	 * @param startLine the start line
	 * @param lineInfo the line info
	 * @param shouldCheck indicates whether this object should be checked (up to the client to determine how to use)
	 */
	public ClassInfo(final ITypeBinding typeBinding, final int startLine, final String lineInfo,
			final boolean shouldCheck) {
		this.qualifiedName = typeBinding.getQualifiedName();
		this.shortName = typeBinding.getName();

		if (typeBinding.isEnum()) {
			this.codeType = CodeType.ENUM;
		} else if (typeBinding.isAnnotation()) {
			this.codeType = CodeType.ANNOTATION;
		} else if (typeBinding.isInterface()) {
			this.codeType = CodeType.INTERFACE;
		} else if (typeBinding.isClass()) {
			this.codeType = CodeType.CLASS;
		} else {
			this.codeType = CodeType.UNKNOWN;
		}

		this.startLine = startLine;
		this.lineInfo = lineInfo;

		this.isDeprecated = typeBinding.isDeprecated();

		this.modifiers = ParsingUtilities.getAnnotationsAndModifiers(typeBinding, "java.lang.Deprecated");

		this.shouldCheck = shouldCheck;
	}

	/**
	 * Gets the class' qualified name
	 * @return the class' qualified name
	 */
	public String getQualifiedName() {
		return this.qualifiedName;
	}

	@Override
	public CodeType getCodeType() {
		return this.codeType;
	}

	@Override
	public int getStartLine() {
		return this.startLine;
	}

	@Override
	public String getLineInfo() {
		return this.lineInfo;
	}

	@Override
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	@Override
	public String getModifiers() {
		return this.modifiers;
	}

	@Override
	public String getClassName() {
		return this.qualifiedName;
	}

	@Override
	public String getShortClassName() {
		return this.shortName;
	}

	/**
	 * Always returns blank (since there is no component other than the class name)
	 *
	 * @return always blank
	 */
	@Override
	public String getSignature() {
		// TODO: why does it return blank (need to document)
		// Returns blank since
		return "";
	}

	@Override
	public boolean shouldCheck() {
		return this.shouldCheck;
	}
}
