package info.codesaway.bex.parsing;

import java.util.Objects;

import info.codesaway.bex.Indexed;

/**
 * ParsingState immutable implementation
 * @since 0.13
 */
public final class ParsingStateValue implements ParsingState {
	private final ParsingState parsingState;

	private final Indexed<ParsingState> parent;

	public ParsingStateValue(final ParsingState parsingState, final Indexed<ParsingState> parent) {
		this.parsingState = parsingState;

		this.parent = parent;
	}

	public ParsingState getParsingState() {
		return this.parsingState;
	}

	@Override
	public boolean isCode() {
		return this.parsingState.isCode();
	}

	@Override
	public boolean isComment() {
		return this.parsingState.isComment();
	}

	@Override
	public boolean isStringLiteral() {
		return this.parsingState.isStringLiteral();
	}

	@Override
	public boolean isWhitespace() {
		return this.parsingState.isWhitespace();
	}

	@Override
	public Indexed<ParsingState> getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return String.format("%s (parent at %s)", this.parsingState, this.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.parsingState);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		ParsingStateValue other = (ParsingStateValue) obj;
		return Objects.equals(this.parent, other.parent)
				&& Objects.equals(this.parsingState, other.parsingState);
	}
}
