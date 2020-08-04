package info.codesaway.bex.matching;

import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_LINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.bex.matching.BEXMatchingStateOption.IN_STRING_LITERAL;
import static info.codesaway.bex.matching.BEXMatchingStateOption.MISMATCHED_BRACKETS;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

final class BEXMatchingState {
	private final int position;
	private final String brackets;
	private final Set<BEXMatchingStateOption> options;

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

	public int getPosition() {
		return this.position;
	}

	public String getBrackets() {
		return this.brackets;
	}

	public Set<BEXMatchingStateOption> getOptions() {
		return this.options;
	}

	public boolean isInStringLiteral() {
		return this.options.contains(IN_STRING_LITERAL);
	}

	public boolean hasMismatchedBrackets() {
		return this.options.contains(MISMATCHED_BRACKETS);
	}

	public boolean isInLineComment() {
		return this.options.contains(IN_LINE_COMMENT);
	}

	public boolean isInMultilineComment() {
		return this.options.contains(IN_MULTILINE_COMMENT);
	}

	public boolean isValid(final int expectedPosition) {
		return this.isValid(expectedPosition, Collections.emptySet());
	}

	public boolean isValid(final int expectedPosition, final Set<BEXMatchingStateOption> ignoreOptions) {
		Set<BEXMatchingStateOption> compareOptions;
		if (ignoreOptions.isEmpty() || this.options.isEmpty()) {
			compareOptions = this.options;
		} else {
			compareOptions = EnumSet.copyOf(this.options);
			compareOptions.removeAll(ignoreOptions);
		}

		return (this.position == expectedPosition || expectedPosition == -1)
				&& this.brackets.length() == 0
				&& compareOptions.isEmpty();
	}

	@Override
	public String toString() {
		// TODO: fix this
		return String.format("BEXMatchingState[%s, %s, %s", this.position, this.brackets, this.options);
	}
}
