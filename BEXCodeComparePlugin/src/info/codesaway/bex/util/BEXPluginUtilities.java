package info.codesaway.bex.util;

public class BEXPluginUtilities {
	private BEXPluginUtilities() {
		throw new UnsupportedOperationException();
	}

	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[2].toString();
	}
}
