package info.codesaway.becr.parsing;

import java.util.EnumSet;
import java.util.Objects;

import org.eclipse.jdt.core.dom.IVariableBinding;

public final class FieldInfo implements CodeInfo {
	private final EnumSet<FieldInfoOption> optionsSet;

	private final String modifiers;
	private final String className;
	private final String shortClassName;
	private final String type;
	private final String name;

	private final CodeType codeType;

	private final int startLine;
	private final String lineInfo;

	private final boolean isDeprecated;

	private final boolean shouldCheck;

	public FieldInfo(final IVariableBinding variableBinding, final FieldInfoOption... options) {
		this(variableBinding, 0, true, options);
	}

	public FieldInfo(final IVariableBinding variableBinding, final int startLine, final boolean shouldCheck,
			final FieldInfoOption... options) {
		this(variableBinding, startLine, "", shouldCheck, options);
	}

	/**
	 *
	 * @param variableBinding
	 * @param startLine the start line (only need for declarations, used when inserting Deprecated annotation)
	 */
	public FieldInfo(final IVariableBinding variableBinding, final int startLine, final String lineInfo,
			final boolean shouldCheck, final FieldInfoOption... options) {
		// Get the class (without parameterized type)
		this.className = variableBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
		this.shortClassName = variableBinding.getDeclaringClass().getTypeDeclaration().getName();

		// https://stackoverflow.com/a/22886481
		if (options.length != 0) {
			this.optionsSet = EnumSet.of(options[0], options);
		} else {
			this.optionsSet = EnumSet.noneOf(FieldInfoOption.class);
		}

		this.name = variableBinding.getName();

		if (this.optionsSet.contains(FieldInfoOption.USE_SHORT_NAME)) {
			this.type = variableBinding.getType().getName();
		} else {
			this.type = variableBinding.getType().getQualifiedName();
		}

		this.modifiers = ParsingUtilities.getAnnotationsAndModifiers(variableBinding, "java.lang.Deprecated");

		this.codeType = CodeType.FIELD;

		this.startLine = startLine;
		this.lineInfo = lineInfo;

		this.isDeprecated = variableBinding.isDeprecated();

		this.shouldCheck = shouldCheck;
	}

	@Override
	public String getModifiers() {
		return this.modifiers;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public String getShortClassName() {
		return this.shortClassName;
	}

	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
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
		FieldInfo other = (FieldInfo) obj;

		return Objects.equals(this.className, other.className)
				&& Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.name);
	}

	@Override
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	@Override
	public String toString() {
		return this.getQualifiedName() + (this.isDeprecated() ? " (deprecated)" : "");
	}

	@Override
	public boolean shouldCheck() {
		return this.shouldCheck;
	}

	public String getQualifiedName() {
		return this.getClassName() + "." + this.getName();
	}

	@Override
	public String getSignature() {
		return this.getName();
	}
}
