/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package info.codesaway.eclipse.compare.internal;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.core.internal.expressions.util.LRUCache;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Implements the <code>IRangeComparator</code> interface for lines in a document.
 * A <code>DocLineComparator</code> is used as the input for the <code>RangeDifferencer</code>
 * engine to perform a line oriented compare on documents.
 * <p>
 * A <code>DocLineComparator</code> doesn't know anything about line separators because
 * its notion of lines is solely defined in the underlying <code>IDocument</code>.
 */
public class DocLineComparator implements ITokenComparator {

	private final IDocument fDocument;
	private int fLineOffset;
	private int fLineCount;
	private int fLength;
	private final boolean fIgnoreWhiteSpace;
	private final ICompareFilter[] fCompareFilters;
	private final char fContributor;
	private final LRUCache fCompareFilterCache;

	/**
	 * Creates a <code>DocLineComparator</code> for the given document range.
	 * ignoreWhiteSpace controls whether comparing lines (in method
	 * <code>rangesEqual</code>) should ignore whitespace.
	 *
	 * @param document the document from which the lines are taken
	 * @param region if non-<code>null</code> only lines within this range are taken
	 * @param ignoreWhiteSpace if <code>true</code> white space is ignored when comparing lines
	 */
	public DocLineComparator(final IDocument document, final IRegion region,
			final boolean ignoreWhiteSpace) {
		this(document, region, ignoreWhiteSpace, null, '?');
	}

	/**
	 * Creates a <code>DocLineComparator</code> for the given document range.
	 * ignoreWhiteSpace controls whether comparing lines (in method
	 * <code>rangesEqual</code>) should ignore whitespace. Compare filters may be used
	 * to affect the detection of line differences.
	 *
	 * @param document
	 *            the document from which the lines are taken
	 * @param region
	 *            if non-<code>null</code> only lines within this range are
	 *            taken
	 * @param ignoreWhiteSpace
	 *            if <code>true</code> white space is ignored when comparing
	 *            lines
	 * @param compareFilters
	 *            the active compare filters for the compare
	 * @param contributor
	 *            contributor of document
	 */
	public DocLineComparator(final IDocument document, final IRegion region,
			final boolean ignoreWhiteSpace, final ICompareFilter[] compareFilters,
			final char contributor) {
		this.fDocument = document;
		this.fIgnoreWhiteSpace = ignoreWhiteSpace;
		this.fCompareFilters = compareFilters;
		this.fContributor = contributor;

		boolean cacheFilteredLines = false;
		if (compareFilters != null && compareFilters.length > 0) {
			cacheFilteredLines = true;
			for (ICompareFilter compareFilter : compareFilters) {
				if (!compareFilter.canCacheFilteredRegions()) {
					cacheFilteredLines = false;
					break;
				}
			}
		}
		this.fCompareFilterCache = (cacheFilteredLines) ? new LRUCache(1024) : null;

		this.fLineOffset = 0;
		if (region != null) {
			this.fLength = region.getLength();
			int start = region.getOffset();
			try {
				this.fLineOffset = this.fDocument.getLineOfOffset(start);
			} catch (BadLocationException ex) {
				// silently ignored
			}

			if (this.fLength == 0) {
				// optimization, empty documents have one line
				this.fLineCount = 1;
			} else {
				int endLine = this.fDocument.getNumberOfLines();
				try {
					endLine = this.fDocument.getLineOfOffset(start + this.fLength);
				} catch (BadLocationException ex) {
					// silently ignored
				}
				this.fLineCount = endLine - this.fLineOffset + 1;
			}
		} else {
			this.fLength = document.getLength();
			this.fLineCount = this.fDocument.getNumberOfLines();
		}
	}

