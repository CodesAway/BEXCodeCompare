package info.codesaway.bex.diff.substitution.java;

public class JavaRefactorings {
	// Java Refactoring (don't put in JavaRefactoring class since SpotBugs complained about cyclic static initializers (IC_INIT_CIRCULARITY)
	public static final JavaUnboxing JAVA_UNBOXING = new JavaUnboxing();
	public static final ImportSameClassnameDifferentPackage IMPORT_SAME_CLASSNAME_DIFFERENT_PACKAGE = new ImportSameClassnameDifferentPackage();
	public static final JavaCastSubstitution JAVA_CAST = new JavaCastSubstitution();
	public static final JavaFinalKeywordSubstitution JAVA_FINAL_KEYWORD = new JavaFinalKeywordSubstitution();
	public static final JavaDiamondOperatorSubstitution JAVA_DIAMOND_OPERATOR = new JavaDiamondOperatorSubstitution();
	public static final JavaSemicolonSubstitution JAVA_SEMICOLON = new JavaSemicolonSubstitution();

	// Put static instances of Java Refactorings in a separate class
	// Got SpotBugs error if put in Java Refactoring
	// Initialization circularity between info.codesaway.bex.diff.substitution.java.JavaRefactoring and info.codesaway.bex.diff.substitution.java.RefactorUnboxing
	private JavaRefactorings() {
		throw new UnsupportedOperationException();
	}
}
