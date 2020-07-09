package info.codesaway.becr.parsing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class MethodSignature implements Comparable<MethodSignature>, CodeInfo {
	private final EnumSet<MethodSignatureOption> optionsSet;

	// Store classname where method lives
	// (used if have binding info (otherwise not used)
	private final String className;
	private final String shortClassName;
	private final String returnValue;
	private final String name;
	private final int parameterCount;
	/**
	 * Signature, using fully qualified names
	 */
	private final String fullSignature;

	/**
	 * Signature, using fully qualified names and type erasure
	 */
	private final String fullSignatureWithTypeErasure;

	/**
	 * Signature, using short names
	 */
	private final String shortSignature;

	private final String modifiers;

	private final int startLine;
	private final String lineInfo;

	private final boolean isDeprecated;

	private final CodeType codeType;

	private final boolean shouldCheck;

	public MethodSignature(final MethodDeclaration node) {
		this(node, "");
	}

	public MethodSignature(final MethodDeclaration node, final String lineInfo) {
		this.optionsSet = EnumSet.noneOf(MethodSignatureOption.class);
		this.className = "";

		this.name = node.getName().toString();

		if (node.isConstructor()) {
			// Use empty string for constructor return value
			// (makes obvious that it's a constructor)
			this.returnValue = "";
			this.codeType = CodeType.CONSTRUCTOR;
		} else if (node.getReturnType2() != null) {
			this.returnValue = node.getReturnType2().toString();
			this.codeType = CodeType.METHOD;
		} else {
			this.returnValue = "";
			this.codeType = CodeType.UNKNOWN;
		}

		// else
		// {
		// throw new AssertionError("Return type is null for " + this.name);
		// }

		StringBuilder methodSignature = new StringBuilder();

		methodSignature
				.append(this.name)
				.append('(');

		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = node.parameters();
		this.parameterCount = parameters.size();

		// TODO: should this be used??
		// List<TypeParameter> typeParameters = node.typeParameters();

		for (SingleVariableDeclaration parameter : parameters) {
			String type = parameter.getType().toString();
			// TODO: This is only to handle simple cases
			// (use accept method to handle complex types, such as collections of Clarity types)

			// String fullyQualifiedType = this.importMapping.get(type);
			//
			// if (fullyQualifiedType != null)
			// {
			// type = fullyQualifiedType;
			// }

			methodSignature
					.append(type)
					.append(", ");
		}

		// Remove final ", "
		if (!parameters.isEmpty()) {
			methodSignature.delete(methodSignature.length() - ", ".length(), methodSignature.length());
		}

		methodSignature.append(')');

		this.fullSignature = methodSignature.toString();
		this.shortSignature = methodSignature.toString();
		this.fullSignatureWithTypeErasure = methodSignature.toString();
		this.shortClassName = this.className;

		this.modifiers = ParsingUtilities.joining(node.modifiers(), " ");

		// TODO: Can read node to get info, but need compilation unit to get line numbers
		this.startLine = 0;
		this.lineInfo = lineInfo;
		this.shouldCheck = true;

		// TODO: can check modifiers to check if deprecated
		this.isDeprecated = false;

	}

	public MethodSignature(final IMethodBinding methodBinding, final MethodSignatureOption... options) {
		this(methodBinding, "", options);
	}

	public MethodSignature(final IMethodBinding methodBinding, final String lineInfo,
			final MethodSignatureOption... options) {
		this(methodBinding, 0, lineInfo, true, options);
	}

	public MethodSignature(final IMethodBinding methodBinding, final int startLine, final boolean shouldCheck,
			final MethodSignatureOption... options) {
		this(methodBinding, startLine, "", shouldCheck, options);
	}

	public MethodSignature(final IMethodBinding methodBinding, final int startLine, final String lineInfo,
			final boolean shouldCheck, final MethodSignatureOption... options) {
		// Get the class (without parameterized type)
		this.className = methodBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
		this.shortClassName = methodBinding.getDeclaringClass().getTypeDeclaration().getName();

		if (options.length != 0) {
			this.optionsSet = EnumSet.copyOf(Arrays.asList(options));
		} else {
			this.optionsSet = EnumSet.noneOf(MethodSignatureOption.class);
		}

		this.name = methodBinding.getName();

		if (methodBinding.getReturnType() != null) {
			if (methodBinding.isConstructor()) {
				// Use empty string for constructor return value
				// (makes obvious that it's a constructor)
				this.returnValue = "";
				this.codeType = CodeType.CONSTRUCTOR;
			} else {
				if (this.optionsSet.contains(MethodSignatureOption.USE_SHORT_NAME)) {
					this.returnValue = methodBinding.getReturnType().getName();
				} else {
					this.returnValue = methodBinding.getReturnType().getQualifiedName();
				}

				this.codeType = CodeType.METHOD;
			}
		} else if (methodBinding.isConstructor()) {
			// TODO: what to use if constructor?
			// (no return type, so leave blank?)
			this.returnValue = "";
			this.codeType = CodeType.CONSTRUCTOR;
		} else {
			this.returnValue = "";
			this.codeType = CodeType.UNKNOWN;
		}

		String methodSignaturePrefix = this.name + "(";

		StringJoiner shortMethodSignature = new StringJoiner(", ", methodSignaturePrefix, ")");
		StringJoiner fullMethodSignature = new StringJoiner(", ", methodSignaturePrefix, ")");
		StringJoiner fullMethodSignatureWithTypeErasure = new StringJoiner(", ", methodSignaturePrefix, ")");

		ITypeBinding[] parameters = methodBinding.getParameterTypes();
		this.parameterCount = parameters.length;

		for (ITypeBinding parameter : parameters) {
			//            String type;
			//            if (optionsSet.contains(MethodSignatureOption.USE_SHORT_NAME)) {
			//                type = parameter.getName();
			//            } else {
			//                type = parameter.getQualifiedName();
			//            }
			//

			if (this.optionsSet.contains(MethodSignatureOption.GET_ERASURE)) {
				parameter = parameter.getErasure();
			}

			String shortType = parameter.getName();
			String fullType = parameter.getQualifiedName();
			String fullTypeWithTypeErasure = parameter.getErasure().getQualifiedName();

			//            String type = parameter.getName();
			// TODO: This is only to handle simple cases
			// (use accept method to handle complex types, such as collections of Clarity types)

			// String fullyQualifiedType = this.importMapping.get(type);
			//
			// if (fullyQualifiedType != null)
			// {
			// type = fullyQualifiedType;
			// }

			shortMethodSignature.add(shortType);
			fullMethodSignature.add(fullType);
			fullMethodSignatureWithTypeErasure.add(fullTypeWithTypeErasure);
		}

		this.shortSignature = shortMethodSignature.toString();
		this.fullSignature = fullMethodSignature.toString();
		this.fullSignatureWithTypeErasure = fullMethodSignatureWithTypeErasure.toString();

		this.modifiers = ParsingUtilities.getAnnotationsAndModifiers(methodBinding, "java.lang.Deprecated");

		this.startLine = startLine;
		this.lineInfo = lineInfo;

		this.isDeprecated = methodBinding.isDeprecated();

		this.shouldCheck = shouldCheck;
	}

	public String getName() {
		return this.name;
	}

	public int getParameterCount() {
		return this.parameterCount;
	}

	/**
	 * Indicates whether to use short names for method signature
	 *
	 * @return
	 */
	// TODO: could add method to allow to change this value
	// (by removing / adding the specified option
	private boolean useShortName() {
		return this.optionsSet.contains(MethodSignatureOption.USE_SHORT_NAME);
	}

	/**
	 * Get the full signature with type erasure (used to check if two methods are the same)
	 *
	 * @return
	 */
	public String getSignatureWithTypeErasure() {
		return this.fullSignatureWithTypeErasure;
	}

	@Override
	public String getSignature() {
		return this.getSignatureShortOrFull(this.useShortName());
	}

	public String getSignatureShortOrFull(final boolean useShortName) {
		return useShortName ? this.getShortSignature() : this.getFullSignature();
	}

	public String getSignatureWithClass() {
		return this.getSignatureWithClass(this.useShortName());
	}

	public String getSignatureWithClass(final boolean useShortName) {
		if (this.getClassName().isEmpty()) {
			return this.getSignatureShortOrFull(useShortName);
		}

		return this.getClassName() + "." + this.getSignatureShortOrFull(useShortName);
	}

	public String getFullSignature() {
		return this.fullSignature;
	}

	public String getFullSignatureWithClass() {
		return this.getSignatureWithClass(false);
	}

	public String getShortSignature() {
		return this.shortSignature;
	}

	public String getShortSignatureWithClass() {
		return this.getSignatureWithClass(true);
	}

	public String getReturnValue() {
		return this.returnValue;
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
	public String toString() {
		return this.getSignatureWithClass();
	}

	@Override
	public int compareTo(final MethodSignature o) {
		return Comparator.comparing(MethodSignature::getSignatureWithTypeErasure)
				.thenComparing(MethodSignature::getClassName)
				.compare(this, o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getSignatureWithTypeErasure(), this.getClassName());
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
		MethodSignature other = (MethodSignature) obj;

		return Objects.equals(this.getSignatureWithTypeErasure(), other.getSignatureWithTypeErasure())
				&& Objects.equals(this.getClassName(), other.getClassName());
	}

	@Override
	public CodeType getCodeType() {
		return this.codeType;
	}

	@Override
	public boolean shouldCheck() {
		return this.shouldCheck;
	}
}
