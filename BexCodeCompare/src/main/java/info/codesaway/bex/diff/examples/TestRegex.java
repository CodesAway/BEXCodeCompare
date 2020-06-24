package info.codesaway.bex.diff.examples;

import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.Objects;

import info.codesaway.bex.diff.DiffType;
import info.codesaway.bex.diff.substitution.SubstitutionDiffTypeValue;
import info.codesaway.util.regex.MatchResult;
import info.codesaway.util.regex.Matcher;

public class TestRegex {
	private static final ThreadLocal<Matcher> IMPORT_MATCHER = getThreadLocalMatcher(
			"^\\s*+import(?<static>\\s++static)?+\\s++(?<package>(?:[A-Za-z0-9]++\\.)+)?(?<class>[A-Z][A-Za-z0-9]*+);");

	public static void main(final String[] args) {
		System.out.println(accept());
	}

	public static DiffType accept() {
		//		String normalizedLeft = normalizedTexts.get(left);
		//		String normalizedRight = normalizedTexts.get(right);

		String normalizedLeft = "import something.MyClass;";
		String normalizedRight = "import cool.MyClass;";

		Matcher importMatcher = IMPORT_MATCHER.get();

		importMatcher.reset(normalizedLeft).find();
		MatchResult leftMatchResult = importMatcher.toMatchResult();

		importMatcher.reset(normalizedRight).find();
		MatchResult rightMatchResult = importMatcher.toMatchResult();

		System.out.println(leftMatchResult.matched());
		System.out.println(rightMatchResult.matched());

		if (!leftMatchResult.matched() || !rightMatchResult.matched()) {
			return null;
		}

		if (leftMatchResult.matched("static") || rightMatchResult.matched("static")) {
			// For now, don't compare static imports
			return null;
		}

		String leftClass = leftMatchResult.get("class");
		String rightClass = rightMatchResult.get("class");

		if (!Objects.equals(leftClass, rightClass)) {
			return null;
		}

		String leftImport = leftMatchResult.get("package");
		String rightImport = rightMatchResult.get("package");

		// Remove trailing '.'
		if (leftImport.endsWith(".")) {
			leftImport = leftImport.substring(0, leftImport.length() - 1);
		}

		if (rightImport.endsWith(".")) {
			rightImport = rightImport.substring(0, rightImport.length() - 1);
		}

		// TODO: change to special ImportDiffType
		String diffName = String.format("import %s (%s -> %s)", rightClass, leftImport, rightImport);
		return new SubstitutionDiffTypeValue('S', diffName);
	}

}
