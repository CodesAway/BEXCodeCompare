package info.codesaway.bex.matching;

public enum BEXMatchingStateOption implements MatchingStateOption {
	IN_STRING_LITERAL, MISMATCHED_BRACKETS, IN_LINE_COMMENT, IN_MULTILINE_COMMENT, IN_EXPRESSION_BLOCK(true),
	IN_SECONDARY_STRING_LITERAL;

	private final boolean isCode;

	private BEXMatchingStateOption() {
		this(false);
	}

	private BEXMatchingStateOption(final boolean isCode) {
		this.isCode = isCode;
	}

	@Override
	public boolean isCode() {
		return this.isCode;
	}
}
