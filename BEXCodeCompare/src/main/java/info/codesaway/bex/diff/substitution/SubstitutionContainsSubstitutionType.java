package info.codesaway.bex.diff.substitution;

import java.util.Map;
import java.util.function.BiFunction;

import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffNormalizedText;
import info.codesaway.bex.diff.substitution.SubstitutionContainsDiffType.Direction;

// TODO: give better name
public final class SubstitutionContainsSubstitutionType implements SubstitutionType {
	@Override
	public SubstitutionDiffType accept(final DiffEdit left, final DiffEdit right,
			final Map<DiffEdit, String> normalizedTexts,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		String normalizedLeft = normalizedTexts.get(left);
		String normalizedRight = normalizedTexts.get(right);

		boolean isSubstring = false;
		String prefix = "";
		Direction direction = null;
		String suffix = "";

		if (normalizedRight.length() >= normalizedLeft.length()) {
			// Insert is longer, so check if contains delete
			// TODO: 3/26/2019 - use last index of, so if empty block of text, see entire line as prefix versus suffix
			int index = normalizedRight.lastIndexOf(normalizedLeft);

			if (index != -1) {
				// Right contains left
				isSubstring = true;
				prefix = normalizedRight.substring(0, index);
				direction = Direction.RIGHT_CONTAINS_LEFT;
				suffix = normalizedRight.substring(index + normalizedLeft.length());
			}
		} else {
			// Delete is longer, so check if contains insert
			// TODO: 3/26/2019 - use last index of, so if empty block of text, see entire line as prefix versus suffix
			int index = normalizedLeft.lastIndexOf(normalizedRight);

			if (index != -1) {
				// Left contains right
				isSubstring = true;
				prefix = normalizedLeft.substring(0, index);
				direction = Direction.LEFT_CONTAINS_RIGHT;
				suffix = normalizedLeft.substring(index + normalizedRight.length());
			}
		}

		// TODO: make setting?
		// Only consider substring if at most this number of characters
		// (needed to differentiate between deleted line versus substitution (like commenting out blank line))
		int blankLineCompareThreshhold = 3;
		if (isSubstring
				&& (normalizedLeft.isEmpty() || normalizedRight.isEmpty())
				&& (prefix.length() > blankLineCompareThreshhold
						|| suffix.length() > blankLineCompareThreshhold)) {
			isSubstring = false;
		}

		return isSubstring
				? new SubstitutionContainsDiffType(prefix, direction, suffix)
				: null;
	}
}
