package info.codesaway.bex.parsing;

import static info.codesaway.bex.util.BEXUtilities.in;

public enum BEXParsingState implements ParsingState {
	// Code
	IN_EXPRESSION_BLOCK, IN_TAG,
	// Comment
	IN_LINE_COMMENT, IN_MULTILINE_COMMENT, IN_SECONDARY_MULTILINE_COMMENT,
	// String literal
	IN_STRING_LITERAL, IN_SECONDARY_STRING_LITERAL,
	// Whitespace
	WHITESPACE, LINE_TERMINATOR,
	// Other states
	MISMATCHED_DELIMITERS;

	@Override
	public boolean isCode() {
		return in(this, IN_EXPRESSION_BLOCK, IN_TAG);
	}

	@Override
	public boolean isComment() {
		return in(this, IN_LINE_COMMENT, IN_MULTILINE_COMMENT, IN_SECONDARY_MULTILINE_COMMENT);
	}

	@Override
	public boolean isStringLiteral() {
		return in(this, IN_STRING_LITERAL, IN_SECONDARY_STRING_LITERAL);
	}

	@Override
	public boolean isWhitespace() {
		return in(this, WHITESPACE, LINE_TERMINATOR);
	}
}
