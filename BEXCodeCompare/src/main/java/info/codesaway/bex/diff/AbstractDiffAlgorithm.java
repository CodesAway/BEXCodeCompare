package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.NormalizationFunction.NO_NORMALIZATION;
import static info.codesaway.bex.diff.NormalizationFunction.normalization;
import static info.codesaway.bex.util.BEXUtilities.firstNonNull;
import static info.codesaway.bex.util.BEXUtilities.immutableCopyOf;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 */
public abstract class AbstractDiffAlgorithm implements DiffAlgorithm {
	private final List<DiffLine> leftLines;
	private final List<DiffLine> rightLines;

	private final NormalizationFunction normalizationFunction;
	//	private final BiFunction<String, String, DiffNormalizedText> normalizationFunction;

	private final AtomicReference<List<DiffEdit>> cachedDiff = new AtomicReference<>();

	/**
	 *
	 * @param leftLines
	 * @param rightLines
	 * @param normalizationFunction the normalization function (if null, mimics {@link DiffHelper#NO_NORMALIZATION_FUNCTION})
	 */
	protected AbstractDiffAlgorithm(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final BiFunction<String, String, DiffNormalizedText> normalizationFunction) {
		this(leftLines, rightLines, normalization(normalizationFunction));
	}

	/**
	 *
	 * @param leftLines
	 * @param rightLines
	 * @param normalizationFunction the normalization function (if null, mimics {@link NormalizationFunction#NO_NORMALIZATION})
	 * @since 0.14
	 */
	protected AbstractDiffAlgorithm(final List<DiffLine> leftLines, final List<DiffLine> rightLines,
			final NormalizationFunction normalizationFunction) {
		// Take a defensive clone of the passed values and retain them as immutable lists
		//		this.leftLines = ImmutableList.copyOf(leftLines);
		//		this.rightLines = ImmutableList.copyOf(rightLines);
		this.leftLines = immutableCopyOf(leftLines);
		this.rightLines = immutableCopyOf(rightLines);
		this.normalizationFunction = firstNonNull(normalizationFunction, NO_NORMALIZATION);
	}

	/**
	 * Gets the left lines
	 * @return the left lines
	 */
	@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Immutable copy in constructor")
	public List<DiffLine> getLeftLines() {
		return this.leftLines;
	}

	/**
	 * Gets the right lines.
	 *
	 * @return the right lines
	 */
	@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Immutable copy in constructor")
	public List<DiffLine> getRightLines() {
		return this.rightLines;
	}

	// Note: breaking change in 0.14
	@Override
	public NormalizationFunction getNormalizationFunction() {
		return this.normalizationFunction;
	}

	// Helper methods to make code more readable

	/**
	 * Gets the text from the specified line in {@link #getLeftLines()}
	 *
	 * @param position the position
	 * @return the left text on the specified line number
	 */
	public String getLeftText(final int position) {
		return this.getLeftIndexedText(position).getText();
	}

	/**
	 * Gets the text from the specified line in {@link #getRightLines()}
	 *
	 * @param position the position
	 * @return the right text on the specified line
	 */
	public String getRightText(final int position) {
		return this.getRightIndexedText(position).getText();
	}

	/**
	 * Gets the line number and text from the specified line in {@link #getLeftLines()}
	 *
	 * @param position the position
	 * @return the left line number and text on the specified line number
	 * @since 0.14
	 */
	public DiffLine getLeftIndexedText(final int position) {
		return this.getLeftLines().get(position);
	}

	/**
	 * Gets the line number and text from the specified line in {@link #getRightLines()}
	 *
	 * @param position the position
	 * @return the right line number and text on the specified line
	 * @since 0.14
	 */
	public DiffLine getRightIndexedText(final int position) {
		return this.getRightLines().get(position);
	}

	/**
	 * Gets the calculated diff
	 *
	 * <p>This is a lazy getter and the diff is only calculated the first time it's run</p>
	 *
	 * @return the calculated diff
	 */
	// Reference: https://projectlombok.org/features/GetterLazy
	@SuppressFBWarnings(value = "JLM_JSR166_UTILCONCURRENT_MONITORENTER", justification = "Code from Project Lombok")
	public List<DiffEdit> getDiff() {
		List<DiffEdit> value = this.cachedDiff.get();
		if (value == null) {
			synchronized (this.cachedDiff) {
				value = this.cachedDiff.get();
				if (value == null) {
					List<DiffEdit> actualValue = this.diff();
					this.cachedDiff.set(actualValue);
					value = actualValue;
				}
			}
		}
		return value;
	}

	/**
	 * Calculates the diff
	 */
	protected abstract List<DiffEdit> diff();

	/**
	 * Returns a new DiffEdit with DiffType of either {@link BasicDiffType#EQUAL} or {@link BasicDiffType#NORMALIZE} depending whether the specified text is equal or not
	 *
	 * <p>This can be used to easily create DiffEdit objects for equal text which may or may not have been normalized</p>
	 *
	 * @param leftLine (cannot be null)
	 * @param rightLine (cannot be null)
	 * @return
	 */
	protected DiffEdit newEqualOrNormalizeEdit(final DiffLine leftLine, final DiffLine rightLine) {
		Objects.requireNonNull(leftLine, "leftLine cannot be null");
		Objects.requireNonNull(rightLine, "rightLine cannot be null");

		DiffType diffType = leftLine.getText().equals(rightLine.getText())
				? BasicDiffType.EQUAL
				: BasicDiffType.NORMALIZE;

		return new DiffEdit(diffType, leftLine, rightLine);
	}
}
