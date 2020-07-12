package info.codesaway.becr.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import info.codesaway.becr.parsing.CodeInfoWithLineInfo;
import info.codesaway.becr.parsing.FieldInfo;
import info.codesaway.becr.parsing.FieldInfoOption;
import info.codesaway.becr.parsing.MethodSignature;
import info.codesaway.becr.parsing.MethodSignatureOption;
import info.codesaway.becr.parsing.ParsingUtilities;
import info.codesaway.bex.diff.DiffLine;

public final class CompareDirectoriesVisitor extends ASTVisitor {

	private final CompilationUnit compilationUnit;

	private final List<CodeInfoWithLineInfo> details = new ArrayList<>();

	/**
	 * Map from next body of code to the start of the comment block
	 */
	private final Map<Integer, Integer> commentsMap = new HashMap<>();

	//	private final DifferencesResult differencesResult;

	public CompareDirectoriesVisitor(final CompilationUnit compilationUnit, final List<DiffLine> diffLines) {
		this.compilationUnit = compilationUnit;
		//		this.differencesResult = differencesResult;

		// TODO: continue here

		@SuppressWarnings("unchecked")
		List<Comment> comments = compilationUnit.getCommentList();

		for (Comment comment : comments) {
			if (comment.getNodeType() != ASTNode.BLOCK_COMMENT) {
				// Only looking for block comments, which are not Javadoc
				continue;
			}

			int commentStart = comment.getStartPosition();
			int commentEnd = commentStart + comment.getLength();

			int startLine = ParsingUtilities.getLineNumber(compilationUnit, commentStart);
			int endLine = ParsingUtilities.getLineNumber(compilationUnit, commentEnd);

			// -1 to convert from 1-based start in file to 0-based index in list
			DiffLine diffLine = diffLines.get(startLine - 1);

			// Line must start with the block comment
			if (!diffLine.getText().trim().startsWith("/*")) {
				continue;
			}

			//			System.out.println("Block comment from " + startLine + " to " + endLine);

			// TODO: see how can use comments (and whether it's commented out code, such as commented out Method or Field

			//			String commentText = diffLines
			//					.stream()
			//					.filter(d -> d.getNumber() >= startLine && d.getNumber() <= endLine)
			//					.map(DiffLine::getText)
			//					.collect(Collectors.joining(System.lineSeparator()))
			//					.trim();
			//
			//			// Remove leading /*
			//			if (!commentText.endsWith("*/")) {
			//				// All block comments should end with "*/"
			//				// Handle SecurityCache where has block comment ending on same line as line comment afterwards
			//				//				throw new AssertionError("Block comment does not end with */" + System.lineSeparator() + commentText);
			//
			//				try {
			//					Thread.sleep(50);
			//				} catch (InterruptedException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
			//
			//				System.err.println("Block comment does not end with */" + System.lineSeparator() + commentText);
			//
			//				try {
			//					Thread.sleep(50);
			//				} catch (InterruptedException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
			//
			//				// TODO: For now just ignore??
			//				continue;
			//			}

			//			commentText = commentText.substring("/*".length(), commentText.length() - "*/".length());

			// XXX: performance issue since creates new parser for each run
			// (see about reusing parser
			//			ASTNode node = CompareDirectories.getTypeOfCode(commentText);

			//			System.out.println(commentText);

			//			if (node != null) {
			//				//				System.out.println("Block comment from " + startLine + " to " + endLine);
			//				//				System.out.println("Comment contains code: " + System.lineSeparator() + node);
			//
			//				if (node instanceof TypeDeclaration) {
			//					TypeDeclaration typeDeclaration = (TypeDeclaration) node;
			//
			//					// TODO: check for commented out fields in block comments
			//					// (need to support FieldDeclaration when creating FieldInfo
			//
			//					for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			//						MethodSignature methSignature = new MethodSignature(methodDeclaration);
			//						// Get signature with type erasure, so can track if matches method
			//						// (so can indicate if comment out or bring back a method)
			//						//						System.out.println("Commented out method: "
			//						//								+ methSignature.getSignatureWithTypeErasure());
			//
			//						String signature = methSignature.getSignatureWithTypeErasure();
			//
			//						MultiLineTextSelection textSelection = new MultiLineTextSelection(startLine, endLine, signature,
			//								commentText, commentStart, commentEnd);
			//
			//						// TODO: need to continue here
			//						// Plan is to give more details about the type of change
			//					}
			//				}
			//			}

			int nextNonBlankLine = endLine + 1;

			// Find next line which has text on it
			for (int i = endLine + 1; i < diffLines.size(); i++) {
				DiffLine line = diffLines.get(i - 1);

				if (line.getText().trim().isEmpty()) {
					// Blank line
					nextNonBlankLine++;
				} else {
					break;
				}
			}

			if (nextNonBlankLine != endLine + 1) {
				//				System.out.println("Link comments to next code on line " + nextNonBlankLine);
				this.commentsMap.put(nextNonBlankLine, startLine);
			}
		}
	}

