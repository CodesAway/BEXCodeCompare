package info.codesaway.becr.matching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestMatchingWorkspaceFile {

	public static void main(final String[] args) throws IOException {
		//		String pattern = "~[ ]if (this.errorsPresent()) { return null; }";
		//		String pattern = "this.add~[type:w]Msg(~[eventId], ~[parameters]);";
		//		String pattern = "this.add@~~(?<type>Info|Warning|Error)~~@Msg(~[eventId], ~[parameters]);";
		String pattern = "EventInfo~[text.].asBus~[_.](~[fun]);";

		//		String workspace = "C:/Workspaces/Clean/TRSPhase1";
		String workspace = "C:/Workspaces/TRSPhase1";

		//		String relativePath = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/demographics/app/internal/pf/PFCEmployment.java";
		//		String relativePath = "/jClaretyWebProject/src/com/covansys/jclarety/demographics/web/internal/sa/SAEmployment.java";
		String relativePath = "/jClaretyEJBProject/ejbModule/com/covansys/jclarety/account/app/internal/pf/PFCAccountStatus.java";

		//		String groupName = "parameters";
		String groupName = "fun";

		Path path = Paths.get(workspace, relativePath);
		String text = new String(Files.readAllBytes(path));

		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		while (becrMatcher.find()) {
			System.out.printf("Found: %n%s%n", becrMatcher.group());
			System.out.printf("%s: @%s@%n", groupName, becrMatcher.group(groupName));
		}
	}
}
