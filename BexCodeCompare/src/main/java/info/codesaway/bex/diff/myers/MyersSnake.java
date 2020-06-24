package info.codesaway.bex.diff.myers;

public class MyersSnake {
	// Stores two points of info
	// Original source passes around as 2x2 array
	// https://blog.jcoglan.com/2017/04/25/myers-diff-in-linear-space-implementation/

	private final MyersPoint start;
	private final MyersPoint finish;

	public MyersSnake(final MyersPoint start, final MyersPoint finish) {
		this.start = start;
		this.finish = finish;
	}

	public MyersPoint getStart() {
		return this.start;
	}

	public MyersPoint getFinish() {
		return this.finish;
	}

	@Override
	public String toString() {
		return "Snake[" + this.start + ", " + this.finish + "]";
	}
}
