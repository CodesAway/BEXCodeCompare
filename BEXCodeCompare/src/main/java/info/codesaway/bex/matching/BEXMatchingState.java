package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXMatchingStateOption.MISMATCHED_BRACKETS;
import static info.codesaway.bex.util.BEXUtilities.not;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

final class BEXMatchingState {
	private final int position;
	private final String brackets;
	private final Set<MatchingStateOption> options;

	public static final BEXMatchingState DEFAULT = new BEXMatchingState(-1, "");

	public BEXMatchingState(final int position, final String brackets, final BEXMatchingStateOption... options) {
		this.position = position;
		this.brackets = brackets;
		EnumSet<BEXMatchingStateOption> optionSet = EnumSet.noneOf(BEXMatchingStateOption.class);

		for (BEXMatchingStateOption option : options) {
			if (option != null) {
				optionSet.add(option);
			}
		}

		this.options = Collections.unmodifiableSet(optionSet);
	}

	public BEXMatchingState(final int position, final String brackets, final MatchingStateOption... options) {
		this.position = position;
		this.brackets = brackets;
		Set<MatchingStateOption> optionSet = new HashSet<>();

		for (MatchingStateOption option : options) {
			if (option != null) {
				optionSet.add(option);
			}
		}

		this.options = Collections.unmodifiableSet(optionSet);
	}

	public int getPosition() {
		return this.position;
	}

	public String getBrackets() {
		return this.brackets;
	}

	public Set<MatchingStateOption> getOptions() {
		return this.options;
	}

	//	public boolean isInStringLiteral() {
	//		return this.options.contains(IN_STRING_LITERAL);
	//	}

	public boolean hasMismatchedBrackets() {
		return this.options.contains(MISMATCHED_BRACKETS);
	}

	//	public boolean isInLineComment() {
	//		return this.options.contains(IN_LINE_COMMENT);
	//	}

	//	public boolean isInMultilineComment() {
	//		return this.options.contains(IN_MULTILINE_COMMENT);
	//	}

	public boolean isValid(final int expectedPosition) {
		return this.isValid(expectedPosition, Collections.emptySet());
	}

	public boolean isValid(final int expectedPosition, final Set<MatchingStateOption> ignoreOptions) {
		return (this.position == expectedPosition || expectedPosition == -1)
				&& this.brackets.length() == 0
				&& this.isOptionsEmpty(ignoreOptions);
	}

	private boolean isOptionsEmpty(final Set<MatchingStateOption> ignoreOptions) {
		if (ignoreOptions.isEmpty() || this.options.isEmpty()) {
			return this.options.isEmpty();
		} else {
			return !this.options.stream()
					.filter(not(ignoreOptions::contains))
					.findAny()
					.isPresent();

			//			Set<MatchingStateOption> compareOptions = new HashSet<>(this.options);
			//			compareOptions.removeAll(ignoreOptions);
			//			return compareOptions.isEmpty();
		}
	}

	@Override
	public String toString() {
		// TODO: fix this
		return String.format("BEXMatchingState[%s, %s, %s]", this.position, this.brackets, this.options);
	}
}