	/**
	 * Returns the number of lines in the document.
	 *
	 * @return number of lines
	 */
	@Override
	public int getRangeCount() {
		return this.fLineCount;
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenStart
	 */
	@Override
	public int getTokenStart(final int line) {
		try {
			IRegion r = this.fDocument.getLineInformation(this.fLineOffset + line);
			return r.getOffset();
		} catch (BadLocationException ex) {
			return this.fDocument.getLength();
		}
	}

	/* (non Javadoc)
	 * Returns the length of the given line.
	 * see ITokenComparator.getTokenLength
	 */
	@Override
	public int getTokenLength(final int line) {
		return this.getTokenStart(line + 1) - this.getTokenStart(line);
	}

	/**
	 * Returns <code>true</code> if a line given by the first index
	 * matches a line specified by the other <code>IRangeComparator</code> and index.
	 *
	 * @param thisIndex the number of the line within this range comparator
	 * @param otherComparator the range comparator to compare this with
	 * @param otherIndex the number of the line within the other comparator
	 * @return <code>true</code> if the lines are equal
	 */
	@Override
	public boolean rangesEqual(final int thisIndex, final IRangeComparator otherComparator, final int otherIndex) {

		if (otherComparator != null && otherComparator.getClass() == this.getClass()) {
			DocLineComparator other = (DocLineComparator) otherComparator;

			if (this.fIgnoreWhiteSpace) {
				String[] linesToCompare = this.extract(thisIndex, otherIndex, other, false);
				return this.compare(linesToCompare[0], linesToCompare[1]);
			}

			int tlen = this.getTokenLength(thisIndex);
			int olen = other.getTokenLength(otherIndex);
			if (tlen == olen) {
				String[] linesToCompare = this.extract(thisIndex, otherIndex, other, false);
				return linesToCompare[0].equals(linesToCompare[1]);
			} else if (this.fCompareFilters != null && this.fCompareFilters.length > 0) {
				String[] linesToCompare = this.extract(thisIndex, otherIndex, other, true);
				return linesToCompare[0].equals(linesToCompare[1]);
			}
		}
		return false;
	}

	/**
	 * Aborts the comparison if the number of tokens is too large.
	 *
	 * @param length a number on which to base the decision whether to return
	 * 	<code>true</code> or <code>false</code>
	 * @param maxLength another number on which to base the decision whether to return
	 *	<code>true</code> or <code>false</code>
	 * @param other the other <code>IRangeComparator</code> to compare with
	 * @return <code>true</code> to avoid a too lengthy range comparison
	 */
	@Override
	public boolean skipRangeComparison(final int length, final int maxLength, final IRangeComparator other) {
		return false;
	}

	//---- private methods

	private String[] extract(final int thisIndex, final int otherIndex,
			final DocLineComparator other, final boolean includeSeparator) {

		String[] extracts = new String[2];
		if (this.fCompareFilters != null && this.fCompareFilters.length > 0) {
			if (this.fCompareFilterCache != null
					&& other.fCompareFilterCache != null) {
				extracts[0] = (String) this.fCompareFilterCache.get(Integer.valueOf(
						thisIndex));
				if (extracts[0] == null) {
					extracts[0] = Utilities.applyCompareFilters(
							this.extract(thisIndex, includeSeparator), this.fContributor,
							other.extract(otherIndex, includeSeparator), other.fContributor,
							this.fCompareFilters);
					this.fCompareFilterCache
							.put(Integer.valueOf(thisIndex), extracts[0]);
				}

				extracts[1] = (String) other.fCompareFilterCache
						.get(Integer.valueOf(otherIndex));
				if (extracts[1] == null) {
					extracts[1] = Utilities.applyCompareFilters(
							other.extract(otherIndex, includeSeparator), other.fContributor,
							this.extract(thisIndex, includeSeparator), this.fContributor, this.fCompareFilters);
					other.fCompareFilterCache.put(Integer.valueOf(otherIndex),
							extracts[1]);
				}
			} else {
				String thisLine = this.extract(thisIndex, includeSeparator);
				String otherLine = other.extract(otherIndex, includeSeparator);
				extracts = new String[] {
						Utilities.applyCompareFilters(thisLine, this.fContributor,
								otherLine, other.fContributor, this.fCompareFilters),
						Utilities.applyCompareFilters(otherLine,
								other.fContributor, thisLine, this.fContributor,
								this.fCompareFilters) };
			}
		} else {
			extracts = new String[] { this.extract(thisIndex, includeSeparator),
					other.extract(otherIndex, includeSeparator) };
		}
		return extracts;
	}

	/**
	 * Extract a single line from the underlying document.
	 *
	 * @param line the number of the line to extract
	 * @param whether to include the line separator
	 * @return the contents of the line as a String
	 */
	// Make public so can use Patience diff
	public String extract(final int line, final boolean includeSeparator) {
		if (line < this.fLineCount) {
			try {
				if (includeSeparator) {
					return this.fDocument.get(this.fDocument.getLineOffset(line),
							this.fDocument.getLineLength(line));
				}

				IRegion r = this.fDocument.getLineInformation(this.fLineOffset + line);
				return this.fDocument.get(r.getOffset(), r.getLength());

			} catch (BadLocationException e) {
				// silently ignored
			}
		}
		return ""; //$NON-NLS-1$
	}

	private boolean compare(final String s1, final String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		int c1 = 0, c2 = 0;
		int i1 = 0, i2 = 0;

		while (c1 != -1) {

			c1 = -1;
			while (i1 < l1) {
				char c = s1.charAt(i1++);
				if (!Character.isWhitespace(c)) {
					c1 = c;
					break;
				}
			}

			c2 = -1;
			while (i2 < l2) {
				char c = s2.charAt(i2++);
				if (!Character.isWhitespace(c)) {
					c2 = c;
					break;
				}
			}

			if (c1 != c2) {
				return false;
			}
		}
		return true;
	}

	// Added to allow BEX to determine which normalization function to use (whitespace or no normalization)
	public boolean isIgnoreWhitespace() {
		return this.fIgnoreWhiteSpace;
	}

	public int getLineOffset() {
		return this.fLineOffset;
	}
}
