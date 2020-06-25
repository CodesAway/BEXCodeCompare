/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package info.codesaway.eclipse.compare.internal.merge;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.CompareContentViewerSwitchingPane;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import info.codesaway.eclipse.compare.internal.DocLineComparator;
import info.codesaway.eclipse.compare.rangedifferencer.RangeDifference;
import info.codesaway.eclipse.compare.rangedifferencer.RangeDifferencer;

/**
 * A document merger manages the differences between two documents
 * for either a 2-way or 3-way comparison.
 * <p>
 * This class should not have any UI dependencies.
 */
public class DocumentMerger {

	private static final String DIFF_RANGE_CATEGORY = CompareUIPlugin.PLUGIN_ID + ".DIFF_RANGE_CATEGORY"; //$NON-NLS-1$

	/** Selects between smartTokenDiff and mergingTokenDiff */
	private static final boolean USE_MERGING_TOKEN_DIFF = false;

	/** if true copying conflicts from one side to other concatenates both sides */
	private static final boolean APPEND_CONFLICT = true;

	/** All diffs for calculating scrolling position (includes line ranges without changes) */
	private ArrayList<Diff> fAllDiffs;
	/** Subset of above: just real differences. */
	private ArrayList<Diff> fChangeDiffs;

	private final IDocumentMergerInput fInput;

	/**
	 * Interface that defines that input to the document merge process
	 */
	public interface IDocumentMergerInput {

		IDocument getDocument(char contributor);

		Position getRegion(char contributor);

		boolean isIgnoreAncestor();

		boolean isThreeWay();

		CompareConfiguration getCompareConfiguration();

		ITokenComparator createTokenComparator(String s);

		boolean isHunkOnLeft();

		int getHunkStart();

		boolean isPatchHunk();

		boolean isShowPseudoConflicts();

		boolean isPatchHunkOk();
	}

	public class Diff {
		/** character range in ancestor document */
		Position fAncestorPos;
		/** character range in left document */
		Position fLeftPos;
		/** character range in right document */
		Position fRightPos;
		/** if this is a TokenDiff fParent points to the enclosing LineDiff */
		Diff fParent;
		/** if Diff has been resolved */
		boolean fResolved;
		int fDirection;
		boolean fIsToken = false;
		/** child token diffs */
		List<Diff> fDiffs;
		boolean fIsWhitespace = false;

		/*
		 * Create Diff from two ranges and an optional parent diff.
		 */
		Diff(final Diff parent, final int dir, final IDocument ancestorDoc, final Position aRange,
				final int ancestorStart, final int ancestorEnd,
				final IDocument leftDoc, final Position lRange, final int leftStart, final int leftEnd,
				final IDocument rightDoc, final Position rRange, final int rightStart, final int rightEnd) {
			// System.out.println(getMethodName());
			this.fParent = parent != null ? parent : this;
			this.fDirection = dir;

			this.fLeftPos = this.createPosition(leftDoc, lRange, leftStart, leftEnd);
			this.fRightPos = this.createPosition(rightDoc, rRange, rightStart, rightEnd);
			if (ancestorDoc != null) {
				this.fAncestorPos = this.createPosition(ancestorDoc, aRange, ancestorStart, ancestorEnd);
			}
		}

		public Position getPosition(final char type) {
			// System.out.println(getMethodName());
			switch (type) {
			case MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR:
				return this.fAncestorPos;
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				return this.fLeftPos;
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				return this.fRightPos;
			}
			return null;
		}

		boolean isInRange(final char type, final int pos) {
			// System.out.println(getMethodName());
			Position p = this.getPosition(type);
			return (pos >= p.offset) && (pos < (p.offset + p.length));
		}

		public String changeType() {
			// System.out.println(getMethodName());
			boolean leftEmpty = this.fLeftPos.length == 0;
			boolean rightEmpty = this.fRightPos.length == 0;

			if (this.fDirection == RangeDifference.LEFT) {
				if (!leftEmpty && rightEmpty) {
					return CompareMessages.TextMergeViewer_changeType_addition;
				}
				if (leftEmpty && !rightEmpty) {
					return CompareMessages.TextMergeViewer_changeType_deletion;
				}
			} else {
				if (leftEmpty && !rightEmpty) {
					return CompareMessages.TextMergeViewer_changeType_addition;
				}
				if (!leftEmpty && rightEmpty) {
					return CompareMessages.TextMergeViewer_changeType_deletion;
				}
			}
			return CompareMessages.TextMergeViewer_changeType_change;
		}

		public Image getImage() {
			// System.out.println(getMethodName());
			int code = Differencer.CHANGE;
			switch (this.fDirection) {
			case RangeDifference.RIGHT:
				code += DocumentMerger.this.getCompareConfiguration().isMirrored() ? Differencer.RIGHT
						: Differencer.LEFT;
				break;
			case RangeDifference.LEFT:
				code += DocumentMerger.this.getCompareConfiguration().isMirrored() ? Differencer.LEFT
						: Differencer.RIGHT;
				break;
			case RangeDifference.ANCESTOR:
			case RangeDifference.CONFLICT:
				code += Differencer.CONFLICTING;
				break;
			}
			if (code != 0) {
				return DocumentMerger.this.getCompareConfiguration().getImage(code);
			}
			return null;
		}

		Position createPosition(final IDocument doc, final Position range, final int start, final int end) {
			// System.out.println(getMethodName());
			try {
				int l = end - start;
				if (range != null) {
					int dl = range.length;
					if (l > dl) {
						l = dl;
					}
				} else {
					int dl = doc.getLength();
					if (start + l > dl) {
						l = dl - start;
					}
				}

				Position p = null;
				try {
					p = new Position(start, l);
				} catch (RuntimeException ex) {
					p = new Position(0, 0);
				}

				try {
					doc.addPosition(DIFF_RANGE_CATEGORY, p);
				} catch (BadPositionCategoryException ex) {
					// silently ignored
				}
				return p;
			} catch (BadLocationException ee) {
				// silently ignored
			}
			return null;
		}

