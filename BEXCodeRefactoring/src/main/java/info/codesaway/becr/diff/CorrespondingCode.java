package info.codesaway.becr.diff;

import java.util.Objects;

import info.codesaway.becr.parsing.CodeInfoWithLineInfo;
import info.codesaway.bex.BEXPairCore;

public class CorrespondingCode implements BEXPairCore<CodeInfoWithLineInfo> {
	private final CodeInfoWithLineInfo left;
	private final CodeInfoWithLineInfo right;

	public CorrespondingCode(final CodeInfoWithLineInfo left,
			final CodeInfoWithLineInfo right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public CodeInfoWithLineInfo getLeft() {
		return this.left;
	}

	@Override
	public CodeInfoWithLineInfo getRight() {
		return this.right;
	}

	@Override
	public String toString() {
		return this.toString("%s -> %s");
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.left, this.right);
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
		CorrespondingCode other = (CorrespondingCode) obj;
		return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
	}
}
