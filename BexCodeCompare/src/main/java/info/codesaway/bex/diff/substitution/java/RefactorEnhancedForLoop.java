package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.identityEquals;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.DiffSide;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.RefactoringType;
import info.codesaway.util.regex.Matcher;

public class RefactorEnhancedForLoop implements JavaSubstitution, RefactoringType {
	private static final ThreadLocal<Matcher> ENHANCED_FOR_LOOP_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			" for \\( "
					+ "(?<type>\\w++)\\s++(?<element>\\w++) "
					+ ": (?<iterable>(?:this\\.)?+\\w++) "
					+ "\\) \\{ "));

	private static final ThreadLocal<Matcher> INDEX_FOR_LOOP_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			" for \\( "
					+ "(?<type>int)\\s++(?<element>\\w++) = 0 ; "
					+ "\\k<element> < (?<iterable>(?:this\\.)?+\\w++)\\.(?:(?<collection>size)\\(\\)||(?<array>length)) ; "
					// 6/6/2020 Support both pre and post fix notation, like Eclipse
					+ "(?:\\k<element>\\+\\+|\\+\\+\\k<element>) "
					+ "\\) \\{ "));

	private final Map<String, State> states = new HashMap<>();
	private State lastState = null;

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher enhancedForLoopMatcher = ENHANCED_FOR_LOOP_MATCHER.get();
		Matcher indexedForLoopMatcher = INDEX_FOR_LOOP_MATCHER.get();

		DiffSide side;
		if (enhancedForLoopMatcher.reset(normalizedRight).matches()
				&& indexedForLoopMatcher.reset(normalizedLeft).matches()) {
			side = DiffSide.RIGHT;
		} else if (enhancedForLoopMatcher.reset(normalizedLeft).matches()
				&& indexedForLoopMatcher.reset(normalizedRight).matches()) {
			side = DiffSide.LEFT;
		} else {
			// Check if this is a substitution part of the for loop (changing)
			for (State state : this.states.values()) {
				//				System.out.println("Norm left: " + normalizedLeft);
				//				System.out.println("Norm right: " + normalizedRight);

				if (state.accept(normalizedLeft, normalizedRight)) {
					//					System.out.println("Refactoring?");
					//					System.out.println("Left: " + left);
					//					System.out.println("Right: " + right);
					return state.diffType;
				}
			}

			return null;
		}

		String iterableName = enhancedForLoopMatcher.get("iterable");

		// Verify iterable name matches
		if (iterableName.equals(indexedForLoopMatcher.get("iterable"))) {
			String elementType = enhancedForLoopMatcher.get("type");
			String elementName = enhancedForLoopMatcher.get("element");

			String indexName = indexedForLoopMatcher.get("element");
			IterableKind iterableKind = indexedForLoopMatcher.matched("array")
					? IterableKind.ARRAY
					: IterableKind.COLLECTION;

			State state = new State(side, elementType, elementName, iterableName, indexName, iterableKind);

			if (state.isValid()) {
				this.states.put(iterableName, state);
				this.lastState = state;
			}

			return state.diffType;
		}

		return null;
	}

	@Override
	public RefactoringDiffType acceptSingleSide(final DiffSide diffSide, final DiffEdit diffEdit,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		if (this.lastState == null) {
			return null;
		} else if (this.lastState.side == diffSide) {
			// We're expecting the deleted line to be on the indexed for loop side
			// (so the other side of the refactored enhanced side)
			return null;
		}

		String expectedText = DiffHelper.normalize(diffSide,
				this.lastState.elementType + " " + this.lastState.elementName + " = "
						+ this.lastState.searchText + ";",
				normalizationFunction);

		String actualText = normalizedTexts.get(diffEdit).trim();

		//		System.out.println(expectedText);
		//		System.out.println(actualText);

		if (actualText.equals(expectedText)) {
			return this.lastState.diffType;
		}

		return null;
	}

	private enum IterableKind {
		ARRAY, COLLECTION
	}

	// Store state information so can track the changes
	private static class State {
		private final DiffSide side;
		private final String elementType;
		private final String elementName;
		//		private final String iterableName;
		//		private final String indexName;
		//		private final IterableKind iterableKind;
		private final String searchText;
		private final String replacementText;
		private final RefactoringDiffType diffType;

		private State(final DiffSide side, final String elementType, final String elementName,
				final String iterableName, final String indexName, final IterableKind iterableKind) {
			this.side = side;
			this.elementType = elementType;
			this.elementName = elementName;
			//			this.iterableName = iterableName;
			//			this.indexName = indexName;
			//			this.iterableKind = iterableKind;

			// Text to search indexed
			if (iterableKind == IterableKind.COLLECTION) {
				this.searchText = iterableName + ".get(" + indexName + ")";
			} else if (iterableKind == IterableKind.ARRAY) {
				this.searchText = iterableName + "[" + indexName + "]";
			} else {
				this.searchText = "";
			}

			// Use null character to allow special handling near the matches
			// Java text file shouldn't contain null characters
			this.replacementText = this.elementName + (char) 0;

			// Treat as equal, so that indexed and for-each won't show as a difference
			// (this is especially useful for code reviews so don't see tons of changes, when the code is more or less the same functionally)
			this.diffType = new RefactoringDiffTypeValue('R', side, "enhanced for", iterableName, true);
		}

		boolean isValid() {
			return !this.searchText.isEmpty();
		}

		boolean accept(final String left, final String right) {
			String expectedText;
			String originalText;
			if (this.side == DiffSide.LEFT) {
				expectedText = left;
				originalText = right;
			} else {
				expectedText = right;
				originalText = left;
			}

			// Does normal text replace
			// (wouldn't correctly handle if string text or comment contained something that would match the refactoring)
			// However, this would require parsing the source code
			String refactoredText = originalText.replace(this.searchText, this.replacementText);

			//			System.out.println("Search for " + this.searchText);
			//			System.out.println("Refactored: " + refactoredText);
			//			System.out.println("Enhanced: " + expectedText);

			// Intentionally doing identify equal check
			// (since if there are no occurrences of searchText, then indexedForText will be returned unmodified)
			// (In this case, no refactoring was done)
			return !identityEquals(refactoredText, originalText)
					&& equalsWithSpecialHandling(refactoredText, expectedText);
		}
	}
}