		void add(final Diff d) {
			// System.out.println(getMethodName());
			if (this.fDiffs == null) {
				this.fDiffs = new ArrayList<>();
			}
			this.fDiffs.add(d);
		}

		public boolean isDeleted() {
			// System.out.println(getMethodName());
			if (this.fAncestorPos != null && this.fAncestorPos.isDeleted()) {
				return true;
			}
			return this.fLeftPos.isDeleted() || this.fRightPos.isDeleted();
		}

		void setResolved(final boolean r) {
			// System.out.println(getMethodName());
			this.fResolved = r;
			if (r) {
				this.fDiffs = null;
			}
		}

		public boolean isResolved() {
			// System.out.println(getMethodName());
			if (!this.fResolved && this.fDiffs != null) {
				Iterator<Diff> e = this.fDiffs.iterator();
				while (e.hasNext()) {
					Diff d = e.next();
					if (!d.isResolved()) {
						return false;
					}
				}
				return true;
			}
			return this.fResolved;
		}

		Position getPosition(final int contributor) {
			// System.out.println(getMethodName());
			if (contributor == MergeViewerContentProvider.LEFT_CONTRIBUTOR) {
				return this.fLeftPos;
			}
			if (contributor == MergeViewerContentProvider.RIGHT_CONTRIBUTOR) {
				return this.fRightPos;
			}
			if (contributor == MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR) {
				return this.fAncestorPos;
			}
			return null;
		}

		/*
		 * Returns true if given character range overlaps with this Diff.
		 */
		public boolean overlaps(final int contributor, final int start, final int end, final int docLength) {
			// System.out.println(getMethodName());
			Position h = this.getPosition(contributor);
			if (h != null) {
				int ds = h.getOffset();
				int de = ds + h.getLength();
				if ((start < de) && (end >= ds)) {
					return true;
				}
				if ((start == docLength) && (start <= de) && (end >= ds)) {
					return true;
				}
			}
			return false;
		}

