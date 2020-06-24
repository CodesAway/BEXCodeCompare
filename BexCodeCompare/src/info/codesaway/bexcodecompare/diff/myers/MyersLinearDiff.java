package info.codesaway.bexcodecompare.diff.myers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableList;

import info.codesaway.bexcodecompare.diff.DiffAlgorithm;
import info.codesaway.bexcodecompare.diff.DiffEdit;
import info.codesaway.bexcodecompare.diff.DiffLine;
import info.codesaway.bexcodecompare.diff.DiffNormalizedText;
import info.codesaway.bexcodecompare.diff.DiffTypeEnum;

public final class MyersLinearDiff implements DiffAlgorithm {
	// Java implementation of Ruby source code found at
	// (many of the comments in the code comes from the article)

	// Myers with linear space
	// https://blog.jcoglan.com/2017/03/22/myers-diff-in-linear-space-theory/
	// https://blog.jcoglan.com/2017/04/25/myers-diff-in-linear-space-implementation/

	// Myers
	// https://blog.jcoglan.com/2017/02/12/the-myers-diff-algorithm-part-1/
	// https://blog.jcoglan.com/2017/02/15/the-myers-diff-algorithm-part-2/
	// https://blog.jcoglan.com/2017/02/17/the-myers-diff-algorithm-part-3/

	private final List<DiffLine> lines1;
	private final List<DiffLine> lines2;

	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;

	private List<DiffEdit> diff;

	/**
	 * Minimum number of lines before run in parallel
	 */
	//	private static final int PARALLEL_THRESHOLD = 10_000;

	//    /**
	//     * Thread pool to use for recursive algorithm
	//     */
	//    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

	public MyersLinearDiff(final List<DiffLine> lines1, final List<DiffLine> lines2) {
		this(lines1, lines2, null);
	}

	public MyersLinearDiff(final List<DiffLine> lines1, final List<DiffLine> lines2,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		// Take a defensive clone of the passed values and retain them as immutable lists
		this.lines1 = ImmutableList.copyOf(lines1);
		this.lines2 = ImmutableList.copyOf(lines2);

		this.normalizationFunction = normalizationFunction != null ? normalizationFunction : NO_NORMALIZATION_FUNCTION;
	}

	public List<DiffLine> getLines1() {
		return this.lines1;
	}

	public List<DiffLine> getLines2() {
		return this.lines2;
	}

	@Override
	public BiFunction<String, String, DiffNormalizedText> getNormalizationFunction() {
		return this.normalizationFunction;
	}

	// Helper methods to make code more readable

	/**
	 * Gets the text from the specified line in {@link #getLines1()}
	 *
	 * @param index
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public String getText1(final int index) {
		return this.getLines1().get(index).getText();
	}

	/**
	 * Gets the text from the specified line in {@link #getLines2()}
	 *
	 * @param index
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public String getText2(final int index) {
		return this.getLines2().get(index).getText();
	}

	/**
	 * Returns the calculated diff (will be <code>null</code> until {@link #diff()} is run)
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018 Initial code
	 *</pre>***********************************************************************************
	 */
	public List<DiffEdit> getDiff() {
		return this.diff;
	}

