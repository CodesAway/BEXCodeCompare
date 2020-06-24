package info.codesaway.bexcodecompare.diff.patience;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class PatienceSliceMatch {
	private final PatienceSlice slice;
	private final List<PatienceMatch> matches;

	/**
	 *
	 * @param slice
	 * @param matches the matches (can be an empty list if there are no matches, or can be one or more matches)
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/01/2019  Initial code
	 *</pre>***********************************************************************************
	 */
	public PatienceSliceMatch(final PatienceSlice slice, final List<PatienceMatch> matches) {
		this.slice = slice;
		this.matches = ImmutableList.copyOf(matches);
	}

	public PatienceSlice getSlice() {
		return this.slice;
	}

	public List<PatienceMatch> getMatches() {
		return this.matches;
	}
}
