package info.codesaway.becr.parsing;

import static info.codesaway.becr.parsing.ParsingUtilities.getParser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.jupiter.api.Test;

class MethodSignatureTest {

	@Test
	// Issue #101
	void testShortMethodSignatureName() {
		String source = "package info.codesaway.becr.parsing;\r\n" +
				"class MethodSignatureTest {\r\n" +
				"	void test() {\r\n" +
				"	}\r\n" +
				"}";

		ASTParser parser = getParser("");
		parser.setSource(source.toCharArray());
		// Set so can resolve bindings
		parser.setUnitName("MethodSignatureTest.java");

		AtomicInteger count = new AtomicInteger();
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(final MethodDeclaration node) {
				MethodSignature methodSignature = new MethodSignature(node.resolveBinding(),
						MethodSignatureOption.USE_SHORT_NAME);

				assertThat(methodSignature.toString()).isEqualTo("MethodSignatureTest.test()");
				count.addAndGet(1);
				return true;
			}
		};

		ASTNode astNode = parser.createAST(null);
		astNode.accept(visitor);

		assertThat(count.get()).isEqualTo(1);
	}

}
