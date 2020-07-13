package info.codesaway.becr.parsing;

public interface CodeInfo {
	/**
	 * Gets the qualified class name
	 * @return the qualifier class name
	 */
	public String getClassName();

	/**
	 * Gets the short class name
	 * @return the short class name
	 */
	public String getShortClassName();

	/**
	 * Gets the signature (without the class name)
	 *
	 * @return the signature (without the class name)
	 */
	public String getSignature();

	/**
	 * Gets the start line
	 * @return the start line
	 */
	public int getStartLine();

	/**
	 * Gets the line info
	 * @return the line info
	 */
	public String getLineInfo();

	/**
	 * Indicates if the code is deprecated
	 * @return <code>true</code> if this CodeInfo is marked as deprecated in the code
	 */
	public boolean isDeprecated();

	/**
	 * Gets the CodeType
	 * @return the CodeType
	 */
	public CodeType getCodeType();

	/**
	 * Gets the modifiers
	 * @return the modifiers
	 */
	public String getModifiers();

	/**
	 * Indicates whether the code info should be checked
	 *
	 * <p><b>NOTE</b>: It is up to the client to decide how to use this; it acts as just an indicator
	 * For example, this can be used to allow storing all code info and then filtering based on this flag.</p>
	 *
	 * @return <code>true</code> if this CodeInfo should be checked
	 */
	public boolean shouldCheck();
}
