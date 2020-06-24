package info.codesaway.bexcodecompare.diff.myers;

import java.util.List;
import java.util.concurrent.RecursiveTask;

public class MyersLinearDiffRecursiveTask extends RecursiveTask<List<MyersPoint>> {
	// Referenced: https://www.baeldung.com/java-fork-join
	private final MyersLinearDiff myers;

	private final MyersPoint topLeft;
	private final MyersPoint bottomRight;

	public MyersLinearDiffRecursiveTask(final MyersLinearDiff myers, final MyersPoint topLeft,
			final MyersPoint bottomRight) {
		this.myers = myers;
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	@Override
	protected List<MyersPoint> compute() {
		return this.myers.findPath(this.topLeft, this.bottomRight);
	}
}