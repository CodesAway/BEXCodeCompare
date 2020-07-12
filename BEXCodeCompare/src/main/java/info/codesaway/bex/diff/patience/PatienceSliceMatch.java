package info.codesaway.bex.diff.patience;

import static info.codesaway.bex.util.BEXUtilities.immutableCopyOf;

import java.util.List;

public final class PatienceSliceMatch {
	private final PatienceSlice slice;
	private final List<PatienceMatch> matches;

	/**
	 *
	 * @param slice
	 * @param matches the matches (can be an empty list if there are no matches, or can be one or more matches)
	 */
	public PatienceSliceMatch(final PatienceSlice slice, final List<PatienceMatch> matches) {
		this.slice = slice;
		this.matches = immutableCopyOf(matches);
		//		this.matches = ImmutableList.copyOf(matches);
	}

	public PatienceSlice getSlice() {
		return this.slice;
	}

	public List<PatienceMatch> getMatches() {
		return this.matches;
	}
}
