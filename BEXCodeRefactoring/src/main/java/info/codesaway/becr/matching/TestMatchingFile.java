package info.codesaway.becr.matching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestMatchingFile {
	public static void main(final String[] args) throws IOException {
		Path path = Paths.get("C:\\Users\\trshco\\Downloads\\AbstractRedisConnection.java");
		String text = new String(Files.readAllBytes(path));

		// TODO: support multiple consecutive "holes"
		// Currently, puts everything in final group and empty in first two
		String pattern = "import ~[name]~[class]~[fun];";
		//		String pattern = "import ~[name].*;";

		BECRPattern becrPattern = BECRPattern.compile(pattern);
		BECRMatcher becrMatcher = becrPattern.matcher(text);

		if (becrMatcher.find()) {
			System.out.println("Found: " + becrMatcher.get("class"));
		} else {
			System.out.println("No match :(");
		}

		System.out.println();

		System.out.println(text);
	}
}
