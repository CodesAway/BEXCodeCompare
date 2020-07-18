package info.codesaway.bex;

public class BEXUtilities {
	private BEXUtilities() {
		throw new UnsupportedOperationException();
	}

	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[2].toString();
	}
	
	private static boolean contentEquals(final StringBuilder left, final StringBuilder right) {
		// Check length as fast check
		if (left.length() != right.length()) {
			return false;
		}

		// Compare contents
		for (int i = 0; i < right.length(); i++) {
			if (left.charAt(i) != right.charAt(i)) {
				return false;
			}
		}

		return true;
	}
}
