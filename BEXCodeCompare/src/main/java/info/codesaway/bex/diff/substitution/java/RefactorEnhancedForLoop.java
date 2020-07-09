package info.codesaway.bex.diff.substitution.java;

import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.enhanceRegexWhitespace;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.equalsWithSpecialHandling;
import static info.codesaway.bex.diff.substitution.java.JavaSubstitution.identityEquals;
import static info.codesaway.util.regex.Pattern.getThreadLocalMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.BEXSide;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.RefactoringType;
import info.codesaway.util.regex.Matcher;

public class RefactorEnhancedForLoop implements JavaSubstitution, RefactoringType {
	private static final String ITERABLE_REGEX = "(?<iterable>(?:\\w++\\.)??\\w++|\\w++\\.get\\w++\\(\\))";

	private static final ThreadLocal<Matcher> ENHANCED_FOR_LOOP_MATCHER = getThreadLocalMatcher(enhanceRegexWhitespace(
			" for \\( (?:final )?+"
					+ "(?<type>\\w++(?:\\[\\])?+) (?<element>\\w++) "
					+ ": " + ITERABLE_REGEX + " "
					+ "\\) \\{? "));

	private static final String INDEX_FOR_LOOP_REGEX = " for \\( "
			+ "(?<type>int)\\s++(?<element>\\w++) = 0 ; "
			+ "\\k<element> < " + ITERABLE_REGEX + "\\.(?:(?<collection>size)\\(\\)||(?<array>length)) ; "
			// 6/6/2020 Support both pre and post fix notation, like Eclipse
			+ "(?:\\k<element>\\+\\+|\\+\\+\\k<element>) "
			+ "\\) \\{? ";

	private static final String ITERATOR_FOR_LOOP_REGEX = " for \\( "
			+ "(?<type>Iterator) (?<element>\\w++) = "
			+ ITERABLE_REGEX + "\\.(?<iterator>iterator)\\(\\) ; "
			+ "(?:\\k<element>\\.hasNext\\(\\) ; ) "
			+ "\\) \\{? ";

	private static final ThreadLocal<Matcher> REGULAR_FOR_LOOP_MATCHER = getThreadLocalMatcher(
			enhanceRegexWhitespace("(?J:" + INDEX_FOR_LOOP_REGEX + "|" + ITERATOR_FOR_LOOP_REGEX + ")"));

	private final Map<String, State> states = new HashMap<>();
	private State lastState = null;

	@Override
	public RefactoringDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		Matcher enhancedForLoopMatcher = ENHANCED_FOR_LOOP_MATCHER.get();
		Matcher regularForLoopMatcher = REGULAR_FOR_LOOP_MATCHER.get();

		BEXSide side;
		// TODO: switch from matches to find to handle named loop and line comments at end?
		if (enhancedForLoopMatcher.reset(normalizedRight).matches()
				&& regularForLoopMatcher.reset(normalizedLeft).matches()) {
			side = BEXSide.RIGHT;
		} else if (enhancedForLoopMatcher.reset(normalizedLeft).matches()
				&& regularForLoopMatcher.reset(normalizedRight).matches()) {
			side = BEXSide.LEFT;
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
		if (iterableName.equals(regularForLoopMatcher.get("iterable"))) {
			String elementType = enhancedForLoopMatcher.get("type");
			String elementName = enhancedForLoopMatcher.get("element");

			String indexName = regularForLoopMatcher.get("element");
			IterableKind iterableKind;

			if (regularForLoopMatcher.matched("array")) {
				iterableKind = IterableKind.ARRAY;
			} else if (regularForLoopMatcher.matched("collection")) {
				iterableKind = IterableKind.COLLECTION;
			} else if (regularForLoopMatcher.matched("iterator")) {
				iterableKind = IterableKind.ITERATOR;
			} else {
				throw new AssertionError("Unexpected iterable kind: " + regularForLoopMatcher.text());
			}

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
	public RefactoringDiffType acceptSingleSide(final BEXSide side, final DiffEdit diffEdit,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		if (this.lastState == null) {
			return null;
		} else if (this.lastState.side == side) {
			// We're expecting the deleted line to be on the indexed for loop side
			// (so the other side of the refactored enhanced side)
			return null;
		}

		String expectedText = DiffHelper.normalize(side,
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
		ARRAY, COLLECTION, ITERATOR
	}

	// Store state information so can track the changes
	private static class State {
		private final BEXSide side;
		private final String elementType;
		private final String elementName;
		//		private final String iterableName;
		//		private final String indexName;
		//		private final IterableKind iterableKind;
		private final String searchText;
		private final String replacementText;
		private final RefactoringDiffType diffType;

		private State(final BEXSide side, final String elementType, final String elementName,
				final String iterableName, final String indexName, final IterableKind iterableKind) {
			this.side = side;
			this.elementType = elementType;
			this.elementName = elementName;
			//			this.iterableName = iterableName;
			//			this.indexName = indexName;
			//			this.iterableKind = iterableKind;

			switch (iterableKind) {
			case COLLECTION:
				this.searchText = iterableName + ".get(" + indexName + ")";
				break;
			case ARRAY:
				this.searchText = iterableName + "[" + indexName + "]";
				break;
			case ITERATOR:
				this.searchText = indexName + ".next()";
				break;
			default:
				throw new AssertionError("Unexpected iterable kind: " + iterableKind);
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
			if (this.side == BEXSide.LEFT) {
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

			//			System.out.println("Checking for " + this.searchText);
			//			System.out.println(refactoredText);
			//			System.out.println(expectedText);

			// Intentionally doing identify equal check
			// (since if there are no occurrences of searchText, then indexedForText will be returned unmodified)
			// (In this case, no refactoring was done)
			if (identityEquals(refactoredText, originalText)) {
				return false;
			}

			if (equalsWithSpecialHandling(refactoredText, expectedText)) {
				return true;
			}

			// Handle if line also has cast
			// For example
			// Message message = (Message) messageList.getMessages().get(i);
			// Message message = element;
			refactoredText = refactoredText.replace("(" + this.elementType + ")", "\0");

			if (equalsWithSpecialHandling(refactoredText, expectedText)) {
				return true;
			}

			return false;
		}
	}
}
