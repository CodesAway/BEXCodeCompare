package info.codesaway.becr.matching;

import static info.codesaway.becr.matching.BECRStateOption.IN_LINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_MULTILINE_COMMENT;
import static info.codesaway.becr.matching.BECRStateOption.IN_STRING_LITERAL;
import static info.codesaway.becr.matching.BECRStateOption.MISMATCHED_BRACKETS;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

final class BECRState {
	private final int position;
	private final String brackets;
	private final Set<BECRStateOption> options;

	public static final BECRState DEFAULT = new BECRState(-1, "");

	public BECRState(final int position, final String brackets, final BECRStateOption... options) {
		this.position = position;
		this.brackets = brackets;
		EnumSet<BECRStateOption> optionSet = EnumSet.noneOf(BECRStateOption.class);

		for (BECRStateOption option : options) {
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

	public Set<BECRStateOption> getOptions() {
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

	public boolean isValid(final int expectedPosition, final Set<BECRStateOption> ignoreOptions) {
		Set<BECRStateOption> compareOptions;
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
		return String.format("BECRState[%s, %s, %s", this.position, this.brackets, this.options);
	}
}
