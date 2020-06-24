package info.codesaway.bexcodecompare.diff;

import java.util.List;

/**
 * Common interface for DiffEdit and DiffBlock
 * @author TRSHCO
 *
 */
// Class created on 1/6/2019
public interface DiffUnit {
	public List<DiffEdit> getEdits();

	public DiffType getType();
}
