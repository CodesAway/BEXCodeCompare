package info.codesaway.becr.matching;

import java.util.Arrays;
import java.util.List;

public class TestMatching {
	public static void main(final String[] args) {
		//		Character.is

		String pattern = "this.add@~~(?<type>Info|Warning|Error)~~@Msg(~[eventId], ~[parameters]);";

		//		String pattern = "this.add@~~(?<type>Info|Warning|Error)~~@Msg(~[eventId], ~[parameters]);";
		// TODO: support limiting match to just word characters
		// TODO: support limiting match to just word characters and punctuation
		//		String pattern = "this.add~[type]Msg(~[eventId], ~[parameters]);";
		//		String pattern = "this.add~[1]Msg(~[2], ~[3]);";

		//		String text = "			this.addErrorMsg(0, new String[] { this.getText(\"error.evenNotAvailable\" ), \"test\" });";
		String text = "			this.addErrorMsg(0, new String[] { this.getText(\"error.evenNotAvailable\", \"blah\" ), \"test\" });";

		//		String text = "this.addErrorMsg(1268, null);";

		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		//		List<String> groups = Arrays.asList("eventId", "parameter1", "parameter2", "type");
		List<String> groups = Arrays.asList("eventId", "parameters", "type");
		//		List<String> groups = Arrays.asList("_", "2", "3");

		if (becrMatcher.find()) {
			System.out.println("Yea!");

			for (String group : groups) {
				System.out.printf("%-15s%s%n", group, becrMatcher.get(group));
			}

			String parametersPattern1 = "new String[] { ~[parameter1] }";
			BECRPattern pattern1 = BECRPattern.compile(parametersPattern1);

			String parametersPattern2 = "new String[] { ~[parameter1], ~[parameter2] }";
			BECRPattern pattern2 = BECRPattern.compile(parametersPattern2);

			String parameters = becrMatcher.get("parameters");
			//			String parameters = becrMatcher.get("3");

			BECRMatcher matcher1 = pattern1.matcher(parameters);
			BECRMatcher matcher2 = pattern2.matcher(parameters);

			if (matcher1.find()) {
				List<String> groups1 = Arrays.asList("parameter1");
				for (String group : groups1) {
					System.out.printf("%-15s%s%n", group, matcher1.get(group));
				}
			} else if (matcher2.find()) {
				List<String> groups2 = Arrays.asList("parameter1", "parameter2");
				for (String group : groups2) {
					System.out.printf("%-15s%s%n", group, matcher2.get(group));
				}
			} else {
				System.out.println("Didn't match any matcher: " + parameters);
			}
		}
	}
}
