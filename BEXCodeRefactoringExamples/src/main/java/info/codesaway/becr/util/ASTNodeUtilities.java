package info.codesaway.becr.util;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

import info.codesaway.becr.StartEndIntPair;
import info.codesaway.becr.matching.BECRMatchResult;

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
	public static NavigableMap<Integer, StartEndIntPair> getCommentRanges(final CompilationUnit cu) {
		@SuppressWarnings("unchecked")
		List<Comment> commentList = cu.getCommentList();

		TreeMap<Integer, StartEndIntPair> commentRanges = new TreeMap<>();

		for (Comment comment : commentList) {
			int start = comment.getStartPosition();
			int end = start + comment.getLength();

			commentRanges.put(start, StartEndIntPair.of(start, end));
		}

		return Collections.unmodifiableNavigableMap(commentRanges);
	}

	public static boolean isInComment(final int start, final NavigableMap<Integer, StartEndIntPair> commentRanges) {
		Entry<Integer, StartEndIntPair> entry = commentRanges.floorEntry(start);

		if (entry == null) {
			return false;
		}

		return entry.getValue().contains(start);
	}

	public static ASTNode findNode(final CompilationUnit cu, final BECRMatchResult match) {
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
