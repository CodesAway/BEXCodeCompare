package info.codesaway.bexcodecompare.util;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

import info.codesaway.util.regex.Matcher;
import info.codesaway.util.regex.Pattern;

@NonNullByDefault
public class RegexUtilities {
	/**
	 * Get thread local matcher for the specified regular expression
	 *
	 * @param regex
	 * @return
	 */
	public static ThreadLocal<Matcher> getThreadLocalMatcher(final String regex) {
		@NonNull
		@SuppressWarnings("null")
		ThreadLocal<Matcher> result = ThreadLocal.withInitial(() -> Pattern.compile(regex).matcher(""));
		return result;
	}
}
