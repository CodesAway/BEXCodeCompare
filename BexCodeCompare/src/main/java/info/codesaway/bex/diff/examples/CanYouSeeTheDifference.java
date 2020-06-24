package info.codesaway.bex.diff.examples;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CanYouSeeTheDifference {
	// Can you see the difference?
	public String methodAfter(final String text1, final String text2) {
		StringBuilder result = new StringBuilder();
		List<String> values = Arrays.asList("1", "22", "333");

		for (int m = 0; m <= 9; m++) {
			for (int n = 0; n <= 9; n++) {
				// Added to make lambda happy
				int mm = m;
				int nn = n;

				List<String> collect = values.stream()
						.map(v -> v + mm + nn)
						.collect(Collectors.toList());

				result.append(m)
						.append(n)
						.append(collect);
				
				for (String value : collect) {
					System.out.println(value);
				}
			}
		}

		return result.toString();
	}

	public String methodBefore(final String text1, final String text2) {
		StringBuilder result = new StringBuilder();
		List<String> values = Arrays.asList("1", "22", "333");

		for (int m = 0; m <= 9; m++) {
			for (int n = 0; n <= 9; n++) {
				// Added to make lambda happy
				int mm = m;
				int nn = n;

				List<String> collect = values.stream().map(v -> v + mm + nn).collect(Collectors.toList());

				result.append(m).append(m).append(collect);
				
				for (int i = 0; i < collect.size(); i++)
				{
					String value = collect.get(i);
					System.out.println(value);
				}
			}
		}

		return result.toString();
	}
}
