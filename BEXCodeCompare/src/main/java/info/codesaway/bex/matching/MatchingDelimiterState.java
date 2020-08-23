package info.codesaway.bex.matching;

public class MatchingDelimiterState {
	private final MatchingDelimiterResult result;
	private final String delimiter;

	public static final MatchingDelimiterState NOT_FOUND = new MatchingDelimiterState(MatchingDelimiterResult.NOT_FOUND,
			"");

	/**
	 * @param result the result
	 * @param delimiter the delimiter or the empty String if there is no delimiter
	 */
	public MatchingDelimiterState(final MatchingDelimiterResult result, final String delimiter) {
		this.result = result;
		this.delimiter = delimiter;
	}

	public MatchingDelimiterResult getResult() {
		return this.result;
	}

	public String getDelimiter() {
		return this.delimiter;
	}
}