	private int getLineNumber(final int position) {
		return ParsingUtilities.getLineNumber(this.compilationUnit, position);
	}

	public List<CodeInfoWithLineInfo> getDetails() {
		return this.details;
	}

	@Override
	public boolean visit(final AnonymousClassDeclaration node) {
		// Ignore anonymous inner classes for now
		// Prevents overlap issue with anyonymous inner class within method body and where the difference lies
		return false;
	}

	@Override
	public boolean visit(final MethodDeclaration node) {
		IMethodBinding methodBinding = node.resolveBinding();

		MethodSignature methodSignature;

		if (methodBinding != null) {
			methodSignature = new MethodSignature(methodBinding, MethodSignatureOption.USE_SHORT_NAME);
		} else {
			methodSignature = new MethodSignature(node);
		}

		//		System.out.println(methodSignature);

		// Get lines for method
		// Get lines for method body as well as for entire methods (including any comments before)
		// This way, can indicate if change occurred within method itself
		// Can also use to read comments before method, such as Change History changed or annotations changed

		int nodeExtendedStart = this.compilationUnit.getExtendedStartPosition(node);
		int methodBodyStart = node.getName().getStartPosition();

		int nodeStart = node.getStartPosition();
		int nodeEnd = nodeStart + node.getLength();

		int nodeExtendedStartLine = this.getLineNumber(nodeExtendedStart);
		int methodBodyStartLine = this.getLineNumber(methodBodyStart);
		int nodeEndLine = this.getLineNumber(nodeEnd);

		Integer commentStart = this.commentsMap.get(nodeExtendedStartLine);

		if (commentStart != null) {
			nodeExtendedStartLine = commentStart;
		}

		//		System.out.println("Extended start: " + nodeExtendedStartLine);
		//		System.out.println("Body start: " + methodBodyStartLine);
		//		System.out.println("End: " + nodeEndLine);

		this.details.add(
				new CodeInfoWithLineInfo(methodSignature, nodeExtendedStartLine, methodBodyStartLine, nodeEndLine));

		return true;
	}

	@Override
	public boolean visit(final FieldDeclaration node) {
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> fragments = node.fragments();

		if (fragments.size() != 1) {
			// if has multiple fragments, skip for now
			// (wiil be seen under "Other" code changes)

			return true;
		}

		for (VariableDeclarationFragment fragment : fragments) {
			IVariableBinding variableBinding = fragment.resolveBinding();

			// Ignore serialVersionUID (used for Serialization)
			if (variableBinding != null) {
				FieldInfo fieldInfo = new FieldInfo(variableBinding, FieldInfoOption.USE_SHORT_NAME);

				int nodeExtendedStart = this.compilationUnit.getExtendedStartPosition(node);
				int fieldStart = fragment.getName().getStartPosition();

				int nodeStart = node.getStartPosition();
				int nodeEnd = nodeStart + node.getLength();

				int nodeExtendedStartLine = this.getLineNumber(nodeExtendedStart);
				int fieldStartLine = this.getLineNumber(fieldStart);
				int nodeEndLine = this.getLineNumber(nodeEnd);

				Integer commentStart = this.commentsMap.get(nodeExtendedStartLine);

				if (commentStart != null) {
					nodeExtendedStartLine = commentStart;
				}

				//				System.out.println("Field: " + fieldInfo + "\t" + nodeExtendedStartLine + "\t" + fieldStartLine + "\t"
				//						+ nodeEndLine);

				this.details.add(
						new CodeInfoWithLineInfo(fieldInfo, nodeExtendedStartLine, fieldStartLine, nodeEndLine));
			}
			//			else if (!Utilities.in(fragment.getName().toString(), "serialVersionUID")) {
			//				throw new AssertionError("Cannot read variable binding for " + fragment);
			//			}
		}

		return true;
	}
}
