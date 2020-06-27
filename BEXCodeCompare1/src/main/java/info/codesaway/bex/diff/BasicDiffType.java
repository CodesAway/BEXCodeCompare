package info.codesaway.bex.diff;

import java.util.function.BiFunction;

import info.codesaway.bex.diff.substitution.RefactoringDiffType;
import info.codesaway.bex.diff.substitution.RefactoringDiffTypeValue;
import info.codesaway.bex.diff.substitution.SubstitutionDiffType;
import info.codesaway.bex.diff.substitution.SubstitutionDiffTypeValue;

public enum BasicDiffType implements DiffType {
	/**
	 * An insert
	 */
	INSERT('+'),

	/**
	 * A delete
	 */
	DELETE('-'),

	/**
	 * Indicates a diff which is equal
	 */
	EQUAL(' '),

	/**
	 * Indicates a diff which is equal after normalization
	 *
	 * @see DiffHelper#normalize(String, String, BiFunction)
	 */
	NORMALIZE('N'),

	/**
	 * Indicates a diff which can be ignored
	 */
	IGNORE('X'),

	/**
	 * Can be used to indicate a DiffBlock containing both {@link #MOVE_LEFT} and {@link #MOVE_RIGHT}
	 */
	MOVE_BLOCK('M', true),

	/**
	 * Move originating from left side
	 */
	MOVE_LEFT('m', true),

	/**
	 * Move originating from right side
	 */
	MOVE_RIGHT('M', true),

	;

	// Values not part of enum, but wanted to keep in same class, so can easily use static imports for all the BasicDiffType
	// Not part of enum, since want these instances to implement certain interfaces
	public static final SubstitutionDiffType SUBSTITUTE = new SubstitutionDiffTypeValue('S', "SUBSTITUTE");

	/**
	 * Can be used to indicate a substitution which isn't similar
	 */
	public static final SubstitutionDiffType REPLACEMENT_BLOCK = new SubstitutionDiffTypeValue('R', "REPLACEMENT");

	/**
	 * Indicates a refactoring substitution
	 */
	public static final RefactoringDiffType REFACTOR = new RefactoringDiffTypeValue('R', null, null, null);

	private final char tag;
	private final boolean isMove;

	private BasicDiffType(final char tag) {
		this(tag, false);
	}

	private BasicDiffType(final char tag, final boolean isMove) {
		this.tag = tag;
		this.isMove = isMove;
	}

	@Override
	public char getTag() {
		return this.tag;
	}

	@Override
	public boolean isMove() {
		return this.isMove;
	}

	@Override
	public boolean isSubstitution() {
		return false;
	}
}