		public int getMaxDiffHeight() {
			// System.out.println(getMethodName());
			Point region = new Point(0, 0);
			int h = DocumentMerger.this.getLineRange(
					DocumentMerger.this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR), this.fLeftPos,
					region).y;
			if (DocumentMerger.this.isThreeWay()) {
				h = Math.max(h,
						DocumentMerger.this.getLineRange(
								DocumentMerger.this.getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR),
								this.fAncestorPos, region).y);
			}
			return Math.max(h,
					DocumentMerger.this.getLineRange(
							DocumentMerger.this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR),
							this.fRightPos, region).y);
		}

		public int getAncestorHeight() {
			// System.out.println(getMethodName());
			Point region = new Point(0, 0);
			return DocumentMerger.this.getLineRange(
					DocumentMerger.this.getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR), this.fAncestorPos,
					region).y;
		}

		public int getLeftHeight() {
			// System.out.println(getMethodName());
			Point region = new Point(0, 0);
			return DocumentMerger.this.getLineRange(
					DocumentMerger.this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR), this.fLeftPos,
					region).y;
		}

		public int getRightHeight() {
			// System.out.println(getMethodName());
			Point region = new Point(0, 0);
			return DocumentMerger.this.getLineRange(
					DocumentMerger.this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR), this.fRightPos,
					region).y;
		}

		public Diff[] getChangeDiffs(final int contributor, final IRegion region) {
			// System.out.println(getMethodName());
			if (this.fDiffs != null && this.intersectsRegion(contributor, region)) {
				List<Diff> result = new ArrayList<>();
				for (Diff diff : this.fDiffs) {
					if (diff.intersectsRegion(contributor, region)) {
						result.add(diff);
					}
				}
				return result.toArray(new Diff[result.size()]);
			}
			return new Diff[0];
		}

		private boolean intersectsRegion(final int contributor, final IRegion region) {
			// System.out.println(getMethodName());
			Position p = this.getPosition(contributor);
			if (p != null) {
				return p.overlapsWith(region.getOffset(), region.getLength());
			}
			return false;
		}

		public boolean hasChildren() {
			// System.out.println(getMethodName());
			return this.fDiffs != null && !this.fDiffs.isEmpty();
		}

		public int getKind() {
			// System.out.println(getMethodName());
			return this.fDirection;
		}

		public boolean isToken() {
			// System.out.println(getMethodName());
			return this.fIsToken;
		}

		public Diff getParent() {
			// System.out.println(getMethodName());
			return this.fParent;
		}

		public Iterator<Diff> childIterator() {
			// System.out.println(getMethodName());
			if (this.fDiffs == null) {
				return new ArrayList<Diff>().iterator();
			}
			return this.fDiffs.iterator();
		}
	}

	public DocumentMerger(final IDocumentMergerInput input) {
		// System.out.println(getMethodName());
		this.fInput = input;
	}

	/**
	 * Perform a two level 2- or 3-way diff.
	 * The first level is based on line comparison, the second level on token comparison.
	 * @throws CoreException
	 */
	public void doDiff() throws CoreException {
		// System.out.println(getMethodName());

		this.fChangeDiffs = new ArrayList<>();
		IDocument lDoc = this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		IDocument rDoc = this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);

		if (lDoc == null || rDoc == null) {
			return;
		}

		Position lRegion = this.getRegion(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		Position rRegion = this.getRegion(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);

		IDocument aDoc = null;
		Position aRegion = null;
		if (this.isThreeWay() && !this.isIgnoreAncestor()) {
			aDoc = this.getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
			aRegion = this.getRegion(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
		}

		this.resetPositions(lDoc);
		this.resetPositions(rDoc);
		this.resetPositions(aDoc);

		boolean ignoreWhiteSpace = this.isIgnoreWhitespace();
		ICompareFilter[] compareFilters = this.getCompareFilters();

		DocLineComparator sright = new DocLineComparator(rDoc,
				toRegion(rRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		DocLineComparator sleft = new DocLineComparator(lDoc,
				toRegion(lRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		DocLineComparator sancestor = null;
		if (aDoc != null) {
			sancestor = new DocLineComparator(aDoc, toRegion(aRegion),
					ignoreWhiteSpace, compareFilters,
					MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
			/*if (isPatchHunk()) {
				if (isHunkOnLeft()) {
					sright= new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace);
				} else {
					sleft= new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace);
				}
			}*/
		}

		final Object[] result = new Object[1];
		final DocLineComparator sa = sancestor, sl = sleft, sr = sright;
		IRunnableWithProgress runnable = monitor -> {
			monitor.beginTask(CompareMessages.DocumentMerger_0, maxWork(sa, sl, sr));
			try {
				// Needed for BEX code compare, so can correctly indicate INSERT versus DELETE
				boolean isMirrored = this.getCompareConfiguration().isMirrored();

				result[0] = RangeDifferencer.findRanges(monitor, sa, sl, sr, isMirrored);
			} catch (OutOfMemoryError ex) {
				System.gc();
				throw new InvocationTargetException(ex);
			}
			if (monitor.isCanceled()) { // canceled
				throw new InterruptedException();
			}
			monitor.done();
		};

		RangeDifference[] e = null;
		try {
			this.getCompareConfiguration().getContainer().run(true, true, runnable);
			e = (RangeDifference[]) result[0];
		} catch (InvocationTargetException ex) {
			// we create a NOCHANGE range for the whole document
			Diff diff = new Diff(null, RangeDifference.NOCHANGE,
					aDoc, aRegion, 0, aDoc != null ? aDoc.getLength() : 0,
					lDoc, lRegion, 0, lDoc.getLength(),
					rDoc, rRegion, 0, rDoc.getLength());

			this.fAllDiffs = new ArrayList<>();
			this.fAllDiffs.add(diff);
			throw new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0,
					CompareMessages.DocumentMerger_1, ex.getTargetException()));
		} catch (InterruptedException ex) {
			// we create a NOCHANGE range for the whole document
			Diff diff = new Diff(null, RangeDifference.NOCHANGE,
					aDoc, aRegion, 0, aDoc != null ? aDoc.getLength() : 0,
					lDoc, lRegion, 0, lDoc.getLength(),
					rDoc, rRegion, 0, rDoc.getLength());

			this.fAllDiffs = new ArrayList<>();
			this.fAllDiffs.add(diff);
			return;
		}

		if (this.isCapped(sa, sl, sr)) {
			this.fInput.getCompareConfiguration()
					.setProperty(
							CompareContentViewerSwitchingPane.OPTIMIZED_ALGORITHM_USED,
							Boolean.TRUE);
		} else {
			this.fInput.getCompareConfiguration()
					.setProperty(
							CompareContentViewerSwitchingPane.OPTIMIZED_ALGORITHM_USED,
							Boolean.FALSE);
		}

		ArrayList<Diff> newAllDiffs = new ArrayList<>();
		for (RangeDifference es : e) {
			int ancestorStart = 0;
			int ancestorEnd = 0;
			if (sancestor != null) {
				ancestorStart = sancestor.getTokenStart(es.ancestorStart());
				ancestorEnd = getTokenEnd2(sancestor, es.ancestorStart(), es.ancestorLength());
			}

			int leftStart = sleft.getTokenStart(es.leftStart());
			int leftEnd = getTokenEnd2(sleft, es.leftStart(), es.leftLength());

			int rightStart = sright.getTokenStart(es.rightStart());
			int rightEnd = getTokenEnd2(sright, es.rightStart(), es.rightLength());

			/*if (isPatchHunk()) {
				if (isHunkOnLeft()) {
					rightStart = rightEnd = getHunkStart();
				} else {
					leftStart = leftEnd = getHunkStart();
				}
			}*/

			Diff diff = new Diff(null, es.kind(),
					aDoc, aRegion, ancestorStart, ancestorEnd,
					lDoc, lRegion, leftStart, leftEnd,
					rDoc, rRegion, rightStart, rightEnd);

			newAllDiffs.add(diff); // remember all range diffs for scrolling

			if (this.isPatchHunk()) {
				if (this.useChange(diff)) {
					this.recordChangeDiff(diff);
				}
			} else {
				if (ignoreWhiteSpace || this.useChange(es.kind())) {

					// Extract the string for each contributor.
					String a = null;
					if (sancestor != null) {
						a = this.extract2(aDoc, sancestor, es.ancestorStart(), es.ancestorLength());
					}
					String s = this.extract2(lDoc, sleft, es.leftStart(), es.leftLength());
					String d = this.extract2(rDoc, sright, es.rightStart(), es.rightLength());

					// Indicate whether all contributors are whitespace
					if (ignoreWhiteSpace
							&& (a == null || a.trim().length() == 0)
							&& s.trim().length() == 0
							&& d.trim().length() == 0) {
						diff.fIsWhitespace = true;
					}

					// If the diff is of interest, record it and generate the token diffs
					if (this.useChange(diff)) {
						this.recordChangeDiff(diff);
						if (s.length() > 0 && d.length() > 0) {
							if (a == null && sancestor != null) {
								a = this.extract2(aDoc, sancestor, es.ancestorStart(), es.ancestorLength());
							}
							if (USE_MERGING_TOKEN_DIFF) {
								this.mergingTokenDiff(diff, aDoc, a, rDoc, d, lDoc, s);
							} else {
								this.simpleTokenDiff(diff, aDoc, a, rDoc, d, lDoc, s);
							}
						}
					}
				}
			}
		}
		this.fAllDiffs = newAllDiffs;
	}

	private boolean isCapped(final DocLineComparator ancestor,
			final DocLineComparator left, final DocLineComparator right) {
		// System.out.println(getMethodName());
		//		if (this.isCappingDisabled()) {
		//			return false;
		//		}
		//		int aLength = ancestor == null ? 0 : ancestor.getRangeCount();
		//		int lLength = left.getRangeCount();
		//		int rLength = right.getRangeCount();
		//		if ((double) aLength * (double) lLength > LCS.TOO_LONG
		//				|| (double) aLength * (double) rLength > LCS.TOO_LONG
		//				|| (double) lLength * (double) rLength > LCS.TOO_LONG) {
		//			return true;
		//		}
		// Since using BEX to compare, don't need to worry about cap
		return false;
	}

	public Diff findDiff(final char type, final int pos) throws CoreException {
		// System.out.println(getMethodName());

		IDocument aDoc = null;
		IDocument lDoc = this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		IDocument rDoc = this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		if (lDoc == null || rDoc == null) {
			return null;
		}

		Position aRegion = null;
		Position lRegion = null;
		Position rRegion = null;

		boolean threeWay = this.isThreeWay();

		if (threeWay && !this.isIgnoreAncestor()) {
			aDoc = this.getDocument(MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
		}

		boolean ignoreWhiteSpace = this.isIgnoreWhitespace();
		ICompareFilter[] compareFilters = this.getCompareFilters();

		DocLineComparator sright = new DocLineComparator(rDoc, toRegion(rRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		DocLineComparator sleft = new DocLineComparator(lDoc, toRegion(lRegion), ignoreWhiteSpace, compareFilters,
				MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		DocLineComparator sancestor = null;
		if (aDoc != null) {
			sancestor = new DocLineComparator(aDoc, toRegion(aRegion), ignoreWhiteSpace, compareFilters,
					MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR);
		}

		final Object[] result = new Object[1];
		final DocLineComparator sa = sancestor, sl = sleft, sr = sright;
		IRunnableWithProgress runnable = monitor -> {
			monitor.beginTask(CompareMessages.DocumentMerger_2, maxWork(sa, sl, sr));
			try {
				boolean isMirrored = this.getCompareConfiguration().isMirrored();
				result[0] = RangeDifferencer.findRanges(monitor, sa, sl, sr, isMirrored);
			} catch (OutOfMemoryError ex) {
				System.gc();
				throw new InvocationTargetException(ex);
			}
			if (monitor.isCanceled()) { // canceled
				throw new InterruptedException();
			}
			monitor.done();
		};

		RangeDifference[] e = null;
		try {
			Utilities.executeRunnable(runnable);
			e = (RangeDifference[]) result[0];
		} catch (InvocationTargetException ex) {
			throw new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0,
					CompareMessages.DocumentMerger_3, ex.getTargetException()));
		} catch (InterruptedException ex) {
			//
		}

		if (e != null) {
			for (RangeDifference es : e) {
				int kind = es.kind();

				int ancestorStart = 0;
				int ancestorEnd = 0;
				if (sancestor != null) {
					ancestorStart = sancestor.getTokenStart(es.ancestorStart());
					ancestorEnd = getTokenEnd2(sancestor, es.ancestorStart(), es.ancestorLength());
				}

				int leftStart = sleft.getTokenStart(es.leftStart());
				int leftEnd = getTokenEnd2(sleft, es.leftStart(), es.leftLength());

				int rightStart = sright.getTokenStart(es.rightStart());
				int rightEnd = getTokenEnd2(sright, es.rightStart(), es.rightLength());

				Diff diff = new Diff(null, kind,
						aDoc, aRegion, ancestorStart, ancestorEnd,
						lDoc, lRegion, leftStart, leftEnd,
						rDoc, rRegion, rightStart, rightEnd);

				if (diff.isInRange(type, pos)) {
					return diff;
				}
			}
		}

		return null;
	}

	private void recordChangeDiff(final Diff diff) {
		// System.out.println(getMethodName());
		this.fChangeDiffs.add(diff); // here we remember only the real diffs
	}

	/*private boolean isHunkOnLeft() {
		return fInput.isHunkOnLeft();
	}
	
	private int getHunkStart() {
		return fInput.getHunkStart();
	}*/

	private boolean isPatchHunk() {
		// System.out.println(getMethodName());
		return this.fInput.isPatchHunk();
	}

	private boolean isIgnoreWhitespace() {
		// System.out.println(getMethodName());
		return Utilities.getBoolean(this.getCompareConfiguration(), CompareConfiguration.IGNORE_WHITESPACE, false);
	}

	private ICompareFilter[] getCompareFilters() {
		// System.out.println(getMethodName());
		return Utilities.getCompareFilters(this.getCompareConfiguration());
	}

	private boolean isCappingDisabled() {
		// System.out.println(getMethodName());
		return CompareUIPlugin.getDefault().getPreferenceStore().getBoolean(ComparePreferencePage.CAPPING_DISABLED);
	}

	private IDocument getDocument(final char contributor) {
		// System.out.println(getMethodName());
		return this.fInput.getDocument(contributor);
	}

	private Position getRegion(final char contributor) {
		// System.out.println(getMethodName());
		return this.fInput.getRegion(contributor);
	}

	public boolean isIgnoreAncestor() {
		// System.out.println(getMethodName());
		return this.fInput.isIgnoreAncestor();
	}

	public boolean isThreeWay() {
		// System.out.println(getMethodName());
		return this.fInput.isThreeWay();
	}

	/**
	 * Return the compare configuration associated with this merger.
	 * @return the compare configuration associated with this merger
	 */
	public CompareConfiguration getCompareConfiguration() {
		// System.out.println(getMethodName());
		return this.fInput.getCompareConfiguration();
	}

	/*
	 * Returns true if kind of change should be shown.
	 */
	public boolean useChange(final Diff diff) {
		// System.out.println(getMethodName());
		if (diff.fIsWhitespace) {
			return false;
		}
		int kind = diff.getKind();
		return this.useChange(kind);
	}

	private boolean useChange(final int kind) {
		// System.out.println(getMethodName());
		if (kind == RangeDifference.NOCHANGE) {
			return false;
		}
		if (this.fInput.getCompareConfiguration().isChangeIgnored(kind)) {
			return false;
		}
		if (kind == RangeDifference.ANCESTOR) {
			return this.fInput.isShowPseudoConflicts();
		}
		return true;
	}

	private int getTokenEnd(final ITokenComparator tc, final int start, final int count) {
		// System.out.println(getMethodName());
		if (count <= 0) {
			return tc.getTokenStart(start);
		}
		int index = start + count - 1;
		return tc.getTokenStart(index) + tc.getTokenLength(index);
	}

	private static int getTokenEnd2(final ITokenComparator tc, final int start, final int length) {
		// System.out.println(getMethodName());
		return tc.getTokenStart(start + length);
	}

	/**
	 * Returns the content of lines in the specified range as a String.
	 * This includes the line separators.
	 *
	 * @param doc the document from which to extract the characters
	 * @param start index of first line
	 * @param length number of lines
	 * @return the contents of the specified line range as a String
	 */
	private String extract2(final IDocument doc, final ITokenComparator tc, final int start, final int length) {
		// System.out.println(getMethodName());
		int count = tc.getRangeCount();
		if (length > 0 && count > 0) {

			//
			//			int startPos= tc.getTokenStart(start);
			//			int endPos= startPos;
			//
			//			if (length > 1)
			//				endPos= tc.getTokenStart(start + (length-1));
			//			endPos+= tc.getTokenLength(start + (length-1));
			//

			int startPos = tc.getTokenStart(start);
			int endPos;

			if (length == 1) {
				endPos = startPos + tc.getTokenLength(start);
			} else {
				endPos = tc.getTokenStart(start + length);
			}

			try {
				return doc.get(startPos, endPos - startPos);
			} catch (BadLocationException e) {
				// silently ignored
			}

		}
		return ""; //$NON-NLS-1$
	}

	private static IRegion toRegion(final Position position) {
		// System.out.println(getMethodName());
		if (position != null) {
			return new Region(position.getOffset(), position.getLength());
		}
		return null;
	}

	/*
	 * Performs a "smart" token based 3-way diff on the character range specified by the given baseDiff.
	 * It is "smart" because it tries to minimize the number of token diffs by merging them.
	 */
	private void mergingTokenDiff(final Diff baseDiff,
			final IDocument ancestorDoc, final String a,
			final IDocument rightDoc, final String d,
			final IDocument leftDoc, final String s) {
		// System.out.println(getMethodName());
		ITokenComparator sa = null;
		int ancestorStart = 0;
		if (ancestorDoc != null) {
			sa = this.createTokenComparator(a);
			ancestorStart = baseDiff.fAncestorPos.getOffset();
		}

		int rightStart = baseDiff.fRightPos.getOffset();
		ITokenComparator sm = this.createTokenComparator(d);

		int leftStart = baseDiff.fLeftPos.getOffset();
		ITokenComparator sy = this.createTokenComparator(s);

		boolean isMirrored = this.getCompareConfiguration().isMirrored();
		RangeDifference[] r = RangeDifferencer.findRanges(sa, sy, sm, isMirrored);
		for (int i = 0; i < r.length; i++) {
			RangeDifference es = r[i];
			// determine range of diffs in one line
			int start = i;
			int leftLine = -1;
			int rightLine = -1;
			try {
				leftLine = leftDoc.getLineOfOffset(leftStart + sy.getTokenStart(es.leftStart()));
				rightLine = rightDoc.getLineOfOffset(rightStart + sm.getTokenStart(es.rightStart()));
			} catch (BadLocationException e) {
				// silently ignored
			}
			i++;
			for (; i < r.length; i++) {
				es = r[i];
				try {
					if (leftLine != leftDoc.getLineOfOffset(leftStart + sy.getTokenStart(es.leftStart()))) {
						break;
					}
					if (rightLine != rightDoc.getLineOfOffset(rightStart + sm.getTokenStart(es.rightStart()))) {
						break;
					}
				} catch (BadLocationException e) {
					// silently ignored
				}
			}
			int end = i;

			// find first diff from left
			RangeDifference first = null;
			for (int ii = start; ii < end; ii++) {
				es = r[ii];
				if (this.useChange(es.kind())) {
					first = es;
					break;
				}
			}

			// find first diff from mine
			RangeDifference last = null;
			for (int ii = end - 1; ii >= start; ii--) {
				es = r[ii];
				if (this.useChange(es.kind())) {
					last = es;
					break;
				}
			}

			if (first != null && last != null) {

				int ancestorStart2 = 0;
				int ancestorEnd2 = 0;
				if (ancestorDoc != null) {
					Objects.requireNonNull(sa);
					ancestorStart2 = ancestorStart + sa.getTokenStart(first.ancestorStart());
					ancestorEnd2 = ancestorStart + this.getTokenEnd(sa, last.ancestorStart(), last.ancestorLength());
				}

				int leftStart2 = leftStart + sy.getTokenStart(first.leftStart());
				int leftEnd2 = leftStart + this.getTokenEnd(sy, last.leftStart(), last.leftLength());

				int rightStart2 = rightStart + sm.getTokenStart(first.rightStart());
				int rightEnd2 = rightStart + this.getTokenEnd(sm, last.rightStart(), last.rightLength());
				Diff diff = new Diff(baseDiff, first.kind(),
						ancestorDoc, null, ancestorStart2, ancestorEnd2,
						leftDoc, null, leftStart2, leftEnd2,
						rightDoc, null, rightStart2, rightEnd2);
				diff.fIsToken = true;
				baseDiff.add(diff);
			}
		}
	}

	/*
	 * Performs a token based 3-way diff on the character range specified by the given baseDiff.
	 */
	private void simpleTokenDiff(final Diff baseDiff,
			final IDocument ancestorDoc, final String a,
			final IDocument rightDoc, final String d,
			final IDocument leftDoc, final String s) {
		// System.out.println(getMethodName());

		int ancestorStart = 0;
		ITokenComparator sa = null;
		if (ancestorDoc != null) {
			ancestorStart = baseDiff.fAncestorPos.getOffset();
			sa = this.createTokenComparator(a);
		}

		int rightStart = baseDiff.fRightPos.getOffset();
		ITokenComparator sm = this.createTokenComparator(d);

		int leftStart = baseDiff.fLeftPos.getOffset();
		ITokenComparator sy = this.createTokenComparator(s);

		boolean isMirrored = this.getCompareConfiguration().isMirrored();
		RangeDifference[] e = RangeDifferencer.findRanges(sa, sy, sm, isMirrored);
		for (RangeDifference es : e) {
			int kind = es.kind();
			if (kind != RangeDifference.NOCHANGE) {

				int ancestorStart2 = ancestorStart;
				int ancestorEnd2 = ancestorStart;
				if (ancestorDoc != null) {
					Objects.requireNonNull(sa);
					ancestorStart2 += sa.getTokenStart(es.ancestorStart());
					ancestorEnd2 += this.getTokenEnd(sa, es.ancestorStart(), es.ancestorLength());
				}

				int leftStart2 = leftStart + sy.getTokenStart(es.leftStart());
				int leftEnd2 = leftStart + this.getTokenEnd(sy, es.leftStart(), es.leftLength());

				int rightStart2 = rightStart + sm.getTokenStart(es.rightStart());
				int rightEnd2 = rightStart + this.getTokenEnd(sm, es.rightStart(), es.rightLength());

				Diff diff = new Diff(baseDiff, kind,
						ancestorDoc, null, ancestorStart2, ancestorEnd2,
						leftDoc, null, leftStart2, leftEnd2,
						rightDoc, null, rightStart2, rightEnd2);

				// ensure that token diff is smaller than basediff
				int leftS = baseDiff.fLeftPos.offset;
				int leftE = baseDiff.fLeftPos.offset + baseDiff.fLeftPos.length;
				int rightS = baseDiff.fRightPos.offset;
				int rightE = baseDiff.fRightPos.offset + baseDiff.fRightPos.length;
				if (leftS != leftStart2 || leftE != leftEnd2 ||
						rightS != rightStart2 || rightE != rightEnd2) {
					diff.fIsToken = true;
					// add to base Diff
					baseDiff.add(diff);
				}
			}
		}
	}

	private ITokenComparator createTokenComparator(final String s) {
		// System.out.println(getMethodName());
		return this.fInput.createTokenComparator(s);
	}

	private static int maxWork(final IRangeComparator a, final IRangeComparator l, final IRangeComparator r) {
		// System.out.println(getMethodName());
		int ln = l.getRangeCount();
		int rn = r.getRangeCount();
		if (a != null) {
			int an = a.getRangeCount();
			return (2 * Math.max(an, ln)) + (2 * Math.max(an, rn));
		}
		return 2 * Math.max(ln, rn);
	}

	private void resetPositions(final IDocument doc) {
		// System.out.println(getMethodName());
		if (doc == null) {
			return;
		}
		try {
			doc.removePositionCategory(DIFF_RANGE_CATEGORY);
		} catch (BadPositionCategoryException e) {
			// Ignore
		}
		doc.addPositionCategory(DIFF_RANGE_CATEGORY);
	}

	/*
	 * Returns the start line and the number of lines which correspond to the given position.
	 * Starting line number is 0 based.
	 */
	protected Point getLineRange(final IDocument doc, final Position p, final Point region) {
		// System.out.println(getMethodName());

		if (p == null || doc == null) {
			region.x = 0;
			region.y = 0;
			return region;
		}

		int start = p.getOffset();
		int length = p.getLength();

		int startLine = 0;
		try {
			startLine = doc.getLineOfOffset(start);
		} catch (BadLocationException e) {
			// silently ignored
		}

		int lineCount = 0;

		if (length == 0) {
			//			// if range length is 0 and if range starts a new line
			//			try {
			//				if (start == doc.getLineStartOffset(startLine)) {
			//					lines--;
			//				}
			//			} catch (BadLocationException e) {
			//				lines--;
			//			}

		} else {
			int endLine = 0;
			try {
				endLine = doc.getLineOfOffset(start + length - 1); // why -1?
			} catch (BadLocationException e) {
				// silently ignored
			}
			lineCount = endLine - startLine + 1;
		}

		region.x = startLine;
		region.y = lineCount;
		return region;
	}

	public Diff findDiff(final Position p, final boolean left) {
		// System.out.println(getMethodName());
		for (Diff diff : this.fAllDiffs) {
			Position diffPos;
			if (left) {
				diffPos = diff.fLeftPos;
			} else {
				diffPos = diff.fRightPos;
			}
			// If the element falls within a diff, highlight that diff
			if (diffPos.offset + diffPos.length >= p.offset && diff.fDirection != RangeDifference.NOCHANGE) {
				return diff;
			}
			// Otherwise, highlight the first diff after the elements position
			if (diffPos.offset >= p.offset) {
				return diff;
			}
		}
		return null;
	}

	public void reset() {
		// System.out.println(getMethodName());
		this.fChangeDiffs = null;
		this.fAllDiffs = null;
	}

	/**
	 * Returns the virtual position for the given view position.
	 * @param contributor
	 * @param vpos
	 * @return the virtual position for the given view position
	 */
	public int realToVirtualPosition(final char contributor, int vpos) {
		// System.out.println(getMethodName());

		if (this.fAllDiffs == null) {
			return vpos;
		}

		int viewPos = 0; // real view position
		int virtualPos = 0; // virtual position
		Point region = new Point(0, 0);

		Iterator<Diff> e = this.fAllDiffs.iterator();
		while (e.hasNext()) {
			Diff diff = e.next();
			Position pos = diff.getPosition(contributor);
			this.getLineRange(this.getDocument(contributor), pos, region);
			int realHeight = region.y;
			int virtualHeight = diff.getMaxDiffHeight();
			if (vpos <= viewPos + realHeight) { // OK, found!
				vpos -= viewPos; // make relative to this slot
				// now scale position within this slot to virtual slot
				if (realHeight <= 0) {
					vpos = 0;
				} else {
					vpos = (vpos * virtualHeight) / realHeight;
				}
				return virtualPos + vpos;
			}
			viewPos += realHeight;
			virtualPos += virtualHeight;
		}
		return virtualPos;
	}

	/**
	 * maps given virtual position into a real view position of this view.
	 * @param contributor
	 * @param v
	 * @return the real view position
	 */
	public int virtualToRealPosition(final char contributor, int v) {
		// System.out.println(getMethodName());

		if (this.fAllDiffs == null) {
			return v;
		}

		int virtualPos = 0;
		int viewPos = 0;
		Point region = new Point(0, 0);

		Iterator<Diff> e = this.fAllDiffs.iterator();
		while (e.hasNext()) {
			Diff diff = e.next();
			Position pos = diff.getPosition(contributor);
			int viewHeight = this.getLineRange(this.getDocument(contributor), pos, region).y;
			int virtualHeight = diff.getMaxDiffHeight();
			if (v < (virtualPos + virtualHeight)) {
				v -= virtualPos; // make relative to this slot
				if (viewHeight <= 0) {
					v = 0;
				} else {
					v = (int) (v * ((double) viewHeight / virtualHeight));
				}
				return viewPos + v;
			}
			virtualPos += virtualHeight;
			viewPos += viewHeight;
		}
		return viewPos;
	}

	/*
	 * Calculates virtual height (in lines) of views by adding the maximum of corresponding diffs.
	 */
	public int getVirtualHeight() {
		// System.out.println(getMethodName());
		int h = 1;
		if (this.fAllDiffs != null) {
			Iterator<Diff> e = this.fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff = e.next();
				h += diff.getMaxDiffHeight();
			}
		}
		return h;
	}

	/*
	 * Calculates height (in lines) of right view by adding the height of the right diffs.
	 */
	public int getRightHeight() {
		// System.out.println(getMethodName());
		int h = 1;
		if (this.fAllDiffs != null) {
			Iterator<Diff> e = this.fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff = e.next();
				h += diff.getRightHeight();
			}
		}
		return h;
	}

	public int findInsertionPoint(final Diff diff, final char type) {
		// System.out.println(getMethodName());
		if (diff != null) {
			switch (type) {
			case MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR:
				if (diff.fAncestorPos != null) {
					return diff.fAncestorPos.offset;
				}
				break;
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				if (diff.fLeftPos != null) {
					return diff.fLeftPos.offset;
				}
				break;
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				if (diff.fRightPos != null) {
					return diff.fRightPos.offset;
				}
				break;
			}
		}
		return 0;
	}

	public Diff[] getChangeDiffs(final char contributor, final IRegion region) {
		// System.out.println(getMethodName());
		if (this.fChangeDiffs == null) {
			return new Diff[0];
		}
		List<Diff> intersectingDiffs = new ArrayList<>();
		for (Diff diff : this.fChangeDiffs) {
			Diff[] changeDiffs = diff.getChangeDiffs(contributor, region);
			Collections.addAll(intersectingDiffs, changeDiffs);
		}
		return intersectingDiffs.toArray(new Diff[intersectingDiffs.size()]);
	}

	public Diff findDiff(final int viewportHeight, final boolean synchronizedScrolling, final Point size,
			final int my) {
		// System.out.println(getMethodName());
		int virtualHeight = synchronizedScrolling ? this.getVirtualHeight() : this.getRightHeight();
		if (virtualHeight < viewportHeight) {
			return null;
		}

		int yy, hh;
		int y = 0;
		if (this.fAllDiffs != null) {
			Iterator<Diff> e = this.fAllDiffs.iterator();
			while (e.hasNext()) {
				Diff diff = e.next();
				int h = synchronizedScrolling ? diff.getMaxDiffHeight()
						: diff.getRightHeight();
				if (this.useChange(diff.getKind()) && !diff.fIsWhitespace) {

					yy = (y * size.y) / virtualHeight;
					hh = (h * size.y) / virtualHeight;
					if (hh < 3) {
						hh = 3;
					}

					if (my >= yy && my < yy + hh) {
						return diff;
					}
				}
				y += h;
			}
		}
		return null;
	}

	public boolean hasChanges() {
		// System.out.println(getMethodName());
		return this.fChangeDiffs != null && !this.fChangeDiffs.isEmpty();
	}

	public Iterator<Diff> changesIterator() {
		// System.out.println(getMethodName());
		if (this.fChangeDiffs == null) {
			return new ArrayList<Diff>().iterator();
		}
		return this.fChangeDiffs.iterator();
	}

	public Iterator<Diff> rangesIterator() {
		// System.out.println(getMethodName());
		if (this.fAllDiffs == null) {
			return new ArrayList<Diff>().iterator();
		}
		return this.fAllDiffs.iterator();
	}

	public boolean isFirstChildDiff(final char contributor, final int childStart, final Diff diff) {
		// System.out.println(getMethodName());
		if (!diff.hasChildren()) {
			return false;
		}
		Diff d = diff.fDiffs.get(0);
		Position p = d.getPosition(contributor);
		return (p.getOffset() >= childStart);
	}

	public Diff getWrappedDiff(final Diff diff, final boolean down) {
		// System.out.println(getMethodName());
		if (this.fChangeDiffs != null && this.fChangeDiffs.size() > 0) {
			if (down) {
				return this.fChangeDiffs.get(0);
			}
			return this.fChangeDiffs.get(this.fChangeDiffs.size() - 1);
		}
		return null;
	}

	/*
	 * Copy the contents of the given diff from one side to the other but
	 * doesn't reveal anything.
	 * Returns true if copy was successful.
	 */
	public boolean copy(final Diff diff, final boolean leftToRight) {
		// System.out.println(getMethodName());

		if (diff != null) {
			Position fromPos = null;
			Position toPos = null;
			IDocument fromDoc = null;
			IDocument toDoc = null;

			if (leftToRight) {
				fromPos = diff.getPosition(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				toPos = diff.getPosition(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				fromDoc = this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				toDoc = this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
			} else {
				fromPos = diff.getPosition(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				toPos = diff.getPosition(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
				fromDoc = this.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
				toDoc = this.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
			}

			if (fromDoc != null) {

				int fromStart = fromPos.getOffset();
				int fromLen = fromPos.getLength();

				int toStart = toPos.getOffset();
				int toLen = toPos.getLength();

				try {
					String s = null;

					switch (diff.getKind()) {
					case RangeDifference.RIGHT:
					case RangeDifference.LEFT:
						s = fromDoc.get(fromStart, fromLen);
						break;
					case RangeDifference.ANCESTOR:
						break;
					case RangeDifference.CONFLICT:
						if (APPEND_CONFLICT) {
							s = toDoc.get(toStart, toLen);
							String ls = TextUtilities.getDefaultLineDelimiter(toDoc);
							if (!s.endsWith(ls)) {
								s += ls;
							}
							s += fromDoc.get(fromStart, fromLen);
						} else {
							s = fromDoc.get(fromStart, fromLen);
						}
						break;
					}
					if (s != null) {
						toDoc.replace(toStart, toLen, s);
						toPos.setOffset(toStart);
						toPos.setLength(s.length());
					}

				} catch (BadLocationException e) {
					// silently ignored
				}
			}

			diff.setResolved(true);
			return true;
		}
		return false;
	}

	public int changesCount() {
		// System.out.println(getMethodName());
		if (this.fChangeDiffs == null) {
			return 0;
		}
		return this.fChangeDiffs.size();
	}

	public Diff findDiff(final char contributor, final int rangeStart, final int rangeEnd) {
		// System.out.println(getMethodName());
		if (this.hasChanges()) {
			for (Iterator<Diff> iterator = this.changesIterator(); iterator.hasNext();) {
				Diff diff = iterator.next();
				if (diff.isDeleted() || diff.getKind() == RangeDifference.NOCHANGE) {
					continue;
				}
				if (diff.overlaps(contributor, rangeStart, rangeEnd, this.getDocument(contributor).getLength())) {
					return diff;
				}
			}
		}
		return null;
	}

	public Diff findDiff(final char contributor, final Position range) {
		// System.out.println(getMethodName());
		int start = range.getOffset();
		int end = start + range.getLength();
		return this.findDiff(contributor, start, end);
	}

	public Diff findNext(final char contributor, final int start, final int end, final boolean deep) {
		// System.out.println(getMethodName());
		return this.findNext(contributor, this.fChangeDiffs, start, end, deep);
	}

	private Diff findNext(final char contributor, final List<Diff> v, final int start, final int end,
			final boolean deep) {
		// System.out.println(getMethodName());
		if (v == null) {
			return null;
		}
		for (Diff diff : v) {
			Position p = diff.getPosition(contributor);
			if (p != null) {
				int startOffset = p.getOffset();
				if (end < startOffset) {
					return diff;
				}
				if (deep && diff.hasChildren()) {
					Diff d = null;
					int endOffset = startOffset + p.getLength();
					if (start == startOffset && (end == endOffset || end == endOffset - 1)) {
						d = this.findNext(contributor, diff.fDiffs, start - 1, start - 1, deep);
					} else if (end < endOffset) {
						d = this.findNext(contributor, diff.fDiffs, start, end, deep);
					}
					if (d != null) {
						return d;
					}
				}
			}
		}
		return null;
	}

	public Diff findPrev(final char contributor, final int start, final int end, final boolean deep) {
		// System.out.println(getMethodName());
		return this.findPrev(contributor, this.fChangeDiffs, start, end, deep);
	}

	private Diff findPrev(final char contributor, final List<Diff> v, final int start, final int end,
			final boolean deep) {
		// System.out.println(getMethodName());
		if (v == null) {
			return null;
		}
		for (int i = v.size() - 1; i >= 0; i--) {
			Diff diff = v.get(i);
			Position p = diff.getPosition(contributor);
			if (p != null) {
				int startOffset = p.getOffset();
				int endOffset = startOffset + p.getLength();
				if (start > endOffset) {
					if (deep && diff.hasChildren()) {
						// If we are going deep, find the last change in the diff
						return this.findPrev(contributor, diff.fDiffs, end, end, deep);
					}
					return diff;
				}
				if (deep && diff.hasChildren()) {
					Diff d = null;
					if (start == startOffset && end == endOffset) {
						// A whole diff is selected so we'll fall through
						// and go the the last change in the previous diff
					} else if (start >= startOffset) {
						// If we are at or before the first diff, select the
						// entire diff so next and previous are symmetrical
						if (this.isFirstChildDiff(contributor, start, diff)) {
							return diff;
						}
						d = this.findPrev(contributor, diff.fDiffs, start, end, deep);
					}
					if (d != null) {
						return d;
					}
				}
			}
		}
		return null;
	}

	public int getOffset(final char contributor, final int lineNumber) {
		IDocument document = this.getDocument(contributor);
		Position position = this.getRegion(contributor);

		try {
			// Logic from DocLineComparator constructor
			//			int lineOffset = document.getLineOfOffset(position.getOffset());

			//			int line = lineNumber - lineOffset;
			int line = lineNumber;
			int offset = document.getLineOffset(line);
			//			return offset - position.getOffset();
			return offset;
		} catch (BadLocationException e) {
			return 0;
		}
	}

}