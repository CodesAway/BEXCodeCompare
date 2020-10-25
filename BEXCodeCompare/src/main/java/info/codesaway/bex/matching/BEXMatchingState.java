package info.codesaway.bex.matching;

import static info.codesaway.bex.parsing.BEXParsingState.MISMATCHED_DELIMITERS;
import static info.codesaway.bex.util.BEXUtilities.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import info.codesaway.bex.BEXPair;
import info.codesaway.bex.parsing.BEXParsingState;
import info.codesaway.bex.parsing.ParsingState;

final class BEXMatchingState {
	private final int position;
	private final Collection<BEXPair<String>> delimiters;
	//	private final String brackets;
	private final Set<ParsingState> options;

	public static final BEXMatchingState DEFAULT = new BEXMatchingState(-1, Collections.emptyList());

	BEXMatchingState(final int position, final Collection<BEXPair<String>> delimiters,
			final BEXParsingState... options) {
		this.position = position;

		this.delimiters = delimiters.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableCollection(new ArrayList<>(delimiters));

		EnumSet<BEXParsingState> optionSet = EnumSet.noneOf(BEXParsingState.class);

		for (BEXParsingState option : options) {
			if (option != null) {
				optionSet.add(option);
			}
		}

		this.options = Collections.unmodifiableSet(optionSet);
	}

	BEXMatchingState(final int position, final Collection<BEXPair<String>> delimiters,
			final ParsingState... options) {
		this.position = position;

		this.delimiters = delimiters.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableCollection(new ArrayList<>(delimiters));

		Set<ParsingState> optionSet = new HashSet<>();

		for (ParsingState option : options) {
			if (option != null) {
				optionSet.add(option);
			}
		}

		this.options = Collections.unmodifiableSet(optionSet);
	}

	public int getPosition() {
		return this.position;
	}

	public Collection<BEXPair<String>> getDelimiters() {
		return this.delimiters;
	}

	public Set<ParsingState> getOptions() {
		return this.options;
	}

	//	public boolean isInStringLiteral() {
	//		return this.options.contains(IN_STRING_LITERAL);
	//	}

	public boolean hasMismatchedDelimiters() {
		return this.options.contains(MISMATCHED_DELIMITERS);
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

	public boolean isValid(final int expectedPosition, final Set<ParsingState> ignoreOptions) {
		return (this.position == expectedPosition || expectedPosition == -1)
				&& this.delimiters.isEmpty()
				&& this.isOptionsEmpty(ignoreOptions);
	}

	private boolean isOptionsEmpty(final Set<ParsingState> ignoreOptions) {
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
		return String.format("BEXMatchingState[%s, %s, %s]", this.position, this.delimiters, this.options);
	}
}
