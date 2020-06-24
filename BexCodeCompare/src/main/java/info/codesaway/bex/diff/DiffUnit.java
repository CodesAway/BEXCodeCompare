package info.codesaway.bex.diff;

import java.util.List;

/**
 * Common interface for DiffEdit and DiffBlock
 *
 */
// Class created on 1/6/2019
public interface DiffUnit {
	public List<DiffEdit> getEdits();

	public DiffType getType();
}