	/**
	 * Calculates the diff
	 *
	 * @param lines1
	 * @param lines2
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public static List<DiffEdit> diff(final List<DiffLine> lines1, final List<DiffLine> lines2) {
		return new MyersLinearDiff(lines1, lines2).diff();
	}

	/**
	 * Calculates the diff
	 *
	 * @param lines1
	 * @param lines2
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    01/05/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	public static List<DiffEdit> diff(final List<DiffLine> lines1, final List<DiffLine> lines2,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		return new MyersLinearDiff(lines1, lines2, normalizationFunction).diff();
	}

	/**
	 * Calculates the diff
	 *
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	@Override
	public List<DiffEdit> diff() {
		this.diff = new ArrayList<>();

		this.walkSnakes((point1, point2) -> this.diff.add(this.handleDiff(point1, point2)));

		return this.getDiff();
	}

	/**
	 * Handles the diff (called from {@link #backtrack()}
	 *
	 * @param previousX
	 * @param previousY
	 * @param x
	 * @param y
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	private DiffEdit handleDiff(final MyersPoint point1, final MyersPoint point2) {
		//        System.out.println(point1 + " -> " + point2);

		if (point1.getX() == point2.getX()) {
			DiffLine line2 = this.getLines2().get(point1.getY());

			return new DiffEdit(DiffTypeEnum.INSERT, null, line2);
		} else if (point1.getY() == point2.getY()) {
			DiffLine line1 = this.getLines1().get(point1.getX());

			return new DiffEdit(DiffTypeEnum.DELETE, line1, null);
		} else {
			DiffLine line1 = this.getLines1().get(point1.getX());
			DiffLine line2 = this.getLines2().get(point1.getY());

			return this.newEqualOrNormalizeEdit(line1, line2);
		}
	}

	// Methods added for linear space version of Myers algorithm
	// Source: https://blog.jcoglan.com/2017/04/25/myers-diff-in-linear-space-implementation/

	private void walkSnakes(final BiFunction<MyersPoint, MyersPoint, ?> block) {
		MyersPoint topLeft = new MyersPoint(0, 0);
		MyersPoint bottomRight = new MyersPoint(this.getLines1().size(), this.getLines2().size());

		List<MyersPoint> path = this.findPath(topLeft, bottomRight);

		if (path == null) {
			return;
		}

		// For each two consecutive points
		for (int i = 1; i < path.size(); i++) {
			MyersPoint walkDiagonal = path.get(i - 1);
			MyersPoint point2 = path.get(i);

			walkDiagonal = this.walkDiagonal(walkDiagonal, point2, block);
			int x1 = walkDiagonal.getX();
			int y1 = walkDiagonal.getY();

			int compare = Integer.compare(point2.getX() - x1, point2.getY() - y1);
			if (compare != 0) {
				MyersPoint newWalkDiagonal = compare < 0 ? new MyersPoint(x1, y1 + 1) : new MyersPoint(x1 + 1, y1);

				block.apply(walkDiagonal, newWalkDiagonal);
				walkDiagonal = newWalkDiagonal;
			}

			this.walkDiagonal(walkDiagonal, point2, block);
		}
	}

	private MyersPoint walkDiagonal(final MyersPoint point1, final MyersPoint point2,
			final BiFunction<MyersPoint, MyersPoint, ?> block) {
		// Point which gets updated via the loop
		// (initialized to point1)
		MyersPoint point = point1;

		while (point.getX() < point2.getX() && point.getY() < point2.getY()
				&& this.normalize(this.getText1(point.getX()), this.getText2(point.getY())).hasEqualText()) {
			//                && this.getText1(point.getX()).equals(this.getText2(point.getY()))) {
			// Create new point by going along diagonal, incrementing both X and Y

			//            System.out.println("Diagonal: " + this.getText1(point.getX()) + "\t" + this.getText2(point.getY()));

			MyersPoint newPoint = new MyersPoint(point.getX() + 1, point.getY() + 1);
			block.apply(point, newPoint);
			point = newPoint;
		}

		return point;
	}

	protected List<MyersPoint> findPath(final MyersPoint topLeft, final MyersPoint bottomRight) {
		MyersBox box = new MyersBox(topLeft, bottomRight);
		MyersSnake snake = this.midpoint(box);

		//        System.out.printf("Checking %s to %s%n", topLeft, bottomRight);
		//        System.out.println("Midpoint: " + snake);

		if (snake == null) {
			return null;
		}

		MyersPoint start = snake.getStart();
		MyersPoint finish = snake.getFinish();

		List<MyersPoint> head;
		List<MyersPoint> tail;

		// TODO: for now, always run in Serial, since not sure if good to multi-thread in plugin
		// (also, in most cases, shouldn't have 10K lines of code
		//		if (box.size() > PARALLEL_THRESHOLD) {
		//			// Parallelize the task
		//			MyersLinearDiffRecursiveTask task1 = new MyersLinearDiffRecursiveTask(this, topLeft, start);
		//			MyersLinearDiffRecursiveTask task2 = new MyersLinearDiffRecursiveTask(this, finish, bottomRight);
		//
		//			ForkJoinTask.invokeAll(task1, task2);
		//
		//			head = task1.join();
		//			tail = task2.join();
		//		} else {
		// Use serial execution
		head = this.findPath(topLeft, start);
		tail = this.findPath(finish, bottomRight);
		//		}

		// Size of result is the sum of head + tail (if null, will be size 1, since adding the start or finish point)
		int resultSize = (head != null ? head.size() : 1) + (tail != null ? tail.size() : 1);
		List<MyersPoint> pathPoints = new ArrayList<>(resultSize);

		if (head != null) {
			pathPoints.addAll(head);
		} else {
			pathPoints.add(start);
		}

		if (tail != null) {
			pathPoints.addAll(tail);
		} else {
			pathPoints.add(finish);
		}

		return pathPoints;
	}

	private MyersSnake midpoint(final MyersBox box) {
		if (box.size() == 0) {
			return null;
		}

		int max = (int) Math.ceil(box.size() / 2.0);

		// The original algorithm mentions negative indexes
		// This is used to offset the indexes so they range from 0 to 2 * max instead of -max to +max
		int offset = max;

		int[] vf = new int[2 * max + 1];
		vf[1 + offset] = box.getLeft();
		int[] vb = new int[2 * max + 1];
		vb[1 + offset] = box.getBottom();

		for (int d = 0; d <= max; d++) {
			Optional<MyersSnake> snake;

			snake = this.forward(box, vf, vb, d, offset);

			// Only if present return the snake
			// (found the midpoint)
			if (snake.isPresent()) {
				return snake.get();
			}

			snake = this.backward(box, vf, vb, d, offset);

			// Only if present return the snake
			// (found the midpoint)
			if (snake.isPresent()) {
				return snake.get();
			}
		}

		throw new AssertionError("Found no midpoint in Myers.midpoint");
	}

	private Optional<MyersSnake> forward(final MyersBox box, final int[] vf, final int[] vb, final int d,
			final int offset) {

		// Check if delta is odd
		// * Will only return value if delta is odd
		// * If delta is even, will always return Optional.empty()
		// Note: side effects from method are necessary, which is why method is called
		boolean isDeltaOdd = isOdd(box.delta());

		//		for (int k = -d; k <= d; k += 2) {
		// Git uses reverse, which is what I'm comparing against for the test
		// (12/4/2019 seems to yield better diff, such as PFCLegalOrders when compare Phase 1 / Phase 2)
		for (int k = d; k >= -d; k -= 2) {
			int c = k - box.delta();

			int px;
			int x;
			if (k == -d || (k != d && vf[k - 1 + offset] < vf[k + 1 + offset])) {
				px = vf[k + 1 + offset];
				x = px;
			} else {
				px = vf[k - 1 + offset];
				x = px + 1;
			}

			int y = box.getTop() + (x - box.getLeft()) - k;
			int py = (d == 0 || x != px) ? y : y - 1;

			while (x < box.getRight() && y < box.getBottom()
					&& this.normalize(this.getText1(x), this.getText2(y)).hasEqualText()) {
				//                    && this.getText1(x).equals(this.getText2(y))) {
				x++;
				y++;
			}

			vf[k + offset] = x;

			if (isDeltaOdd && isBetween(c, -(d - 1), d - 1) && y >= vb[c + offset]) {
				return Optional.of(new MyersSnake(new MyersPoint(px, py), new MyersPoint(x, y)));
			}
		}

		return Optional.empty();
	}

	private Optional<MyersSnake> backward(final MyersBox box, final int[] vf, final int[] vb, final int d,
			final int offset) {
		// Check if delta is even
		// * Will only return value if delta is even
		// * If delta is odd, will always return Optional.empty()
		// Note: side effects from method are necessary, which is why method is called
		boolean isDeltaEven = isEven(box.delta());

		for (int c = -d; c <= d; c += 2) {
			// Git uses reverse, which is what I'm comparing against for the test
			//        for (int c = d; c >= -d; c -= 2) {
			int k = c + box.delta();

			int py;
			int y;
			if (c == -d || (c != d && vb[c - 1 + offset] > vb[c + 1 + offset])) {
				py = vb[c + 1 + offset];
				y = py;
			} else {
				py = vb[c - 1 + offset];
				y = py - 1;
			}

			int x = box.getLeft() + (y - box.getTop()) + k;
			int px = (d == 0 || y != py) ? x : x + 1;

			while (x > box.getLeft() && y > box.getTop()
					&& this.normalize(this.getText1(x - 1), this.getText2(y - 1)).hasEqualText()) {
				//                    && this.getText1(x - 1).equals(this.getText2(y - 1))) {
				x--;
				y--;
			}

			vb[c + offset] = y;

			if (isDeltaEven && isBetween(k, -d, d) && x <= vf[k + offset]) {
				return Optional.of(new MyersSnake(new MyersPoint(x, y), new MyersPoint(px, py)));
			}
		}

		return Optional.empty();
	}

	/**
	 * Indicates if a number is odd
	 *
	 * @param number
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	// Source: https://stackoverflow.com/a/30903516
	private static boolean isOdd(final int number) {
		return (number & 1) != 0;
	}

	/**
	 * Indicates if a number is even
	 *
	 * @param number
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T                  Amy Brennan-Luna    12/29/2018  Initial code
	 *</pre>***********************************************************************************
	 */
	private static boolean isEven(final int number) {
		return (number & 1) == 0;
	}

	/**
	 * Indicates if the specified <code>number</code> is between the specified <code>start</code> and <code>end</code> (inclusive)
	 *
	 * @param number
	 * @param start
	 * @param end
	 * @return
	 * @since
	 * <pre> Change History
	 * ========================================================================================
	 * Version  Change #        Developer           Date        Description
	 * =======  =============== =================== ==========  ===============================
	 * TRS.01T
	 *</pre>***********************************************************************************
	 */
	private static boolean isBetween(final int number, final int start, final int end) {
		return number >= start && number <= end;
	}
}
