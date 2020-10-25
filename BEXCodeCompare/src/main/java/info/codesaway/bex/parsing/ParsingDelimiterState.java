package info.codesaway.bex.parsing;

public class ParsingDelimiterState {
	private final ParsingDelimiterResult result;
	private final String delimiter;

	public static final ParsingDelimiterState NOT_FOUND = new ParsingDelimiterState(ParsingDelimiterResult.NOT_FOUND,
			"");

	/**
	 * @param result the result
	 * @param delimiter the delimiter or the empty String if there is no delimiter
	 */
	public ParsingDelimiterState(final ParsingDelimiterResult result, final String delimiter) {
		this.result = result;
		this.delimiter = delimiter;
	}

	public ParsingDelimiterResult getResult() {
		return this.result;
	}

	public String getDelimiter() {
		return this.delimiter;
	}
}
