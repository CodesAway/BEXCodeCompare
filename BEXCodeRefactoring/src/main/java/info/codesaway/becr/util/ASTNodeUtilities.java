package info.codesaway.becr.util;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

import info.codesaway.bex.IntBEXRange;
import info.codesaway.bex.matching.BEXMatchResult;

public final class ASTNodeUtilities {
	private ASTNodeUtilities() {
		throw new UnsupportedOperationException();
	}

	public static IntBEXRange getStartEnd(final ASTNode node) {
		int start = node.getStartPosition();
		int end = start + node.getLength();

		return IntBEXRange.of(start, end);
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
	public static NavigableMap<Integer, IntBEXRange> getCommentRanges(final CompilationUnit cu) {
		@SuppressWarnings("unchecked")
		List<Comment> commentList = cu.getCommentList();

		TreeMap<Integer, IntBEXRange> commentRanges = new TreeMap<>();

		for (Comment comment : commentList) {
			commentRanges.put(comment.getStartPosition(), getStartEnd(comment));
		}

		return Collections.unmodifiableNavigableMap(commentRanges);
	}

	public static ASTNode findNode(final CompilationUnit cu, final BEXMatchResult match) {
		int start = match.start();
		int end = match.end();
		String text = match.text();

		// Ignore leading and trailing whitespace when finding the corresponding node
		while (start < end && Character.isWhitespace(text.codePointAt(start))) {
			start++;
		}

		while (end > start && Character.isWhitespace(text.codePointAt(end - 1))) {
			end--;
		}

		if (end <= start) {
			// Entire match is whitespace, so match original range
			return NodeFinder.perform(cu, match.start(), match.end() - match.start());
		}

		return NodeFinder.perform(cu, start, end - start);
	}
}
