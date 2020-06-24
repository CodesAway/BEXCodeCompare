package info.codesaway.bex;

public class BEXUtilities {
	private BEXUtilities() {
		throw new UnsupportedOperationException();
	}

	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[2].toString();
	}
}
