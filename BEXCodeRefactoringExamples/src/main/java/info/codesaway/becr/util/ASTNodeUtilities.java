package info.codesaway.becr.util;

import static info.codesaway.bex.util.BEXUtilities.isBetween;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;

import info.codesaway.bex.IntBEXPair;
import info.codesaway.bex.IntPair;

public final class ASTNodeUtilities {
	private ASTNodeUtilities() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets the comment ranges for the comments in the specified CompilationUnit
	 *
	 * <p>For each entry in the returned map</p>
	 * <ul>
	 * <li>The key is start position of the comment</li>
	 * <li>The value is the position range (start, inclusive to end, exclusive) for the comment
	 * </ul>
	 * @param cu the CompilationUnit
	 * @return the comment ranges
	 */
	public static NavigableMap<Integer, IntPair> getCommentRanges(final CompilationUnit cu) {
		@SuppressWarnings("unchecked")
		List<Comment> commentList = cu.getCommentList();

		TreeMap<Integer, IntPair> commentRanges = new TreeMap<>();

		for (Comment comment : commentList) {
			int start = comment.getStartPosition();
			int end = start + comment.getLength();

			commentRanges.put(start, IntBEXPair.of(start, end));
		}

		return Collections.unmodifiableNavigableMap(commentRanges);
	}

	public static boolean isInComment(final int start, final NavigableMap<Integer, IntPair> commentRanges) {
		Entry<Integer, IntPair> entry = commentRanges.floorEntry(start);

		if (entry == null) {
			return false;
		}

		IntPair range = entry.getValue();

		// Subtract 1 from end since exclusive
		return isBetween(start, range.getLeft(), range.getRight() - 1);
	}
}
