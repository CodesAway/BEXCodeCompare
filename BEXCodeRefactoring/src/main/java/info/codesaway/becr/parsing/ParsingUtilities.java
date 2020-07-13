package info.codesaway.becr.parsing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;

import info.codesaway.bex.util.BEXUtilities;

public final class ParsingUtilities {
	static final String lineSeparator = System.getProperty("line.separator");

	/**
	 * Cannot instantiate utility class
	 *
	 * @throws UnsupportedOperationException
	 */
	private ParsingUtilities() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets the line number for the specified position in <code>compilationUnit</code>
	 *
	 * @param compilationUnit
	 * @param position
	 * @return
	 */
	public static int getLineNumber(final CompilationUnit compilationUnit, final int position) {
		int lineNumber = compilationUnit.getLineNumber(position);

		if (lineNumber != -1) {
			return lineNumber;
		}

		// Handle case where node ends at end of file
		// (getLineNumber returns -1, since outside of file)
		// In this case, get the line number of the last character
		lineNumber = compilationUnit.getLineNumber(position - 1);

		return lineNumber;
	}

	/**
	 *
	 *
	 * @param text
	 * @return
	 */
	public static String changeTabsToSpaces(final String text) {
		return changeTabsToSpaces(text, 4);
	}

	/**
	 *
	 *
	 * @param text
	 * @return
	 */
	public static String changeTabsToSpaces(final String text, final int spaces) {
		if (!text.contains("\t")) {
			return text;
		}

		// Replace tabs with spaces (up to 4 spaces for each tab)
		// TODO: should reset after line terminator?
		// (if so, introduce int cursor to track where on line)
		StringBuilder textResult = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (c == '\t') {
				// Replace tabs with spaces (up to 4 spaces for each tab)
				textResult.append(repeat(" ", spaces - textResult.length() % spaces));
			} else {
				textResult.append(c);
			}
		}

		return textResult.toString();
	}

	private static String repeat(final String text, final int count) {
		if (count == 0) {
			return "";
		} else if (count == 1) {
			return text;
		}

		// TODO: change to use binary value, so sublinear performance
		// (doesn't matter for our needs)
		StringBuilder result = new StringBuilder(text.length() * count);

		for (int i = 1; i <= count; i++) {
			result.append(text);
		}

		return result.toString();
	}

	// Suggestion for a utility method, once we go to Java 8
	// (makes it easy to negate a predicate; why this isn't in core Java is anyone's guess)
	// Added in Java 11 to Predicate class
	public static <T> Predicate<T> not(final Predicate<T> predicate) {
		return predicate.negate();
	}

	public static ASTParser getParser(final String workspace, final String jrePathname) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setBindingsRecovery(true);
		// TODO: not sure if needed, but setting
		parser.setStatementsRecovery(true);

		Map<String, String> options = JavaCore.getOptions();

		// Required to correctly read enums
		// http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FASTParser.html
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);

		parser.setCompilerOptions(options);

		// parser.setUnitName(unitName);

		// http://www.programcreek.com/2014/01/how-to-resolve-bindings-when-using-eclipse-jdt-astparser/
		// Doesn't seem like this is needed (classpath is set though)
		String[] sources = { "" };

		String[] classpath = getClasspath(workspace, jrePathname, true).toArray(new String[0]);

		// Previous reference said last parameter should be false, but the above link says to use true
		// (not sure the difference)
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, false);

		return parser;
	}

	/**
	 *
	 *
	 * @param workspace
	 * @param jrePathname JRE pathname (not needed is not including jars)
	 * @param includeJars
	 * @return
	 */
	public static List<String> getClasspath(final String workspace, final String jrePathname,
			final boolean includeJars) {
		List<String> classpath = new ArrayList<>();

		if (!Files.isDirectory(Paths.get(workspace))) {
			throw new IllegalArgumentException("Workspace is not a valid directory: " + workspace);
		}

		// TODO: add checks to confirm the workspace directory is valid
		// (such as ensuring a specific project is there)
		//		if (!new File(workspace, "\\MyProject\\Folder").isDirectory()) {
		//			throw new IllegalArgumentException("Workspace is not a valid workspace: " + workspace);
		//		}

		if (includeJars) {
			// If this is a JDK, get jre subfolder
			String useJrePathname = jrePathname;

			Path jrePathTest = Paths.get(jrePathname, "jre");
			if (Files.isDirectory(jrePathTest)) {
				useJrePathname = jrePathTest.toString();
			}

			Path jrePath = Paths.get(useJrePathname, "lib\\rt.jar");

			if (!Files.exists(jrePath)) {
				throw new IllegalArgumentException("JRE pathname is not a valid JRE directory: " + jrePathname);
			}

			classpath.add(jrePath.toString());

			// Eclipse parser doesn't seem to like wildcard on classpath

			// TODO: add any workspace / project specific JARs
		}

		// Seems to work like this (add each location to classpath)
		// Allows resolving bindings
		// Must list source folder, otherwise does not work
		// (presumably related to folder structure used for package name)
		// TODO: add your project specific source
		//		classpath.add(workspace + "\\MyProject\\src");

		// 2/27/2020 Remove entries that don't exist (prevent InvalidStateException)
		classpath.removeIf(c -> Files.notExists(Paths.get(c)));

		return classpath;
	}

	public static String joining(final Collection<?> collection, final String delimiter) {
		return joining(collection.stream(), delimiter);
	}

	public static String joining(final Stream<?> stream, final String delimiter) {
		return stream.map(Object::toString).collect(Collectors.joining(delimiter));
	}

	public static String getAnnotationsAndModifiers(final IBinding binding, final String... ignoreThese) {
		StringBuilder modifiers = new StringBuilder();

		String annotations = joining(Stream.of(binding.getAnnotations())
				.filter(a -> !BEXUtilities.in(a.getAnnotationType().getQualifiedName(), (Object[]) ignoreThese)), " ");

		modifiers.append(annotations);

		if (modifiers.length() != 0) {
			modifiers.append(' ');
		}

		modifiers.append(Flags.toString(binding.getModifiers()));

		return modifiers.toString();
	}
}