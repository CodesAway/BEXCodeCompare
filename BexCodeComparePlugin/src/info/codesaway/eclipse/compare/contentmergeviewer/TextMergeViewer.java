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
 *     channingwalton@mac.com - curved line code
 *     gilles.querret@free.fr - fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=72995
 *     Max Weninger (max.weninger@windriver.com) - Bug 131895 [Edit] Undo in compare
 *     Max Weninger (max.weninger@windriver.com) - Bug 72936 [Viewers] Show line numbers in comparision
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 178968 [Viewers] Lines scrambled and different font size in compare
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 191524 [Viewers] Synchronize horizontal scrolling by # characters, not % of longest line
 *     Stephan Herrmann (stephan@cs.tu-berlin.de) - Bug 291695: Element compare fails to use source range
 *     Robin Stocker (robin@nibor.org) - Bug 398594: [Edit] Enable center arrow buttons when editable and for both sides
 *     Robin Stocker (robin@nibor.org) - Bug 399960: [Edit] Make merge arrow buttons easier to hit
 *     John Hendrikx (hjohn@xs4all.nl) - Bug 541401 - [regression] Vertical scrollbar thumb size is wrong in compare view
 *     Stefan Dirix (sdirix@eclipsesource.com) - Bug 473847: Minimum E4 Compatibility of Compare
 *******************************************************************************/
package info.codesaway.eclipse.compare.contentmergeviewer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.INavigatable;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.BufferedCanvas;
import org.eclipse.compare.internal.ChangeCompareFilterPropertyAction;
import org.eclipse.compare.internal.ChangePropertyAction;
import org.eclipse.compare.internal.CompareContentViewerSwitchingPane;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareEditorContributor;
import org.eclipse.compare.internal.CompareEditorSelectionProvider;
import org.eclipse.compare.internal.CompareFilterDescriptor;
import org.eclipse.compare.internal.CompareHandlerService;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.DocumentManager;
import org.eclipse.compare.internal.ICompareContextIds;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.internal.MergeSourceViewer;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.NavigationEndDialog;
import org.eclipse.compare.internal.OutlineViewerCreator;
import org.eclipse.compare.internal.ShowWhitespaceAction;
import org.eclipse.compare.internal.TextEditorPropertyAction;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.CursorLinePainter;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ChangeEncodingAction;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.GotoLineAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.IFindReplaceTargetExtension2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import info.codesaway.bex.views.BEXView;
import info.codesaway.eclipse.compare.internal.merge.DocumentMerger;
import info.codesaway.eclipse.compare.internal.merge.DocumentMerger.Diff;
import info.codesaway.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;

/**
 * A text merge viewer uses the <code>RangeDifferencer</code> to perform a
 * textual, line-by-line comparison of two (or three) input documents. It is
 * based on the <code>ContentMergeViewer</code> and uses
 * <code>TextViewer</code>s to implement the ancestor, left, and right content
 * areas.
 * <p>
 * In the three-way compare case ranges of differing lines are highlighted and
 * framed with different colors to show whether the difference is an incoming,
 * outgoing, or conflicting change. The <code>TextMergeViewer</code> supports
 * the notion of a current "differing range" and provides toolbar buttons to
 * navigate from one range to the next (or previous).
 * <p>
 * If there is a current "differing range" and the underlying document is
 * editable the <code>TextMergeViewer</code> enables actions in context menu and
 * toolbar to copy a range from one side to the other side, thereby performing a
 * merge operation.
 * <p>
 * In addition to a line-by-line comparison the <code>TextMergeViewer</code>
 * uses a token based compare on differing lines. The token compare is activated
 * when navigating into a range of differing lines. At first the lines are
 * selected as a block. When navigating into this block the token compare shows
 * for every line the differing token by selecting them.
 * <p>
 * The <code>TextMergeViewer</code>'s default token compare works on characters
 * separated by whitespace. If a different strategy is needed (for example, Java
 * tokens in a Java-aware merge viewer), clients can create their own token
 * comparators by implementing the <code>ITokenComparator</code> interface and
 * overriding the <code>TextMergeViewer.createTokenComparator</code> factory
 * method).
 * <p>
 * Access to the <code>TextMergeViewer</code>'s model is by means of an
 * <code>IMergeViewerContentProvider</code>. Its <code>getXContent</code>
 * methods must return either an <code>IDocument</code>, an
 * <code>IDocumentRange</code>, or an <code>IStreamContentAccessor</code>. In
 * the <code>IDocumentRange</code> case the <code>TextMergeViewer</code> works
 * on a subrange of a document. In the <code>IStreamContentAccessor</code> case
 * a document is created internally and initialized from the stream.
 * <p>
 * A <code>TextMergeViewer</code> can be used as is. However clients may
 * subclass to customize the behavior. For example a
 * <code>MergeTextViewer</code> for Java would override the
 * <code>configureTextViewer</code> method to configure the
 * <code>TextViewer</code> for Java source code, the
 * <code>createTokenComparator</code> method to create a Java specific
 * tokenizer.
 * <p>
 * In 3.5 a new API has been introduced to let clients provide their own source
 * viewers implementation with an option to configure them basing on a
 * corresponding editor input.
 *
 * @see org.eclipse.compare.rangedifferencer.RangeDifferencer
 * @see org.eclipse.jface.text.TextViewer
 * @see ITokenComparator
 * @see IDocumentRange
 * @see org.eclipse.compare.IStreamContentAccessor
 */
public class TextMergeViewer extends ContentMergeViewer implements IAdaptable {
	private static final String COPY_LEFT_TO_RIGHT_INDICATOR = ">"; //$NON-NLS-1$
	private static final String COPY_RIGHT_TO_LEFT_INDICATOR = "<"; //$NON-NLS-1$
	private static final char ANCESTOR_CONTRIBUTOR = MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR;
	private static final char RIGHT_CONTRIBUTOR = MergeViewerContentProvider.RIGHT_CONTRIBUTOR;
	private static final char LEFT_CONTRIBUTOR = MergeViewerContentProvider.LEFT_CONTRIBUTOR;

	private static final String DIFF_RANGE_CATEGORY = CompareUIPlugin.PLUGIN_ID + ".DIFF_RANGE_CATEGORY"; //$NON-NLS-1$

	static final boolean DEBUG = false;

	private static final boolean FIX_47640 = true;

	private static final String[] GLOBAL_ACTIONS = {
			ActionFactory.UNDO.getId(),
			ActionFactory.REDO.getId(),
			ActionFactory.CUT.getId(),
			ActionFactory.COPY.getId(),
			ActionFactory.PASTE.getId(),
			ActionFactory.DELETE.getId(),
			ActionFactory.SELECT_ALL.getId(),
			ActionFactory.FIND.getId(),
			ITextEditorActionDefinitionIds.LINE_GOTO,
			ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS
	};
	private static final String[] TEXT_ACTIONS = {
			MergeSourceViewer.UNDO_ID,
			MergeSourceViewer.REDO_ID,
			MergeSourceViewer.CUT_ID,
			MergeSourceViewer.COPY_ID,
			MergeSourceViewer.PASTE_ID,
			MergeSourceViewer.DELETE_ID,
			MergeSourceViewer.SELECT_ALL_ID,
			MergeSourceViewer.FIND_ID,
			MergeSourceViewer.GOTO_LINE_ID,
			ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS
	};

	private static final String BUNDLE_NAME = "org.eclipse.compare.contentmergeviewer.TextMergeViewerResources"; //$NON-NLS-1$

	// the following symbolic constants must match the IDs in Compare's plugin.xml
	private static final String INCOMING_COLOR = "INCOMING_COLOR"; //$NON-NLS-1$
	private static final String OUTGOING_COLOR = "OUTGOING_COLOR"; //$NON-NLS-1$
	private static final String CONFLICTING_COLOR = "CONFLICTING_COLOR"; //$NON-NLS-1$
	private static final String RESOLVED_COLOR = "RESOLVED_COLOR"; //$NON-NLS-1$
	private static final String ADDITION_COLOR = "ADDITION_COLOR"; //$NON-NLS-1$
	private static final String DELETION_COLOR = "DELETION_COLOR"; //$NON-NLS-1$
	private static final String EDITION_COLOR = "EDITION_COLOR"; //$NON-NLS-1$

	// constants
	/** Width of left and right vertical bar */
	private static final int MARGIN_WIDTH = 6;
	/** Width of center bar */
	private static final int CENTER_WIDTH = 34;
	/** Width of birds eye view */
	private static final int BIRDS_EYE_VIEW_WIDTH = 12;
	/** Width of birds eye view */
	private static final int BIRDS_EYE_VIEW_INSET = 2;
	/** */
	private static final int RESOLVE_SIZE = 5;

	/** line width of change borders */
	private static final int LW = 1;

	private final boolean fShowCurrentOnly = false;
	private final boolean fShowCurrentOnly2 = false;
	private final int fMarginWidth = MARGIN_WIDTH;
	private int fTopInset;

	// Colors
	private RGB fBackground;
	private RGB fForeground;

	private boolean fIsUsingSystemForeground = true;
	private boolean fIsUsingSystemBackground = true;

	private static final class ColorPalette {
		public final RGB selected;
		public final RGB normal;
		public final RGB fill;
		public final RGB textFill;

		public ColorPalette(final RGB background, final String registryKey, final RGB defaultRGB) {
			RGB registry = JFaceResources.getColorRegistry().getRGB(registryKey);
			if (registry != null) {
				this.selected = registry;
			} else {
				this.selected = defaultRGB;
			}
			this.normal = interpolate(this.selected, background, 0.6);
			this.fill = interpolate(this.selected, background, 0.9);
			this.textFill = interpolate(this.selected, background, 0.8);
		}

		private static RGB interpolate(final RGB fg, final RGB bg, final double scale) {
			if (fg != null && bg != null) {
				return new RGB((int) ((1.0 - scale) * fg.red + scale * bg.red),
						(int) ((1.0 - scale) * fg.green + scale * bg.green),
						(int) ((1.0 - scale) * fg.blue + scale * bg.blue));
			}
			if (fg != null) {
				return fg;
			}
			if (bg != null) {
				return bg;
			}
			return new RGB(128, 128, 128); // a gray
		}
	}

	private ColorPalette incomingPalette;
	private ColorPalette conflictPalette;
	private ColorPalette outgoingPalette;
	private ColorPalette additionPalette;
	private ColorPalette deletionPalette;
	private ColorPalette editionPalette;
	private RGB RESOLVED;

	private IPreferenceStore fPreferenceStore;
	private IPropertyChangeListener fPreferenceChangeListener;

	private final HashMap<Object, Position> fNewAncestorRanges = new HashMap<>();
	private final HashMap<Object, Position> fNewLeftRanges = new HashMap<>();
	private final HashMap<Object, Position> fNewRightRanges = new HashMap<>();

	private MergeSourceViewer fAncestor;
	private MergeSourceViewer fLeft;
	private MergeSourceViewer fRight;

	private int fLeftLineCount;
	private int fRightLineCount;

	private boolean fInScrolling;

	private final int fPts[] = new int[8]; // scratch area for polygon drawing

	private int fInheritedDirection; // inherited direction
	private int fTextDirection; // requested direction for embedded SourceViewer

	private ActionContributionItem fIgnoreAncestorItem;
	private boolean fHighlightRanges;

	private boolean fShowPseudoConflicts = false;

	private final boolean fUseSplines = true;
	private boolean fUseSingleLine = true;
	private boolean fHighlightTokenChanges = false;

	private String fSymbolicFontName;

	private ActionContributionItem fNextDiff; // goto next difference
	private ActionContributionItem fPreviousDiff; // goto previous difference
	private ActionContributionItem fCopyDiffLeftToRightItem;
	private ActionContributionItem fCopyDiffRightToLeftItem;

	private CompareHandlerService fHandlerService;

	private boolean fSynchronizedScrolling = true;

	private MergeSourceViewer fFocusPart;

	private final boolean fSubDoc = true;
	private IPositionUpdater fPositionUpdater;
	private boolean fIsMac;

	private boolean fHasErrors;

	// SWT widgets
	private BufferedCanvas fAncestorCanvas;
	private BufferedCanvas fLeftCanvas;
	private BufferedCanvas fRightCanvas;
	private Canvas fScrollCanvas;
	private ScrollBar fVScrollBar;
	private Canvas fBirdsEyeCanvas;
	private Canvas fSummaryHeader;
	private HeaderPainter fHeaderPainter;

	// SWT resources to be disposed
	private Map<RGB, Color> fColors;

	// points for center curves
	private double[] fBasicCenterCurve;

	private Button fLeftToRightButton;
	private Button fRightToLeftButton;
	private Diff fButtonDiff;

	private ContributorInfo fLeftContributor;
	private ContributorInfo fRightContributor;
	private ContributorInfo fAncestorContributor;
	private int isRefreshing = 0;
	private int fSynchronziedScrollPosition;
	private ActionContributionItem fNextChange;
	private ActionContributionItem fPreviousChange;
	private ShowWhitespaceAction showWhitespaceAction;
	private InternalOutlineViewerCreator fOutlineViewerCreator;
	private TextEditorPropertyAction toggleLineNumbersAction;
	private IFindReplaceTarget fFindReplaceTarget;
	private ChangePropertyAction fIgnoreWhitespace;
	private final List<ChangeCompareFilterPropertyAction> fCompareFilterActions = new ArrayList<>();
	private DocumentMerger fMerger;
	/** The current diff */
	private Diff fCurrentDiff;
	private Diff fSavedDiff;

	// Bug 259362 - Update diffs after undo
	private boolean copyOperationInProgress = false;
	private IUndoableOperation copyUndoable = null;
	private IOperationHistoryListener operationHistoryListener;

	/**
	 * Preference key for highlighting current line.
	 */
	private final static String CURRENT_LINE = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;
	/**
	 * Preference key for highlight color of current line.
	 */
	private final static String CURRENT_LINE_COLOR = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR;

	private List<SourceViewerDecorationSupport> fSourceViewerDecorationSupport = new ArrayList<>(3);
	// whether enhanced viewer configuration has been done
	private boolean isConfigured = false;
	private boolean fRedoDiff = false;

	private final class InternalOutlineViewerCreator extends OutlineViewerCreator implements ISelectionChangedListener {
		@Override
		public Viewer findStructureViewer(final Viewer oldViewer,
				final ICompareInput input, final Composite parent,
				final CompareConfiguration configuration) {
			if (input != this.getInput()) {
				return null;
			}
			final Viewer v = CompareUI.findStructureViewer(oldViewer, input, parent, configuration);
			if (v != null) {
				v.getControl()
						.addDisposeListener(e -> v.removeSelectionChangedListener(InternalOutlineViewerCreator.this));
				v.addSelectionChangedListener(this);
			}

			return v;
		}

		@Override
		public boolean hasViewerFor(final Object input) {
			return true;
		}

		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			ISelection s = event.getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) s;
				Object element = ss.getFirstElement();
				Diff diff = this.findDiff(element);
				if (diff != null) {
					TextMergeViewer.this.setCurrentDiff(diff, true);
				}
			}
		}

		private Diff findDiff(final Object element) {
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				Position p = this.getPosition(ci.getLeft());
				if (p != null) {
					return this.findDiff(p, true);
				}
				p = this.getPosition(ci.getRight());
				if (p != null) {
					return this.findDiff(p, false);
				}
			}
			return null;
		}

		private Diff findDiff(final Position p, final boolean left) {
			for (Iterator<?> iterator = TextMergeViewer.this.fMerger.rangesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				Position diffPos;
				if (left) {
					diffPos = diff.getPosition(LEFT_CONTRIBUTOR);
				} else {
					diffPos = diff.getPosition(RIGHT_CONTRIBUTOR);
				}
				// If the element falls within a diff, highlight that diff
				if (diffPos.offset + diffPos.length >= p.offset && diff.getKind() != RangeDifference.NOCHANGE) {
					return diff;
				}
				// Otherwise, highlight the first diff after the elements position
				if (diffPos.offset >= p.offset) {
					return diff;
				}
			}
			return null;
		}

		private Position getPosition(final ITypedElement left) {
			if (left instanceof DocumentRangeNode) {
				DocumentRangeNode drn = (DocumentRangeNode) left;
				return drn.getRange();
			}
			return null;
		}

		@Override
		public Object getInput() {
			return TextMergeViewer.this.getInput();
		}
	}

	class ContributorInfo implements IElementStateListener, VerifyListener, IDocumentListener, IEncodingSupport {
		private final TextMergeViewer fViewer;
		private final Object fElement;
		private final char fLeg;
		private String fEncoding;
		private IDocumentProvider fDocumentProvider;
		private IEditorInput fDocumentKey;
		private ISelection fSelection;
		private int fTopIndex = -1;
		private boolean fNeedsValidation = false;
		private MergeSourceViewer fSourceViewer;

		public ContributorInfo(final TextMergeViewer viewer, final Object element, final char leg) {
			this.fViewer = viewer;
			this.fElement = element;
			this.fLeg = leg;
			if (this.fElement instanceof IEncodedStreamContentAccessor) {
				try {
					this.fEncoding = ((IEncodedStreamContentAccessor) this.fElement).getCharset();
				} catch (CoreException e) {
					// silently ignored
				}
			}
		}

		@Override
		public void setEncoding(final String encoding) {
			if (this.fDocumentKey == null || this.fDocumentProvider == null) {
				return;
			}
			if (this.fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) this.fDocumentProvider;
				String current = provider.getEncoding(this.fDocumentKey);
				boolean dirty = this.fDocumentProvider.canSaveDocument(this.fDocumentKey);
				if (!dirty) {
					String internal = encoding == null ? "" : encoding; //$NON-NLS-1$
					if (!internal.equals(current)) {
						provider.setEncoding(this.fDocumentKey, encoding);
						try {
							this.fDocumentProvider.resetDocument(this.fDocumentKey);
						} catch (CoreException e) {
							CompareUIPlugin.log(e);
						} finally {
							TextMergeViewer.this.update(true);
							TextMergeViewer.this.updateStructure(this.fLeg);
						}
					}
				}
			}
		}

		@Override
		public String getEncoding() {
			if (this.fDocumentProvider != null && this.fDocumentKey != null
					&& this.fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) this.fDocumentProvider;
				return provider.getEncoding(this.fDocumentKey);
			}
			return null;
		}

		@Override
		public String getDefaultEncoding() {
			if (this.fDocumentProvider != null && this.fDocumentKey != null
					&& this.fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) this.fDocumentProvider;
				return provider.getDefaultEncoding();
			}
			return null;
		}

		private String internalGetEncoding() {
			if (this.fElement instanceof IEncodedStreamContentAccessor) {
				try {
					this.fEncoding = ((IEncodedStreamContentAccessor) this.fElement)
							.getCharset();
				} catch (CoreException e) {
					// silently ignored
				}
			}
			if (this.fEncoding != null) {
				return this.fEncoding;
			}
			return ResourcesPlugin.getEncoding();
		}

		public void setEncodingIfAbsent(final ContributorInfo otherContributor) {
			if (this.fEncoding == null) {
				this.fEncoding = otherContributor.fEncoding;
			}
		}

		public IDocument getDocument() {
			if (this.fDocumentProvider != null) {
				IDocument document = this.fDocumentProvider.getDocument(this.getDocumentKey());
				if (document != null) {
					return document;
				}
			}
			if (this.fElement instanceof IDocument) {
				return (IDocument) this.fElement;
			}
			if (this.fElement instanceof IDocumentRange) {
				return ((IDocumentRange) this.fElement).getDocument();
			}
			if (this.fElement instanceof IStreamContentAccessor) {
				return DocumentManager.get(this.fElement);
			}
			return null;
		}

		public void setDocument(final MergeSourceViewer viewer, final boolean isEditable) {
			// Ensure that this method is only called once
			Assert.isTrue(this.fSourceViewer == null);
			this.fSourceViewer = viewer;
			try {
				this.internalSetDocument(viewer);
			} catch (RuntimeException e) {
				// The error may be due to a stale entry in the DocumentManager (see bug 184489)
				this.clearCachedDocument();
				throw e;
			}
			TextMergeViewer.this.setEditable(viewer.getSourceViewer(), isEditable);
			// Verify changes if the document is editable
			if (isEditable) {
				this.fNeedsValidation = true;
				viewer.getSourceViewer().getTextWidget().addVerifyListener(this);
			}
		}

		/*
		 * Returns true if a new Document could be installed.
		 */
		private boolean internalSetDocument(final MergeSourceViewer tp) {

			if (tp == null) {
				return false;
			}

			IDocument newDocument = null;
			Position range = null;

			if (this.fElement instanceof IDocumentRange) {
				newDocument = ((IDocumentRange) this.fElement).getDocument();
				range = ((IDocumentRange) this.fElement).getRange();
				this.connectToSharedDocument();

			} else if (this.fElement instanceof IDocument) {
				newDocument = (IDocument) this.fElement;
				TextMergeViewer.this.setupDocument(newDocument);

			} else if (this.fElement instanceof IStreamContentAccessor) {
				newDocument = DocumentManager.get(this.fElement);
				if (newDocument == null) {
					newDocument = this.createDocument();
					DocumentManager.put(this.fElement, newDocument);
					TextMergeViewer.this.setupDocument(newDocument);
				} else if (this.fDocumentProvider == null) {
					// Connect to a shared document so we can get the proper save synchronization
					this.connectToSharedDocument();
				}
			} else if (this.fElement == null) { // deletion on one side

				ITypedElement parent = this.fViewer.getParent(this.fLeg); // we try to find an insertion position within the deletion's parent

				if (parent instanceof IDocumentRange) {
					newDocument = ((IDocumentRange) parent).getDocument();
					newDocument.addPositionCategory(DIFF_RANGE_CATEGORY);
					Object input = this.fViewer.getInput();
					range = this.fViewer.getNewRange(this.fLeg, input);
					if (range == null) {
						int pos = 0;
						if (input instanceof ICompareInput) {
							pos = this.fViewer.findInsertionPosition(this.fLeg, (ICompareInput) input);
						}
						range = new Position(pos, 0);
						try {
							newDocument.addPosition(DIFF_RANGE_CATEGORY, range);
						} catch (BadPositionCategoryException ex) {
							// silently ignored
							if (TextMergeViewer.DEBUG) {
								System.out.println("BadPositionCategoryException: " + ex); //$NON-NLS-1$
							}
						} catch (BadLocationException ex) {
							// silently ignored
							if (TextMergeViewer.DEBUG) {
								System.out.println("BadLocationException: " + ex); //$NON-NLS-1$
							}
						}
						this.fViewer.addNewRange(this.fLeg, input, range);
					}
				} else if (parent instanceof IDocument) {
					newDocument = ((IDocumentRange) this.fElement).getDocument();
				}
			}

			boolean enabled = true;
			if (newDocument == null) {
				newDocument = new Document(""); //$NON-NLS-1$
				enabled = false;
			}

			// Update the viewer document or range
			IDocument oldDoc = tp.getSourceViewer().getDocument();
			if (newDocument != oldDoc) {
				this.updateViewerDocument(tp, newDocument, range);
			} else { // same document but different range
				this.updateViewerDocumentRange(tp, range);
			}
			newDocument.addDocumentListener(this);

			tp.setEnabled(enabled);

			return enabled;
		}

		/*
		 * The viewer document is the same but the range has changed
		 */
		private void updateViewerDocumentRange(final MergeSourceViewer tp, final Position range) {
			tp.setRegion(range);
			if (this.fViewer.fSubDoc) {
				if (range != null) {
					IRegion r = this.fViewer.normalizeDocumentRegion(tp.getSourceViewer().getDocument(),
							TextMergeViewer.toRegion(range));
					tp.getSourceViewer().setVisibleRegion(r.getOffset(), r.getLength());
				} else {
					tp.getSourceViewer().resetVisibleRegion();
				}
			} else {
				tp.getSourceViewer().resetVisibleRegion();
			}
		}

		/*
		 * The viewer has a new document
		 */
		private void updateViewerDocument(final MergeSourceViewer tp, final IDocument document, final Position range) {
			this.unsetDocument(tp);
			if (document == null) {
				return;
			}

			this.connectPositionUpdater(document);

			// install new document
			tp.setRegion(range);
			SourceViewer sourceViewer = tp.getSourceViewer();
			sourceViewer.setRedraw(false);
			try {
				if (this.fViewer.fSubDoc && range != null) {
					IRegion r = this.fViewer.normalizeDocumentRegion(document, TextMergeViewer.toRegion(range));
					sourceViewer.setDocument(document, r.getOffset(), r.getLength());
				} else {
					sourceViewer.setDocument(document);
				}
			} finally {
				sourceViewer.setRedraw(true);
			}

			tp.rememberDocument(document);
		}

		void connectPositionUpdater(final IDocument document) {
			document.addPositionCategory(DIFF_RANGE_CATEGORY);
			if (this.fViewer.fPositionUpdater == null) {
				this.fViewer.fPositionUpdater = this.fViewer.new ChildPositionUpdater(DIFF_RANGE_CATEGORY);
			} else {
				document.removePositionUpdater(this.fViewer.fPositionUpdater);
			}
			document.addPositionUpdater(this.fViewer.fPositionUpdater);
		}

		private void unsetDocument(final MergeSourceViewer tp) {
			IDocument oldDoc = this.internalGetDocument(tp);
			if (oldDoc != null) {
				tp.rememberDocument(null);
				try {
					oldDoc.removePositionCategory(DIFF_RANGE_CATEGORY);
				} catch (BadPositionCategoryException ex) {
					// Ignore
				}
				if (TextMergeViewer.this.fPositionUpdater != null) {
					oldDoc.removePositionUpdater(TextMergeViewer.this.fPositionUpdater);
				}
				oldDoc.removeDocumentListener(this);
			}
		}

		private IDocument createDocument() {
			// If the content provider is a text content provider, attempt to obtain
			// a shared document (i.e. file buffer)
			IDocument newDoc = this.connectToSharedDocument();

			if (newDoc == null) {
				IStreamContentAccessor sca = (IStreamContentAccessor) this.fElement;
				String s = null;

				try {
					String encoding = this.internalGetEncoding();
					s = Utilities.readString(sca, encoding);
				} catch (CoreException ex) {
					this.fViewer.setError(this.fLeg, ex.getMessage());
				}

				newDoc = new Document(s != null ? s : ""); //$NON-NLS-1$
			}
			return newDoc;
		}

		/**
		 * Connect to a shared document if possible. Return <code>null</code>
		 * if the connection was not possible.
		 * @return the shared document or <code>null</code> if connection to a
		 * shared document was not possible
		 */
		private IDocument connectToSharedDocument() {
			IEditorInput key = this.getDocumentKey();
			if (key != null) {
				if (this.fDocumentProvider != null) {
					// We've already connected and setup the document
					return this.fDocumentProvider.getDocument(key);
				}
				IDocumentProvider documentProvider = this.getDocumentProvider();
				if (documentProvider != null) {
					try {
						this.connect(documentProvider, key);
						this.setCachedDocumentProvider(key,
								documentProvider);
						IDocument newDoc = documentProvider.getDocument(key);
						this.fViewer.updateDirtyState(key, documentProvider, this.fLeg);
						return newDoc;
					} catch (CoreException e) {
						// Connection failed. Log the error and continue without a shared document
						CompareUIPlugin.log(e);
					}
				}
			}
			return null;
		}

		private void connect(final IDocumentProvider documentProvider, final IEditorInput input) throws CoreException {
			final ISharedDocumentAdapter sda = Adapters.adapt(this.fElement, ISharedDocumentAdapter.class);
			if (sda != null) {
				sda.connect(documentProvider, input);
			} else {
				documentProvider.connect(input);
			}
		}

		private void disconnect(final IDocumentProvider provider, final IEditorInput input) {
			final ISharedDocumentAdapter sda = Adapters.adapt(this.fElement, ISharedDocumentAdapter.class);
			if (sda != null) {
				sda.disconnect(provider, input);
			} else {
				provider.disconnect(input);
			}
		}

		private void setCachedDocumentProvider(final IEditorInput key,
				final IDocumentProvider documentProvider) {
			this.fDocumentKey = key;
			this.fDocumentProvider = documentProvider;
			documentProvider.addElementStateListener(this);
		}

		public void disconnect() {
			IDocumentProvider provider = null;
			IEditorInput input = this.getDocumentKey();
			synchronized (this) {
				if (this.fDocumentProvider != null) {
					provider = this.fDocumentProvider;
					this.fDocumentProvider = null;
					this.fDocumentKey = null;
				}
			}
			if (provider != null) {
				this.disconnect(provider, input);
				provider.removeElementStateListener(this);
			}
			// If we have a listener registered with the widget, remove it
			if (this.fSourceViewer != null) {
				StyledText textWidget = this.fSourceViewer.getSourceViewer().getTextWidget();
				if (textWidget != null && !textWidget.isDisposed()) {
					if (this.fNeedsValidation) {
						textWidget.removeVerifyListener(this);
						this.fNeedsValidation = false;
					}
					IDocument oldDoc = this.internalGetDocument(this.fSourceViewer);
					if (oldDoc != null) {
						oldDoc.removeDocumentListener(this);
					}
				}
			}
			this.clearCachedDocument();
		}

		private void clearCachedDocument() {
			// Finally, remove the document from the document manager
			IDocument doc = DocumentManager.get(this.fElement);
			if (doc != null) {
				DocumentManager.remove(doc);
			}
		}

		private IDocument internalGetDocument(final MergeSourceViewer tp) {
			IDocument oldDoc = tp.getSourceViewer().getDocument();
			if (oldDoc == null) {
				oldDoc = tp.getRememberedDocument();
			}
			return oldDoc;
		}

		/**
		 * Return the document key used to obtain a shared document. A <code>null</code>
		 * is returned in the following cases:
		 * <ol>
		 * <li>This contributor does not have a shared document adapter.</li>
		 * <li>This text merge viewer has a document partitioner but uses the default partitioning.</li>
		 * <li>This text merge viewer does not use he default content provider.</li>
		 * </ol>
		 * @return the document key used to obtain a shared document or <code>null</code>
		 */
		private IEditorInput getDocumentKey() {
			if (this.fDocumentKey != null) {
				return this.fDocumentKey;
			}
			if (this.isUsingDefaultContentProvider() && this.fElement != null && this.canHaveSharedDocument()) {
				ISharedDocumentAdapter sda = Adapters.adapt(this.fElement, ISharedDocumentAdapter.class);
				if (sda != null) {
					return sda.getDocumentKey(this.fElement);
				}
			}
			return null;
		}

		private IDocumentProvider getDocumentProvider() {
			if (this.fDocumentProvider != null) {
				return this.fDocumentProvider;
			}
			// We will only use document providers if the content provider is the
			// default content provider
			if (this.isUsingDefaultContentProvider()) {
				IEditorInput input = this.getDocumentKey();
				if (input != null) {
					return SharedDocumentAdapter.getDocumentProvider(input);
				}
			}
			return null;
		}

		private boolean isUsingDefaultContentProvider() {
			return this.fViewer.isUsingDefaultContentProvider();
		}

		private boolean canHaveSharedDocument() {
			return this.fViewer.canHaveSharedDocument();
		}

		boolean hasSharedDocument(final Object object) {
			return (this.fElement == object &&
					this.fDocumentProvider != null
					&& this.fDocumentProvider.getDocument(this.getDocumentKey()) != null);
		}

		public boolean flush() throws CoreException {
			if (this.fDocumentProvider != null) {
				IEditorInput input = this.getDocumentKey();
				IDocument document = this.fDocumentProvider.getDocument(input);
				if (document != null) {
					final ISharedDocumentAdapter sda = Adapters.adapt(this.fElement, ISharedDocumentAdapter.class);
					if (sda != null) {
						sda.flushDocument(this.fDocumentProvider, input, document, false);
						return true;
					}
					try {
						this.fDocumentProvider.aboutToChange(input);
						this.fDocumentProvider.saveDocument(new NullProgressMonitor(), input, document, false);
						return true;
					} finally {
						this.fDocumentProvider.changed(input);
					}
				}
			}
			return false;
		}

		@Override
		public void elementMoved(final Object originalElement, final Object movedElement) {
			IEditorInput input = this.getDocumentKey();
			if (input != null && input.equals(originalElement)) {
				// This method will only get called if the buffer is not dirty
				this.resetDocument();
			}
		}

		@Override
		public void elementDirtyStateChanged(final Object element, final boolean isDirty) {
			if (!this.checkState()) {
				return;
			}
			IEditorInput input = this.getDocumentKey();
			if (input != null && input.equals(element)) {
				this.fViewer.updateDirtyState(input, this.getDocumentProvider(), this.fLeg);
			}
		}

		@Override
		public void elementDeleted(final Object element) {
			IEditorInput input = this.getDocumentKey();
			if (input != null && input.equals(element)) {
				// This method will only get called if the buffer is not dirty
				this.resetDocument();
			}
		}

		private void resetDocument() {
			// Need to remove the document from the manager before refreshing
			// or the old document will still be found
			this.clearCachedDocument();
			// TODO: This is fine for now but may need to be revisited if a refresh is performed
			// higher up as well (e.g. perhaps a refresh request that waits until after all parties
			// have been notified).
			if (this.checkState()) {
				this.fViewer.refresh();
			}
		}

		private boolean checkState() {
			if (this.fViewer == null) {
				return false;
			}
			Control control = this.fViewer.getControl();
			if (control == null) {
				return false;
			}
			return !control.isDisposed();
		}

		@Override
		public void elementContentReplaced(final Object element) {
			if (!this.checkState()) {
				return;
			}
			IEditorInput input = this.getDocumentKey();
			if (input != null && input.equals(element)) {
				this.fViewer.updateDirtyState(input, this.getDocumentProvider(), this.fLeg);

				// recalculate diffs and update controls
				new UIJob(CompareMessages.DocumentMerger_0) {
					@Override
					public IStatus runInUIThread(final IProgressMonitor monitor) {
						TextMergeViewer.this.update(true);
						TextMergeViewer.this.updateStructure(ContributorInfo.this.fLeg);
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		}

		@Override
		public void elementContentAboutToBeReplaced(final Object element) {
			// Nothing to do
		}

		public Object getElement() {
			return this.fElement;
		}

		public void cacheSelection(final MergeSourceViewer viewer) {
			if (viewer == null) {
				this.fSelection = null;
				this.fTopIndex = -1;
			} else {
				this.fSelection = viewer.getSourceViewer().getSelection();
				this.fTopIndex = viewer.getSourceViewer().getTopIndex();
			}
		}

		public void updateSelection(final MergeSourceViewer viewer, final boolean includeScroll) {
			if (this.fSelection != null) {
				viewer.getSourceViewer().setSelection(this.fSelection);
			}
			if (includeScroll && this.fTopIndex != -1) {
				viewer.getSourceViewer().setTopIndex(this.fTopIndex);
			}
		}

		public void transferContributorStateFrom(
				final ContributorInfo oldContributor) {
			if (oldContributor != null) {
				this.fSelection = oldContributor.fSelection;
				this.fTopIndex = oldContributor.fTopIndex;
				this.fEncoding = oldContributor.fEncoding;
			}

		}

		public boolean validateChange() {
			if (this.fElement == null) {
				return true;
			}
			if (this.fDocumentProvider instanceof IDocumentProviderExtension) {
				IDocumentProviderExtension ext = (IDocumentProviderExtension) this.fDocumentProvider;
				if (ext.isReadOnly(this.fDocumentKey)) {
					try {
						ext.validateState(this.fDocumentKey, TextMergeViewer.this.getControl().getShell());
						ext.updateStateCache(this.fDocumentKey);
					} catch (CoreException e) {
						ErrorDialog.openError(TextMergeViewer.this.getControl().getShell(),
								CompareMessages.TextMergeViewer_12, CompareMessages.TextMergeViewer_13, e.getStatus());
						return false;
					}
				}
				return !ext.isReadOnly(this.fDocumentKey);
			}
			IEditableContentExtension ext = Adapters.adapt(this.fElement, IEditableContentExtension.class);
			if (ext != null) {
				if (ext.isReadOnly()) {
					IStatus status = ext.validateEdit(TextMergeViewer.this.getControl().getShell());
					if (!status.isOK()) {
						if (status.getSeverity() == IStatus.ERROR) {
							ErrorDialog.openError(TextMergeViewer.this.getControl().getShell(),
									CompareMessages.TextMergeViewer_14, CompareMessages.TextMergeViewer_15, status);
							return false;
						}
						if (status.getSeverity() == IStatus.CANCEL) {
							return false;
						}
					}
				}
			}
			return true;
		}

		@Override
		public void verifyText(final VerifyEvent e) {
			if (!this.validateChange()) {
				e.doit = false;
			}
		}

		@Override
		public void documentAboutToBeChanged(final DocumentEvent e) {
			// nothing to do
		}

		@Override
		public void documentChanged(final DocumentEvent e) {
			boolean dirty = true;
			if (this.fDocumentProvider != null && this.fDocumentKey != null) {
				dirty = this.fDocumentProvider.canSaveDocument(this.fDocumentKey);
			}
			TextMergeViewer.this.documentChanged(e, dirty);
			// Remove our verify listener since the document is now dirty
			if (this.fNeedsValidation && this.fSourceViewer != null
					&& !this.fSourceViewer.getSourceViewer().getTextWidget().isDisposed()) {
				this.fSourceViewer.getSourceViewer().getTextWidget().removeVerifyListener(this);
				this.fNeedsValidation = false;
			}
		}
	}

	class HeaderPainter implements PaintListener {
		private static final int INSET = BIRDS_EYE_VIEW_INSET;

		private RGB fIndicatorColor;
		private final Color fSeparatorColor;

		public HeaderPainter() {
			this.fSeparatorColor = TextMergeViewer.this.fSummaryHeader.getDisplay()
					.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		}

		/*
		 * Returns true on color change
		 */
		public boolean setColor(final RGB color) {
			RGB oldColor = this.fIndicatorColor;
			this.fIndicatorColor = color;
			if (color == null) {
				return oldColor != null;
			}
			if (oldColor != null) {
				return !color.equals(oldColor);
			}
			return true;
		}

		private void drawBevelRect(final GC gc, final int x, final int y, final int w, final int h, final Color topLeft,
				final Color bottomRight) {
			gc.setForeground(topLeft);
			gc.drawLine(x, y, x + w - 1, y);
			gc.drawLine(x, y, x, y + h - 1);

			gc.setForeground(bottomRight);
			gc.drawLine(x + w, y, x + w, y + h);
			gc.drawLine(x, y + h, x + w, y + h);
		}

		@Override
		public void paintControl(final PaintEvent e) {
			Point s = TextMergeViewer.this.fSummaryHeader.getSize();

			if (this.fIndicatorColor != null) {
				Display d = TextMergeViewer.this.fSummaryHeader.getDisplay();
				e.gc.setBackground(TextMergeViewer.this.getColor(d, this.fIndicatorColor));
				int min = Math.min(s.x, s.y) - 2 * INSET;
				Rectangle r = new Rectangle((s.x - min) / 2, (s.y - min) / 2, min, min);
				e.gc.fillRectangle(r);
				if (d != null) {
					this.drawBevelRect(e.gc, r.x, r.y, r.width - 1, r.height - 1,
							d.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
							d.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
				}

				e.gc.setForeground(this.fSeparatorColor);
				e.gc.setLineWidth(0 /* 1 */);
				e.gc.drawLine(0 + 1, s.y - 1, s.x - 1 - 1, s.y - 1);
			}
		}
	}

	/*
	 * The position updater used to adapt the positions representing
	 * the child document ranges to changes of the parent document.
	 */
	class ChildPositionUpdater extends DefaultPositionUpdater {
		/*
		 * Creates the position updated.
		 */
		protected ChildPositionUpdater(final String category) {
			super(category);
		}

		/*
		 * Child document ranges cannot be deleted other then by calling
		 * freeChildDocument.
		 */
		@Override
		protected boolean notDeleted() {
			return true;
		}

		/*
		 * If an insertion happens at a child document's start offset, the
		 * position is extended rather than shifted. Also, if something is added
		 * right behind the end of the position, the position is extended rather
		 * than kept stable.
		 */
		@Override
		protected void adaptToInsert() {
			if (this.fPosition == TextMergeViewer.this.fLeft.getRegion()
					|| this.fPosition == TextMergeViewer.this.fRight.getRegion()) {
				int myStart = this.fPosition.offset;
				int myEnd = this.fPosition.offset + this.fPosition.length;
				myEnd = Math.max(myStart, myEnd);

				int yoursStart = this.fOffset;
				int yoursEnd = this.fOffset + this.fReplaceLength - 1;
				yoursEnd = Math.max(yoursStart, yoursEnd);

				if (myEnd < yoursStart) {
					return;
				}

				if (myStart <= yoursStart) {
					this.fPosition.length += this.fReplaceLength;
				} else {
					this.fPosition.offset += this.fReplaceLength;
				}
			} else {
				super.adaptToInsert();
			}
		}
	}

	private class ChangeHighlighter implements ITextPresentationListener {
		private final MergeSourceViewer viewer;

		public ChangeHighlighter(final MergeSourceViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void applyTextPresentation(final TextPresentation textPresentation) {
			if (!TextMergeViewer.this.fHighlightTokenChanges) {
				return;
			}
			IRegion region = textPresentation.getExtent();
			Diff[] changeDiffs = TextMergeViewer.this.fMerger.getChangeDiffs(TextMergeViewer.this.getLeg(this.viewer),
					region);
			boolean allOutgoing = TextMergeViewer.this.allOutgoing();
			for (Diff diff : changeDiffs) {
				StyleRange range = this.getStyleRange(diff, region, allOutgoing);
				if (range != null) {
					textPresentation.mergeStyleRange(range);
				}
			}
		}

		private StyleRange getStyleRange(final Diff diff, final IRegion region, final boolean showAdditionRemoval) {
			//Color cText = getColor(null, getTextColor());
			Color cTextFill = TextMergeViewer.this.getColor(null, this.getTextFillColor(diff, showAdditionRemoval));
			if (cTextFill == null) {
				return null;
			}
			Position p = diff.getPosition(TextMergeViewer.this.getLeg(this.viewer));
			int start = p.getOffset();
			int length = p.getLength();
			// Don't start before the region
			if (start < region.getOffset()) {
				length = length - (region.getOffset() - start);
				start = region.getOffset();
			}
			// Don't go past the end of the region
			int regionEnd = region.getOffset() + region.getLength();
			if (start + length > regionEnd) {
				length = regionEnd - start;
			}
			if (length < 0) {
				return null;
			}

			return new StyleRange(start, length, null, cTextFill);
		}

		private RGB getTextFillColor(final Diff diff, final boolean showAdditionRemoval) {
			ColorPalette palette = TextMergeViewer.this.getColorPalette(diff, showAdditionRemoval);
			if (palette == null) {
				return null;
			}
			return palette.textFill;
		}
	}

	private class FindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension,
			IFindReplaceTargetExtension2, IFindReplaceTargetExtension3 {
		@Override
		public boolean canPerformFind() {
			return TextMergeViewer.this.fFocusPart != null;
		}

		@Override
		public int findAndSelect(final int widgetOffset, final String findString,
				final boolean searchForward, final boolean caseSensitive, final boolean wholeWord) {
			return Optional.ofNullable(this.getTarget())
					.map(
							target -> target.findAndSelect(widgetOffset, findString, searchForward, caseSensitive,
									wholeWord))
					.orElse(-1);
		}

		private IFindReplaceTarget getTarget() {
			return Optional.ofNullable(TextMergeViewer.this.fFocusPart)
					.map(MergeSourceViewer::getSourceViewer)
					.map(SourceViewer::getFindReplaceTarget)
					.orElse(null);
		}

		@Override
		public Point getSelection() {
			return Optional.ofNullable(this.getTarget())
					.map(IFindReplaceTarget::getSelection)
					.orElse(new Point(-1, -1));
		}

		@Override
		public String getSelectionText() {
			return Optional.ofNullable(this.getTarget()).map(IFindReplaceTarget::getSelectionText).orElse(""); //$NON-NLS-1$
		}

		@Override
		public boolean isEditable() {
			return Optional.ofNullable(this.getTarget()).map(IFindReplaceTarget::isEditable).orElse(false);
		}

		@Override
		public void replaceSelection(final String text) {
			Optional.ofNullable(this.getTarget()).ifPresent(target -> target.replaceSelection(text));
		}

		@Override
		public int findAndSelect(final int offset, final String findString, final boolean searchForward,
				final boolean caseSensitive, final boolean wholeWord, final boolean regExSearch) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension3) {
				return ((IFindReplaceTargetExtension3) findReplaceTarget).findAndSelect(offset, findString,
						searchForward, caseSensitive, wholeWord, regExSearch);
			}

			// fallback like in org.eclipse.ui.texteditor.FindReplaceTarget
			if (!regExSearch && findReplaceTarget != null) {
				return findReplaceTarget.findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord);
			}
			return -1;
		}

		@Override
		public void replaceSelection(final String text, final boolean regExReplace) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension3) {
				((IFindReplaceTargetExtension3) findReplaceTarget).replaceSelection(text, regExReplace);
				return;
			}

			// fallback like in org.eclipse.ui.texteditor.FindReplaceTarget
			if (!regExReplace && findReplaceTarget != null) {
				findReplaceTarget.replaceSelection(text);
			}
		}

		@Override
		public boolean validateTargetState() {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension2) {
				return ((IFindReplaceTargetExtension2) findReplaceTarget).validateTargetState();
			}
			// TODO not sure if true when findReplaceTarget is null
			return true;
		}

		@Override
		public void beginSession() {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).beginSession();
			}
		}

		@Override
		public void endSession() {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).endSession();
			}
		}

		@Override
		public IRegion getScope() {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				return ((IFindReplaceTargetExtension) findReplaceTarget).getScope();
			}
			return null;
		}

		@Override
		public void setScope(final IRegion scope) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setScope(scope);
			}
		}

		@Override
		public Point getLineSelection() {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				return ((IFindReplaceTargetExtension) findReplaceTarget).getLineSelection();
			}
			return null;
		}

		@Override
		public void setSelection(final int offset, final int length) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setSelection(offset, length);
			}
		}

		@Override
		public void setScopeHighlightColor(final Color color) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setScopeHighlightColor(color);
			}
		}

		@Override
		public void setReplaceAllMode(final boolean replaceAll) {
			IFindReplaceTarget findReplaceTarget = this.getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setReplaceAllMode(replaceAll);
			}
		}

	}

	//---- MergeTextViewer

	/**
	 * Creates a text merge viewer under the given parent control.
	 *
	 * @param parent the parent control
	 * @param configuration the configuration object
	 */
	public TextMergeViewer(final Composite parent, final CompareConfiguration configuration) {
		this(parent, SWT.NULL, configuration);
	}

	/**
	 * Creates a text merge viewer under the given parent control.
	 *
	 * @param parent the parent control
	 * @param style SWT style bits for top level composite of this viewer
	 * @param configuration the configuration object
	 */
	public TextMergeViewer(final Composite parent, final int style, final CompareConfiguration configuration) {
		super(style, ResourceBundle.getBundle(BUNDLE_NAME), configuration);

		this.operationHistoryListener = TextMergeViewer.this::historyNotification;
		OperationHistoryFactory.getOperationHistory()
				.addOperationHistoryListener(this.operationHistoryListener);

		this.fMerger = new DocumentMerger(new IDocumentMergerInput() {
			@Override
			public ITokenComparator createTokenComparator(final String line) {
				return TextMergeViewer.this.createTokenComparator(line);
			}

			@Override
			public CompareConfiguration getCompareConfiguration() {
				return TextMergeViewer.this.getCompareConfiguration();
			}

			@Override
			public IDocument getDocument(final char contributor) {
				switch (contributor) {
				case LEFT_CONTRIBUTOR:
					return TextMergeViewer.this.fLeft.getSourceViewer().getDocument();
				case RIGHT_CONTRIBUTOR:
					return TextMergeViewer.this.fRight.getSourceViewer().getDocument();
				case ANCESTOR_CONTRIBUTOR:
					return TextMergeViewer.this.fAncestor.getSourceViewer().getDocument();
				default:
					return null;
				}
			}

			@Override
			public int getHunkStart() {
				return TextMergeViewer.this.getHunkStart();
			}

			@Override
			public Position getRegion(final char contributor) {
				switch (contributor) {
				case LEFT_CONTRIBUTOR:
					return TextMergeViewer.this.fLeft.getRegion();
				case RIGHT_CONTRIBUTOR:
					return TextMergeViewer.this.fRight.getRegion();
				case ANCESTOR_CONTRIBUTOR:
					return TextMergeViewer.this.fAncestor.getRegion();
				default:
					return null;
				}
			}

			@Override
			public boolean isHunkOnLeft() {
				ITypedElement left = ((ICompareInput) TextMergeViewer.this.getInput()).getRight();
				return left != null && Adapters.adapt(left, IHunk.class) != null;
			}

			@Override
			public boolean isIgnoreAncestor() {
				return TextMergeViewer.this.isIgnoreAncestor();
			}

			@Override
			public boolean isPatchHunk() {
				return TextMergeViewer.this.isPatchHunk();
			}

			@Override
			public boolean isShowPseudoConflicts() {
				return TextMergeViewer.this.fShowPseudoConflicts;
			}

			@Override
			public boolean isThreeWay() {
				return TextMergeViewer.this.isThreeWay();
			}

			@Override
			public boolean isPatchHunkOk() {
				return TextMergeViewer.this.isPatchHunkOk();
			}
		});

		int inheritedStyle = parent.getStyle();
		if ((inheritedStyle & SWT.LEFT_TO_RIGHT) != 0) {
			this.fInheritedDirection = SWT.LEFT_TO_RIGHT;
		} else if ((inheritedStyle & SWT.RIGHT_TO_LEFT) != 0) {
			this.fInheritedDirection = SWT.RIGHT_TO_LEFT;
		} else {
			this.fInheritedDirection = SWT.NONE;
		}

		if ((style & SWT.LEFT_TO_RIGHT) != 0) {
			this.fTextDirection = SWT.LEFT_TO_RIGHT;
		} else if ((style & SWT.RIGHT_TO_LEFT) != 0) {
			this.fTextDirection = SWT.RIGHT_TO_LEFT;
		} else {
			this.fTextDirection = SWT.NONE;
		}

		this.fSymbolicFontName = this.getSymbolicFontName();

		this.fIsMac = Util.isMac();

		this.fPreferenceChangeListener = TextMergeViewer.this::handlePropertyChangeEvent;

		this.fPreferenceStore = this.createChainedPreferenceStore();
		if (this.fPreferenceStore != null) {
			this.fPreferenceStore.addPropertyChangeListener(this.fPreferenceChangeListener);

			this.fSynchronizedScrolling = this.fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING);
			this.fShowPseudoConflicts = this.fPreferenceStore.getBoolean(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS);
			//fUseSplines= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SPLINES);
			this.fUseSingleLine = this.fPreferenceStore.getBoolean(ComparePreferencePage.USE_SINGLE_LINE);
			this.fHighlightTokenChanges = this.fPreferenceStore
					.getBoolean(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES);
			//fUseResolveUI= fPreferenceStore.getBoolean(ComparePreferencePage.USE_RESOLVE_UI);
		}

		this.buildControl(parent);

		this.setColors();

		INavigatable nav = new INavigatable() {
			@Override
			public boolean selectChange(final int flag) {
				if (flag == INavigatable.FIRST_CHANGE || flag == INavigatable.LAST_CHANGE) {
					TextMergeViewer.this.selectFirstDiff(flag == INavigatable.FIRST_CHANGE);
					return false;
				}
				return TextMergeViewer.this.navigate(flag == INavigatable.NEXT_CHANGE, false, false);
			}

			@Override
			public Object getInput() {
				return TextMergeViewer.this.getInput();
			}

			@Override
			public boolean openSelectedChange() {
				return false;
			}

			@Override
			public boolean hasChange(final int flag) {
				return TextMergeViewer.this.getNextVisibleDiff(flag == INavigatable.NEXT_CHANGE, false) != null;
			}
		};
		this.fComposite.setData(INavigatable.NAVIGATOR_PROPERTY, nav);

		JFaceResources.getFontRegistry().addListener(this.fPreferenceChangeListener);
		JFaceResources.getColorRegistry().addListener(this.fPreferenceChangeListener);
		this.updateFont();
	}

	private static class LineNumberRulerToggleAction extends TextEditorPropertyAction {
		public LineNumberRulerToggleAction(final String label, final MergeSourceViewer[] viewers,
				final String preferenceKey) {
			super(label, viewers, preferenceKey);
		}

		@Override
		protected boolean toggleState(final boolean checked) {
			return true;
		}
	}

	private ChainedPreferenceStore createChainedPreferenceStore() {
		List<IPreferenceStore> stores = new ArrayList<>(2);
		stores.add(this.getCompareConfiguration().getPreferenceStore());
		stores.add(EditorsUI.getPreferenceStore());
		return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 * @param store preference store
	 * @param key preference key
	 * @return the color or <code>null</code>
	 */
	private static RGB createColor(final IPreferenceStore store, final String key) {
		if (!store.contains(key)) {
			return null;
		}
		if (store.isDefault(key)) {
			return PreferenceConverter.getDefaultColor(store, key);
		}
		return PreferenceConverter.getColor(store, key);
	}

	private void setColors() {
		this.fIsUsingSystemBackground = this.fPreferenceStore
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		if (!this.fIsUsingSystemBackground) {
			this.fBackground = createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		}

		this.fIsUsingSystemForeground = this.fPreferenceStore
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT);
		if (!this.fIsUsingSystemForeground) {
			this.fForeground = createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
		}

		this.updateColors(null);
	}

	private String getSymbolicFontName() {
		// Changed to get Eclipse class, since I don't extend it, so this code doesn't pick it up
		Class<?> clazz = org.eclipse.compare.contentmergeviewer.TextMergeViewer.class;
		//		Class<?> clazz = this.getClass();
		do {
			String fontName = clazz.getName();
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				return fontName;
			}
			clazz = clazz.getSuperclass();
		} while (clazz != null);
		// use text compare font if no font has been registered for subclass
		return this.getClass().getName();
	}

	private void updateFont() {
		Font f = JFaceResources.getFont(this.fSymbolicFontName);
		if (f != null) {
			if (this.fAncestor != null) {
				this.fAncestor.setFont(f);
			}
			if (this.fLeft != null) {
				this.fLeft.setFont(f);
			}
			if (this.fRight != null) {
				this.fRight.setFont(f);
			}
		}
	}

	private void checkForColorUpdate(final Display display) {
		if (this.fIsUsingSystemBackground) {
			RGB bg = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			if (!bg.equals(this.getBackground(display))) {
				this.updateColors(display);
			}
		}
	}

	/**
	 * Sets the viewer's background color to the given RGB value.
	 * If the value is <code>null</code> the system's default background color is used.
	 * @param background the background color or <code>null</code> to use the system's default background color
	 * @since 2.0
	 */
	public void setBackgroundColor(final RGB background) {
		this.fIsUsingSystemBackground = (background == null);
		this.fBackground = background;
		this.updateColors(null);
	}

	private RGB getBackground(Display display) {
		if (this.fBackground != null) {
			return this.fBackground;
		}

		if (display == null) {
			display = this.fComposite.getDisplay();
		}

		return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
	}

	/**
	 * Sets the viewer's foreground color to the given RGB value.
	 * If the value is <code>null</code> the system's default foreground color is used.
	 * @param foreground the foreground color or <code>null</code> to use the system's default foreground color
	 * @since 2.0
	 */
	public void setForegroundColor(final RGB foreground) {
		this.fIsUsingSystemForeground = (foreground == null);
		this.fForeground = foreground;
		this.updateColors(null);
	}

	private void updateColors(Display display) {

		if (display == null) {
			display = this.fComposite.getDisplay();
		}

		Color bgColor = null;
		if (this.fBackground != null) {
			bgColor = this.getColor(display, this.fBackground);
		}

		if (this.fAncestor != null) {
			this.fAncestor.setBackgroundColor(bgColor);
		}
		if (this.fLeft != null) {
			this.fLeft.setBackgroundColor(bgColor);
		}
		if (this.fRight != null) {
			this.fRight.setBackgroundColor(bgColor);
		}

		Color fgColor = null;
		if (this.fForeground != null) {
			fgColor = this.getColor(display, this.fForeground);
		}

		if (this.fAncestor != null) {
			this.fAncestor.setForegroundColor(fgColor);
		}
		if (this.fLeft != null) {
			this.fLeft.setForegroundColor(fgColor);
		}
		if (this.fRight != null) {
			this.fRight.setForegroundColor(fgColor);
		}

		ColorRegistry registry = JFaceResources.getColorRegistry();

		RGB bg = this.getBackground(display);

		this.incomingPalette = new ColorPalette(bg, INCOMING_COLOR, new RGB(0, 0, 255));
		this.outgoingPalette = new ColorPalette(bg, OUTGOING_COLOR, new RGB(0, 0, 0));
		this.conflictPalette = new ColorPalette(bg, CONFLICTING_COLOR, new RGB(255, 0, 0));
		this.additionPalette = new ColorPalette(bg, ADDITION_COLOR, new RGB(0, 255, 0));
		this.deletionPalette = new ColorPalette(bg, DELETION_COLOR, new RGB(255, 0, 0));
		this.editionPalette = new ColorPalette(bg, EDITION_COLOR, new RGB(0, 0, 0));

		this.RESOLVED = registry.getRGB(RESOLVED_COLOR);
		if (this.RESOLVED == null) {
			this.RESOLVED = new RGB(0, 255, 0); // GREEN
		}

		this.updatePresentation();
	}

	private void updatePresentation() {
		this.refreshBirdsEyeView();
		this.invalidateLines();
		this.invalidateTextPresentation();
	}

	/**
	 * Invalidates the current presentation by invalidating the three text viewers.
	 * @since 2.0
	 */
	public void invalidateTextPresentation() {
		if (this.fAncestor != null) {
			this.fAncestor.getSourceViewer().invalidateTextPresentation();
		}
		if (this.fLeft != null) {
			this.fLeft.getSourceViewer().invalidateTextPresentation();
		}
		if (this.fRight != null) {
			this.fRight.getSourceViewer().invalidateTextPresentation();
		}
	}

	/**
	 * Configures the passed text viewer. This method is called after the three
	 * text viewers have been created for the content areas. The
	 * <code>TextMergeViewer</code> implementation of this method will
	 * configure the viewer with a {@link SourceViewerConfiguration}.
	 * Subclasses may reimplement to provide a specific configuration for the
	 * text viewer.
	 *
	 * @param textViewer
	 *            the text viewer to configure
	 */
	protected void configureTextViewer(final TextViewer textViewer) {
		// to get undo for all text files, bugzilla 131895, 33665
		if (textViewer instanceof ISourceViewer) {
			SourceViewerConfiguration configuration = new SourceViewerConfiguration();
			((ISourceViewer) textViewer).configure(configuration);
		}
	}

	/**
	 * Creates an <code>ITokenComparator</code> which is used to show the
	 * intra line differences.
	 * The <code>TextMergeViewer</code> implementation of this method returns a
	 * tokenizer that breaks a line into words separated by whitespace.
	 * Subclasses may reimplement to provide a specific tokenizer.
	 * @param line the line for which to create the <code>ITokenComparator</code>
	 * @return a ITokenComparator which is used for a second level token compare.
	 */
	protected ITokenComparator createTokenComparator(final String line) {
		return new TokenComparator(line);
	}

	/**
	 * Setup the given document for use with this viewer. By default,
	 * the partitioner returned from {@link #getDocumentPartitioner()}
	 * is registered as the default partitioner for the document.
	 * Subclasses that return a partitioner must also override
	 * {@link #getDocumentPartitioning()} if they wish to be able to use shared
	 * documents (i.e. file buffers).
	 * @param document the document to be set up
	 *
	 * @since 3.3
	 */
	protected void setupDocument(final IDocument document) {
		String partitioning = this.getDocumentPartitioning();
		if (partitioning == null || !(document instanceof IDocumentExtension3)) {
			if (document.getDocumentPartitioner() == null) {
				IDocumentPartitioner partitioner = this.getDocumentPartitioner();
				if (partitioner != null) {
					document.setDocumentPartitioner(partitioner);
					partitioner.connect(document);
				}
			}
		} else {
			IDocumentExtension3 ex3 = (IDocumentExtension3) document;
			if (ex3.getDocumentPartitioner(partitioning) == null) {
				IDocumentPartitioner partitioner = this.getDocumentPartitioner();
				if (partitioner != null) {
					ex3.setDocumentPartitioner(partitioning, partitioner);
					partitioner.connect(document);
				}
			}
		}
	}

	/**
	 * Returns a document partitioner which is suitable for the underlying content type.
	 * This method is only called if the input provided by the content provider is a
	 * <code>IStreamContentAccessor</code> and an internal document must be obtained. This
	 * document is initialized with the partitioner returned from this method.
	 * <p>
	 * The <code>TextMergeViewer</code> implementation of this method returns
	 * <code>null</code>. Subclasses may reimplement to create a partitioner for a
	 * specific content type. Subclasses that do return a partitioner should also
	 * return a partitioning from {@link #getDocumentPartitioning()} in order to make
	 * use of shared documents (e.g. file buffers).
	 *
	 * @return a document partitioner, or <code>null</code>
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return null;
	}

	/**
	 * Return the partitioning to which the partitioner returned from
	 * {@link #getDocumentPartitioner()} is to be associated. Return <code>null</code>
	 * only if partitioning is not needed or if the subclass
	 * overrode {@link #setupDocument(IDocument)} directly.
	 * By default, <code>null</code> is returned which means that shared
	 * documents that return a partitioner from {@link #getDocumentPartitioner()}
	 * will not be able to use shared documents.
	 * @see IDocumentExtension3
	 * @return a partitioning
	 *
	 * @since 3.3
	 */
	protected String getDocumentPartitioning() {
		return null;
	}

	/**
	 * Called on the viewer disposal.
	 * Unregisters from the compare configuration.
	 * Clients may extend if they have to do additional cleanup.
	 */
	@Override
	protected void handleDispose(final DisposeEvent event) {
		if (BEXView.INSTANCE != null) {
			BEXView.INSTANCE.setMergeViewer(null);
		}

		OperationHistoryFactory.getOperationHistory().removeOperationHistoryListener(this.operationHistoryListener);

		if (this.fHandlerService != null) {
			this.fHandlerService.dispose();
		}

		Object input = this.getInput();
		this.removeFromDocumentManager(ANCESTOR_CONTRIBUTOR, input);
		this.removeFromDocumentManager(LEFT_CONTRIBUTOR, input);
		this.removeFromDocumentManager(RIGHT_CONTRIBUTOR, input);

		if (DEBUG) {
			DocumentManager.dump();
		}

		if (this.fPreferenceChangeListener != null) {
			JFaceResources.getFontRegistry().removeListener(this.fPreferenceChangeListener);
			JFaceResources.getColorRegistry().removeListener(this.fPreferenceChangeListener);
			if (this.fPreferenceStore != null) {
				this.fPreferenceStore.removePropertyChangeListener(this.fPreferenceChangeListener);
			}
			this.fPreferenceChangeListener = null;
		}

		this.fLeftCanvas = null;
		this.fRightCanvas = null;
		this.fVScrollBar = null;
		this.fBirdsEyeCanvas = null;
		this.fSummaryHeader = null;

		this.fAncestorContributor.unsetDocument(this.fAncestor);
		this.fLeftContributor.unsetDocument(this.fLeft);
		this.fRightContributor.unsetDocument(this.fRight);

		this.disconnect(this.fLeftContributor);
		this.disconnect(this.fRightContributor);
		this.disconnect(this.fAncestorContributor);

		if (this.showWhitespaceAction != null) {
			this.showWhitespaceAction.dispose();
		}

		if (this.toggleLineNumbersAction != null) {
			this.toggleLineNumbersAction.dispose();
		}

		if (this.fIgnoreWhitespace != null) {
			this.fIgnoreWhitespace.dispose();
		}

		this.getCompareConfiguration()
				.setProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING,
						Boolean.TRUE);
		this.disposeCompareFilterActions(false);

		if (this.fSourceViewerDecorationSupport != null) {
			for (SourceViewerDecorationSupport sourceViewerDecorationSupport : this.fSourceViewerDecorationSupport) {
				sourceViewerDecorationSupport.dispose();
			}
			this.fSourceViewerDecorationSupport = null;
		}

		if (this.fAncestor != null) {
			this.fAncestor.dispose();
		}
		this.fAncestor = null;
		if (this.fLeft != null) {
			this.fLeft.dispose();
		}
		this.fLeft = null;
		if (this.fRight != null) {
			this.fRight.dispose();
		}
		this.fRight = null;

		if (this.fColors != null) {
			Iterator<Color> i = this.fColors.values().iterator();
			while (i.hasNext()) {
				Color color = i.next();
				if (!color.isDisposed()) {
					color.dispose();
				}
			}
			this.fColors = null;
		}
		// don't add anything here, disposing colors should be done last
		super.handleDispose(event);
	}

	private void disconnect(final ContributorInfo legInfo) {
		if (legInfo != null) {
			legInfo.disconnect();
		}
	}

	//-------------------------------------------------------------------------------------------------------------
	//--- internal ------------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------------------------

	/*
	 * Creates the specific SWT controls for the content areas.
	 * Clients must not call or override this method.
	 */
	@Override
	protected void createControls(final Composite composite) {
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ICompareContextIds.TEXT_MERGE_VIEW);
		}

		// 1st row
		if (this.fMarginWidth > 0) {
			this.fAncestorCanvas = new BufferedCanvas(composite, SWT.NONE) {
				@Override
				public void doPaint(final GC gc) {
					TextMergeViewer.this.paintSides(gc, TextMergeViewer.this.fAncestor,
							TextMergeViewer.this.fAncestorCanvas, false);
				}
			};
			this.fAncestorCanvas.addMouseListener(
					new MouseAdapter() {
						@Override
						public void mouseDown(final MouseEvent e) {
							TextMergeViewer.this.setCurrentDiff2(TextMergeViewer.this.handleMouseInSides(
									TextMergeViewer.this.fAncestorCanvas, TextMergeViewer.this.fAncestor, e.y), false);
						}
					});
		}

		this.fAncestor = this.createPart(composite);
		this.setEditable(this.fAncestor.getSourceViewer(), false);
		this.fAncestor.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(final AccessibleEvent e) {
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_ancestor,
						TextMergeViewer.this.getCompareConfiguration()
								.getAncestorLabel(TextMergeViewer.this.getInput()));
			}
		});
		this.fAncestor.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(this.fAncestor));

		this.fSummaryHeader = new Canvas(composite, SWT.NONE);
		this.fHeaderPainter = new HeaderPainter();
		this.fSummaryHeader.addPaintListener(this.fHeaderPainter);
		this.updateResolveStatus();

		// 2nd row
		if (this.fMarginWidth > 0) {
			this.fLeftCanvas = new BufferedCanvas(composite, SWT.NONE) {
				@Override
				public void doPaint(final GC gc) {
					TextMergeViewer.this.paintSides(gc, TextMergeViewer.this.fLeft, TextMergeViewer.this.fLeftCanvas,
							false);
				}
			};
			this.fLeftCanvas.addMouseListener(
					new MouseAdapter() {
						@Override
						public void mouseDown(final MouseEvent e) {
							TextMergeViewer.this.setCurrentDiff2(TextMergeViewer.this.handleMouseInSides(
									TextMergeViewer.this.fLeftCanvas, TextMergeViewer.this.fLeft, e.y), false);
						}
					});
		}

		this.fLeft = this.createPart(composite);
		this.fLeft.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!this.fSynchronizedScrolling);
		this.fLeft.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(final AccessibleEvent e) {
				// Check for Mirrored status flag before returning the left label's text.
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_left,
						TextMergeViewer.this.getControl().getData(CompareUI.COMPARE_VIEWER_TITLE),
						TextMergeViewer.this.getCompareConfiguration().isMirrored()
								? TextMergeViewer.this.getCompareConfiguration()
										.getRightLabel(TextMergeViewer.this.getInput())
								: TextMergeViewer.this.getCompareConfiguration()
										.getLeftLabel(TextMergeViewer.this.getInput()));
			}
		});
		this.fLeft.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(this.fLeft));

		this.fRight = this.createPart(composite);
		this.fRight.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!this.fSynchronizedScrolling);
		this.fRight.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(final AccessibleEvent e) {
				// Check for Mirrored status flag before returning the right label's text.
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_right,
						TextMergeViewer.this.getControl().getData(CompareUI.COMPARE_VIEWER_TITLE),
						TextMergeViewer.this.getCompareConfiguration().isMirrored()
								? TextMergeViewer.this.getCompareConfiguration()
										.getLeftLabel(TextMergeViewer.this.getInput())
								: TextMergeViewer.this.getCompareConfiguration()
										.getRightLabel(TextMergeViewer.this.getInput()));
			}
		});
		this.fRight.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(this.fRight));

		IWorkbenchPart part = this.getCompareConfiguration().getContainer().getWorkbenchPart();
		// part is not available for contexts different than editor
		if (part != null) {
			ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
			if (selectionProvider instanceof CompareEditorSelectionProvider) {
				CompareEditorSelectionProvider cesp = (CompareEditorSelectionProvider) selectionProvider;
				SourceViewer focusSourceViewer = this.fFocusPart == null ? null : this.fFocusPart.getSourceViewer();
				cesp.setViewers(new SourceViewer[] { this.fLeft.getSourceViewer(), this.fRight.getSourceViewer(),
						this.fAncestor.getSourceViewer() }, focusSourceViewer);
			}
		}

		this.hsynchViewport(this.fAncestor.getSourceViewer(), this.fLeft.getSourceViewer(),
				this.fRight.getSourceViewer());
		this.hsynchViewport(this.fLeft.getSourceViewer(), this.fAncestor.getSourceViewer(),
				this.fRight.getSourceViewer());
		this.hsynchViewport(this.fRight.getSourceViewer(), this.fAncestor.getSourceViewer(),
				this.fLeft.getSourceViewer());

		if (this.fMarginWidth > 0) {
			this.fRightCanvas = new BufferedCanvas(composite, SWT.NONE) {
				@Override
				public void doPaint(final GC gc) {
					TextMergeViewer.this.paintSides(gc, TextMergeViewer.this.fRight, TextMergeViewer.this.fRightCanvas,
							TextMergeViewer.this.fSynchronizedScrolling);
				}
			};
			this.fRightCanvas.addMouseListener(
					new MouseAdapter() {
						@Override
						public void mouseDown(final MouseEvent e) {
							TextMergeViewer.this.setCurrentDiff2(TextMergeViewer.this.handleMouseInSides(
									TextMergeViewer.this.fRightCanvas, TextMergeViewer.this.fRight, e.y), false);
						}
					});
		}

		this.fScrollCanvas = new Canvas(composite, SWT.V_SCROLL);
		Rectangle trim = this.fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
		this.fTopInset = trim.y;

		this.fVScrollBar = this.fScrollCanvas.getVerticalBar();
		this.fVScrollBar.setIncrement(1);
		this.fVScrollBar.setVisible(true);
		this.fVScrollBar.addListener(SWT.Selection,
				e -> {
					int vpos = ((ScrollBar) e.widget).getSelection();
					this.synchronizedScrollVertical(vpos);
				});

		this.fBirdsEyeCanvas = new BufferedCanvas(composite, SWT.NONE) {
			@Override
			public void doPaint(final GC gc) {
				TextMergeViewer.this.updateVScrollBar(); // Update scroll bar here as initially viewport height is wrong
				TextMergeViewer.this.paintBirdsEyeView(this, gc);
			}
		};
		this.fBirdsEyeCanvas.addMouseListener(
				new MouseAdapter() {
					@Override
					public void mouseDown(final MouseEvent e) {
						TextMergeViewer.this.setCurrentDiff2(TextMergeViewer.this
								.handlemouseInBirdsEyeView(TextMergeViewer.this.fBirdsEyeCanvas, e.y), true);
					}
				});
		this.fBirdsEyeCanvas.addMouseMoveListener(
				new MouseMoveListener() {
					private Cursor fLastCursor;

					@Override
					public void mouseMove(final MouseEvent e) {
						Cursor cursor = null;
						Diff diff = TextMergeViewer.this.handlemouseInBirdsEyeView(TextMergeViewer.this.fBirdsEyeCanvas,
								e.y);
						if (diff != null && diff.getKind() != RangeDifference.NOCHANGE) {
							cursor = e.widget.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
						}
						if (this.fLastCursor != cursor) {
							TextMergeViewer.this.fBirdsEyeCanvas.setCursor(cursor);
							this.fLastCursor = cursor;
						}
					}
				});

		IWorkbenchPart workbenchPart = this.getCompareConfiguration().getContainer().getWorkbenchPart();
		if (workbenchPart != null) {
			IContextService service = workbenchPart.getSite().getService(IContextService.class);
			if (service != null) {
				service.activateContext("org.eclipse.ui.textEditorScope"); //$NON-NLS-1$
			}
		}
	}

	private void hsynchViewport(final TextViewer tv1, final TextViewer tv2, final TextViewer tv3) {
		final StyledText st1 = tv1.getTextWidget();
		final StyledText st2 = tv2.getTextWidget();
		final StyledText st3 = tv3.getTextWidget();
		final ScrollBar sb1 = st1.getHorizontalBar();
		sb1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (TextMergeViewer.this.fSynchronizedScrolling) {
					int v = sb1.getSelection();
					if (st2.isVisible()) {
						st2.setHorizontalPixel(v);
					}
					if (st3.isVisible()) {
						st3.setHorizontalPixel(v);
					}
				}
			}
		});
	}

	private void setCurrentDiff2(final Diff diff, final boolean reveal) {
		if (diff != null && diff.getKind() != RangeDifference.NOCHANGE) {
			//fCurrentDiff= null;
			this.setCurrentDiff(diff, reveal);
		}
	}

	private Diff handleMouseInSides(final Canvas canvas, final MergeSourceViewer tp, final int my) {

		int lineHeight = tp.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight = tp.getViewportHeight();

		if (!this.fHighlightRanges) {
			return null;
		}

		if (this.fMerger.hasChanges()) {
			int shift = tp.getVerticalScrollOffset() + (2 - LW);

			Point region = new Point(0, 0);
			char leg = this.getLeg(tp);
			for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted()) {
					continue;
				}

				if (this.fShowCurrentOnly2 && !this.isCurrentDiff(diff)) {
					continue;
				}

				tp.getLineRange(diff.getPosition(leg), region);
				int y = (region.x * lineHeight) + shift;
				int h = region.y * lineHeight;

				if (y + h < 0) {
					continue;
				}
				if (y >= visibleHeight) {
					break;
				}

				if (my >= y && my < y + h) {
					return diff;
				}
			}
		}
		return null;
	}

	private Diff getDiffUnderMouse(final Canvas canvas, final int mx, final int my, final Rectangle r) {

		if (!this.fSynchronizedScrolling) {
			return null;
		}

		int lineHeight = this.fLeft.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight = this.fRight.getViewportHeight();

		Point size = canvas.getSize();
		int w = size.x;

		if (!this.fHighlightRanges) {
			return null;
		}

		if (this.fMerger.hasChanges()) {
			int lshift = this.fLeft.getVerticalScrollOffset();
			int rshift = this.fRight.getVerticalScrollOffset();

			Point region = new Point(0, 0);

			for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted()) {
					continue;
				}

				if (this.fShowCurrentOnly2 && !this.isCurrentDiff(diff)) {
					continue;
				}

				this.fLeft.getLineRange(diff.getPosition(LEFT_CONTRIBUTOR), region);
				int ly = (region.x * lineHeight) + lshift;
				int lh = region.y * lineHeight;

				this.fRight.getLineRange(diff.getPosition(RIGHT_CONTRIBUTOR), region);
				int ry = (region.x * lineHeight) + rshift;
				int rh = region.y * lineHeight;

				if (Math.max(ly + lh, ry + rh) < 0) {
					continue;
				}
				if (Math.min(ly, ry) >= visibleHeight) {
					break;
				}

				int SIZE = 20;
				int cx = (w - SIZE) / 2;
				int cy = ((ly + lh / 2) + (ry + rh / 2) - SIZE) / 2;
				if (my >= cy && my < cy + SIZE && mx >= cx && mx < cx + SIZE) {
					if (r != null) {
						r.x = cx;
						r.y = cy;
						r.width = SIZE;
						r.height = SIZE;
					}
					return diff;
				}
			}
		}
		return null;
	}

	private Diff handlemouseInBirdsEyeView(final Canvas canvas, final int my) {
		return this.fMerger.findDiff(this.getViewportHeight(), this.fSynchronizedScrolling, canvas.getSize(), my);
	}

	private void paintBirdsEyeView(final Canvas canvas, final GC gc) {

		Color c;
		Rectangle r = new Rectangle(0, 0, 0, 0);
		int yy, hh;

		Point size = canvas.getSize();

		int virtualHeight = this.fSynchronizedScrolling ? this.fMerger.getVirtualHeight()
				: this.fMerger.getRightHeight();
		if (virtualHeight < this.getViewportHeight()) {
			return;
		}

		Display display = canvas.getDisplay();
		int y = 0;
		boolean allOutgoing = this.allOutgoing();
		for (Iterator<Diff> iterator = this.fMerger.rangesIterator(); iterator.hasNext();) {
			Diff diff = iterator.next();
			int h = this.fSynchronizedScrolling ? diff.getMaxDiffHeight()
					: diff.getRightHeight();

			if (this.fMerger.useChange(diff)) {

				yy = (y * size.y) / virtualHeight;
				hh = (h * size.y) / virtualHeight;
				if (hh < 3) {
					hh = 3;
				}

				c = this.getColor(display, this.getFillColor(diff, allOutgoing));
				if (c != null) {
					gc.setBackground(c);
					gc.fillRectangle(BIRDS_EYE_VIEW_INSET, yy, size.x - (2 * BIRDS_EYE_VIEW_INSET), hh);
				}
				c = this.getColor(display, this.getStrokeColor(diff, allOutgoing));
				if (c != null) {
					gc.setForeground(c);
					r.x = BIRDS_EYE_VIEW_INSET;
					r.y = yy;
					r.width = size.x - (2 * BIRDS_EYE_VIEW_INSET) - 1;
					r.height = hh;
					if (this.isCurrentDiff(diff)) {
						gc.setLineWidth(2);
						r.x++;
						r.y++;
						r.width--;
						r.height--;
					} else {
						gc.setLineWidth(0 /* 1 */);
					}
					gc.drawRectangle(r);
				}
			}

			y += h;
		}
	}

	private void refreshBirdsEyeView() {
		if (this.fBirdsEyeCanvas != null) {
			this.fBirdsEyeCanvas.redraw();
		}
	}

	/**
	 * Override to give focus to the pane that previously had focus or to a suitable
	 * default pane.
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleSetFocus()
	 * @since 3.3
	 */
	@Override
	protected boolean handleSetFocus() {
		if (this.fRedoDiff) {
			new UIJob(CompareMessages.DocumentMerger_0) {
				@Override
				public IStatus runInUIThread(final IProgressMonitor monitor) {
					TextMergeViewer.this.update(true);
					TextMergeViewer.this.updateStructure();
					return Status.OK_STATUS;
				}
			}.schedule();
			this.fRedoDiff = false;
		}
		if (this.fFocusPart == null) {
			if (this.fLeft != null && this.fLeft.getEnabled()) {
				this.fFocusPart = this.fLeft;
			} else if (this.fRight != null && this.fRight.getEnabled()) {
				this.fFocusPart = this.fRight;
			} else if (this.fAncestor != null && this.fAncestor.getEnabled()) {
				this.fFocusPart = this.fAncestor;
			}
		}
		if (this.fFocusPart != null) {
			StyledText st = this.fFocusPart.getSourceViewer().getTextWidget();
			if (st != null) {
				return st.setFocus();
			}
		}
		return false; // could not set focus
	}

	class HoverResizer extends Resizer {
		Canvas fCanvas;

		public HoverResizer(final Canvas c, final int dir) {
			super(c, dir);
			this.fCanvas = c;
		}

		@Override
		public void mouseMove(final MouseEvent e) {
			if (!this.fIsDown && TextMergeViewer.this.fUseSingleLine && TextMergeViewer.this.isAnySideEditable()
					&& TextMergeViewer.this.handleMouseMoveOverCenter(this.fCanvas, e.x, e.y)) {
				return;
			}
			super.mouseMove(e);
		}
	}

	@Override
	protected final Control createCenterControl(final Composite parent) {
		if (this.fSynchronizedScrolling) {
			final Canvas canvas = new BufferedCanvas(parent, SWT.NONE) {
				@Override
				public void doPaint(final GC gc) {
					TextMergeViewer.this.paintCenter(this, gc);
				}
			};
			new HoverResizer(canvas, HORIZONTAL);

			Cursor normalCursor = canvas.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
			int style = this.fIsMac ? SWT.FLAT : SWT.PUSH;

			this.fLeftToRightButton = new Button(canvas, style);
			this.fLeftToRightButton.setCursor(normalCursor);
			this.fLeftToRightButton.setText(COPY_LEFT_TO_RIGHT_INDICATOR);
			this.fLeftToRightButton.setToolTipText(
					Utilities.getString(this.getResourceBundle(), "action.CopyDiffLeftToRight.tooltip")); //$NON-NLS-1$
			this.fLeftToRightButton.pack();
			this.fLeftToRightButton.setVisible(false);
			this.fLeftToRightButton.addSelectionListener(
					new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							TextMergeViewer.this.handleCenterButtonSelection(true);
						}
					});

			this.fRightToLeftButton = new Button(canvas, style);
			this.fRightToLeftButton.setCursor(normalCursor);
			this.fRightToLeftButton.setText(COPY_RIGHT_TO_LEFT_INDICATOR);
			this.fRightToLeftButton.setToolTipText(
					Utilities.getString(this.getResourceBundle(), "action.CopyDiffRightToLeft.tooltip")); //$NON-NLS-1$
			this.fRightToLeftButton.pack();
			this.fRightToLeftButton.setVisible(false);
			this.fRightToLeftButton.addSelectionListener(
					new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							TextMergeViewer.this.handleCenterButtonSelection(false);
						}
					});

			return canvas;
		}
		return super.createCenterControl(parent);
	}

	private void handleCenterButtonSelection(final boolean leftToRight) {
		this.fLeftToRightButton.setVisible(false);
		this.fRightToLeftButton.setVisible(false);
		if (this.fButtonDiff != null) {
			this.setCurrentDiff(this.fButtonDiff, false);
			this.copy(this.fCurrentDiff, leftToRight, false);
		}
	}

	private boolean handleMouseMoveOverCenter(final Canvas canvas, final int x, final int y) {
		Rectangle r = new Rectangle(0, 0, 0, 0);
		Diff diff = this.getDiffUnderMouse(canvas, x, y, r);
		if (diff != this.fButtonDiff) {
			if (diff != null) {
				this.fButtonDiff = diff;
				boolean leftEditable = this.fLeft.getSourceViewer().isEditable();
				boolean rightEditable = this.fRight.getSourceViewer().isEditable();
				if (leftEditable && rightEditable) {
					int height = r.height;
					int leftToRightY = r.y - height / 2;
					int rightToLeftY = leftToRightY + height;
					Rectangle bounds = canvas.getBounds();
					if (leftToRightY < 0) {
						// button must not be hidden at top
						leftToRightY = 0;
						rightToLeftY = height;
					} else if (rightToLeftY + height > bounds.height) {
						// button must not be hidden at bottom
						leftToRightY = bounds.height - height - height;
						rightToLeftY = leftToRightY + height;
					}
					Rectangle leftToRightBounds = new Rectangle(r.x, leftToRightY, r.width, r.height);
					this.fLeftToRightButton.setBounds(leftToRightBounds);
					this.fLeftToRightButton.setVisible(true);
					Rectangle rightToLeftBounds = new Rectangle(r.x, rightToLeftY, r.width, r.height);
					this.fRightToLeftButton.setBounds(rightToLeftBounds);
					this.fRightToLeftButton.setVisible(true);
				} else if (leftEditable) {
					this.fRightToLeftButton.setBounds(r);
					this.fRightToLeftButton.setVisible(true);
					this.fLeftToRightButton.setVisible(false);
				} else if (rightEditable) {
					this.fLeftToRightButton.setBounds(r);
					this.fLeftToRightButton.setVisible(true);
					this.fRightToLeftButton.setVisible(false);
				} else {
					this.fButtonDiff = null;
				}
			} else {
				this.fRightToLeftButton.setVisible(false);
				this.fLeftToRightButton.setVisible(false);
				this.fButtonDiff = null;
			}
		}
		return this.fButtonDiff != null;
	}

	@Override
	protected final int getCenterWidth() {
		if (this.fSynchronizedScrolling) {
			return CENTER_WIDTH;
		}
		return super.getCenterWidth();
	}

	private int getDirection() {
		switch (this.fTextDirection) {
		case SWT.LEFT_TO_RIGHT:
		case SWT.RIGHT_TO_LEFT:
			if (this.fInheritedDirection == this.fTextDirection) {
				return SWT.NONE;
			}
			return this.fTextDirection;
		default:
			return this.fInheritedDirection;
		}
	}

	/**
	 * Creates a new source viewer. This method is called when creating and
	 * initializing the content areas of the merge viewer (see
	 * {@link #createControls(Composite)}). It is called three
	 * times for each text part of the comparison: ancestor, left, right.
	 * Clients may implement to provide their own type of source viewers. The
	 * viewer is not expected to be configured with a source viewer
	 * configuration.
	 *
	 * @param parent
	 *            the parent of the viewer's control
	 * @param textOrientation
	 *            style constant bit for text orientation
	 * @return Default implementation returns an instance of
	 *         {@link SourceViewer}.
	 * @since 3.5
	 */
	protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
		return new SourceViewer(parent, new CompositeRuler(), textOrientation | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	/**
	 * Tells whether the given text viewer is backed by an editor.
	 *
	 * @param textViewer the text viewer to check
	 * @return <code>true</code> if the viewer is backed by an editor
	 * @since 3.5
	 */
	protected boolean isEditorBacked(final ITextViewer textViewer) {
		return false;
	}

	/**
	 * Returns an editor input for the given source viewer. The method returns
	 * <code>null</code> when no input is available, for example when the input
	 * for the merge viewer has not been set yet.
	 *
	 * @param sourceViewer
	 *            the source viewer to get input for
	 * @return input for the given viewer or <code>null</code> when no input is
	 *         available
	 *
	 * @since 3.5
	 */
	protected IEditorInput getEditorInput(final ISourceViewer sourceViewer) {
		if (this.fLeft != null && sourceViewer == this.fLeft.getSourceViewer()) {
			if (this.fLeftContributor != null) {
				return this.fLeftContributor.getDocumentKey();
			}
		}
		if (this.fRight != null && sourceViewer == this.fRight.getSourceViewer()) {
			if (this.fRightContributor != null) {
				return this.fRightContributor.getDocumentKey();
			}
		}
		if (this.fAncestor != null
				&& sourceViewer == this.fAncestor.getSourceViewer()) {
			if (this.fAncestorContributor != null) {
				return this.fAncestorContributor.getDocumentKey();
			}
		}
		return null;
	}

	/*
	 * Creates and initializes a text part.
	 */
	private MergeSourceViewer createPart(final Composite parent) {
		final MergeSourceViewer viewer = new MergeSourceViewer(
				this.createSourceViewer(parent, this.getDirection()),
				this.getResourceBundle(), this.getCompareConfiguration().getContainer());
		final StyledText te = viewer.getSourceViewer().getTextWidget();

		if (!this.fConfirmSave) {
			viewer.hideSaveAction();
		}

		te.addPaintListener(
				e -> this.paint(e, viewer));
		te.addKeyListener(
				new KeyAdapter() {
					@Override
					public void keyPressed(final KeyEvent e) {
						TextMergeViewer.this.handleSelectionChanged(viewer);
					}
				});
		te.addMouseListener(
				new MouseAdapter() {
					@Override
					public void mouseDown(final MouseEvent e) {
						//syncViewport(part);
						TextMergeViewer.this.handleSelectionChanged(viewer);
					}
				});

		te.addFocusListener(
				new FocusAdapter() {
					@Override
					public void focusGained(final FocusEvent fe) {
						TextMergeViewer.this.setActiveViewer(viewer, true);
					}

					@Override
					public void focusLost(final FocusEvent fe) {
						TextMergeViewer.this.setActiveViewer(viewer, false);
					}
				});

		viewer.getSourceViewer()
				.addViewportListener(
						verticalPosition -> this.syncViewport(viewer));

		Font font = JFaceResources.getFont(this.fSymbolicFontName);
		if (font != null) {
			te.setFont(font);
		}

		if (this.fBackground != null) {
			te.setBackground(this.getColor(parent.getDisplay(), this.fBackground));
		}

		// Add the find action to the popup menu of the viewer
		this.contributeFindAction(viewer);

		this.contributeGotoLineAction(viewer);

		this.contributeChangeEncodingAction(viewer);

		// showWhiteSpaceAction is added in createToolItems when fAncestor, fLeft and
		// fRight are initialized

		this.contributeDiffBackgroundListener(viewer);

		return viewer;
	}

	private void setActiveViewer(final MergeSourceViewer viewer, final boolean activate) {
		this.connectContributedActions(viewer, activate);
		if (activate) {
			this.fFocusPart = viewer;
			this.connectGlobalActions(this.fFocusPart);
		} else {
			this.connectGlobalActions(null);
		}
	}

	private SourceViewerDecorationSupport getSourceViewerDecorationSupport(final ISourceViewer viewer) {
		SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(viewer, null, null,
				EditorsUI.getSharedTextColors());
		support.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
		this.fSourceViewerDecorationSupport.add(support);
		return support;
	}

	private void contributeFindAction(final MergeSourceViewer viewer) {
		IAction action;
		IWorkbenchPart wp = this.getCompareConfiguration().getContainer().getWorkbenchPart();
		if (wp != null) {
			action = new FindReplaceAction(this.getResourceBundle(), "Editor.FindReplace.", wp); //$NON-NLS-1$
		} else {
			action = new FindReplaceAction(this.getResourceBundle(), "Editor.FindReplace.", //$NON-NLS-1$
					viewer.getSourceViewer().getControl().getShell(), this.getFindReplaceTarget());
		}
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		viewer.addAction(MergeSourceViewer.FIND_ID, action);
	}

	private void contributeGotoLineAction(final MergeSourceViewer viewer) {
		IAction action = new GotoLineAction(viewer.getAdapter(ITextEditor.class));
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
		viewer.addAction(MergeSourceViewer.GOTO_LINE_ID, action);
	}

	private void contributeChangeEncodingAction(final MergeSourceViewer viewer) {
		IAction action = new ChangeEncodingAction(this.getTextEditorAdapter());
		viewer.addAction(MergeSourceViewer.CHANGE_ENCODING_ID, action);
	}

	private void contributeDiffBackgroundListener(final MergeSourceViewer viewer) {
		viewer.getSourceViewer()
				.getTextWidget()
				.addLineBackgroundListener(
						event -> {
							StyledText textWidget = viewer.getSourceViewer().getTextWidget();
							if (textWidget != null) {

								int caret = textWidget.getCaretOffset();
								int length = event.lineText.length();

								if (event.lineOffset <= caret
										&& caret <= event.lineOffset + length) {
									// current line, do nothing
									// decorated by CursorLinePainter
								} else {
									// find diff for the event line
									Diff diff = this.findDiff(viewer, event.lineOffset,
											event.lineOffset + length);
									if (diff != null && this.updateDiffBackground(diff)) {
										// highlights only the event line, not the
										// whole diff
										event.lineBackground = this.getColor(this.fComposite
												.getDisplay(), this.getFillColor(diff, this.allOutgoing()));
									}
								}
							}
						});
	}

	private void connectGlobalActions(final MergeSourceViewer part) {
		if (this.fHandlerService != null) {
			if (part != null) {
				part.updateActions();
			}
			this.fHandlerService.updatePaneActionHandlers(() -> {
				for (int i = 0; i < GLOBAL_ACTIONS.length; i++) {
					IAction action = null;
					if (part != null) {
						action = part.getAction(TEXT_ACTIONS[i]);
					}
					this.fHandlerService.setGlobalActionHandler(GLOBAL_ACTIONS[i], action);
				}
			});
		}
	}

	private void connectContributedActions(final MergeSourceViewer viewer, final boolean connect) {
		if (this.fHandlerService != null) {
			this.fHandlerService.updatePaneActionHandlers(() -> {
				if (viewer != null) {
					this.setActionsActivated(viewer.getSourceViewer(), connect);
					if (this.isEditorBacked(viewer.getSourceViewer()) && connect) {
						/*
						 * If editor backed, activating contributed actions
						 * might have disconnected actions provided in
						 * CompareEditorContributor => when connecting,
						 * refresh active editor in the contributor, when
						 * disconnecting do nothing. See bug 261229.
						 */
						IWorkbenchPart part = this.getCompareConfiguration().getContainer().getWorkbenchPart();
						if (part instanceof CompareEditor) {
							((CompareEditor) part).refreshActionBarsContributor();
						}
					}
				}
			});
		}
	}

	/**
	 * Activates or deactivates actions of the given source viewer.
	 * <p>
	 * The default implementation does nothing, but clients should override to properly react to
	 * viewers switching.
	 * </p>
	 *
	 * @param sourceViewer the source viewer
	 * @param state <code>true</code> if activated
	 * @since 3.5
	 */
	protected void setActionsActivated(final SourceViewer sourceViewer, final boolean state) {
		// default implementation does nothing
	}

	private IDocument getElementDocument(final char type, final Object element) {
		if (element instanceof IDocument) {
			return (IDocument) element;
		}
		ITypedElement te = Utilities.getLeg(type, element);
		// First check the contributors for the document
		IDocument document = null;
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			document = this.getDocument(te, this.fAncestorContributor);
			break;
		case LEFT_CONTRIBUTOR:
			document = this.getDocument(te, this.fLeftContributor);
			break;
		case RIGHT_CONTRIBUTOR:
			document = this.getDocument(te, this.fRightContributor);
			break;
		default:
			break;
		}
		if (document != null) {
			return document;
		}
		// The document is not associated with the input of the viewer so try to find the document
		return Utilities.getDocument(type, element, this.isUsingDefaultContentProvider(), this.canHaveSharedDocument());
	}

	private boolean isUsingDefaultContentProvider() {
		return this.getContentProvider() instanceof MergeViewerContentProvider;
	}

	private boolean canHaveSharedDocument() {
		return this.getDocumentPartitioning() != null
				|| this.getDocumentPartitioner() == null;
	}

	private IDocument getDocument(final ITypedElement te, final ContributorInfo info) {
		if (info != null && info.getElement() == te) {
			return info.getDocument();
		}
		return null;
	}

	IDocument getDocument(final char type, final Object input) {
		IDocument doc = this.getElementDocument(type, input);
		if (doc != null) {
			return doc;
		}

		if (input instanceof IDiffElement) {
			IDiffContainer parent = ((IDiffElement) input).getParent();
			return this.getElementDocument(type, parent);
		}
		return null;
	}

	/*
	 * Returns true if the given inputs map to the same documents
	 */
	boolean sameDoc(final char type, final Object newInput, final Object oldInput) {
		IDocument newDoc = this.getDocument(type, newInput);
		IDocument oldDoc = this.getDocument(type, oldInput);
		return newDoc == oldDoc;
	}

	/**
	 * Overridden to prevent save confirmation if new input is sub document of current input.
	 * @param newInput the new input of this viewer, or <code>null</code> if there is no new input
	 * @param oldInput the old input element, or <code>null</code> if there was previously no input
	 * @return <code>true</code> if saving was successful, or if the user didn't want to save (by pressing 'NO' in the confirmation dialog).
	 * @since 2.0
	 */
	@Override
	protected boolean doSave(final Object newInput, final Object oldInput) {
		// TODO: Would be good if this could be restated in terms of Saveables and moved up
		if (oldInput != null && newInput != null) {
			// check whether underlying documents have changed.
			if (this.sameDoc(ANCESTOR_CONTRIBUTOR, newInput, oldInput) &&
					this.sameDoc(LEFT_CONTRIBUTOR, newInput, oldInput) &&
					this.sameDoc(RIGHT_CONTRIBUTOR, newInput, oldInput)) {
				if (DEBUG) {
					System.out.println("----- Same docs !!!!"); //$NON-NLS-1$
				}
				return false;
			}
		}

		if (DEBUG) {
			System.out.println("***** New docs !!!!"); //$NON-NLS-1$
		}

		this.removeFromDocumentManager(ANCESTOR_CONTRIBUTOR, oldInput);
		this.removeFromDocumentManager(LEFT_CONTRIBUTOR, oldInput);
		this.removeFromDocumentManager(RIGHT_CONTRIBUTOR, oldInput);

		if (DEBUG) {
			DocumentManager.dump();
		}

		return super.doSave(newInput, oldInput);
	}

	private void removeFromDocumentManager(final char leg, final Object oldInput) {
		IDocument document = this.getDocument(leg, oldInput);
		if (document != null) {
			DocumentManager.remove(document);
		}
	}

	private ITypedElement getParent(final char type) {
		Object input = this.getInput();
		if (input instanceof IDiffElement) {
			IDiffContainer parent = ((IDiffElement) input).getParent();
			return Utilities.getLeg(type, parent);
		}
		return null;
	}

	/*
	 * Initializes the text viewers of the three content areas with the given input objects.
	 * Subclasses may extend.
	 */
	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		boolean emptyInput = (ancestor == null && left == null && right == null);

		Object input = this.getInput();

		this.configureCompareFilterActions(input, ancestor, left, right);

		Position leftRange = null;
		Position rightRange = null;

		// if one side is empty use container
		if (FIX_47640 && !emptyInput && (left == null || right == null)) {
			if (input instanceof IDiffElement) {
				IDiffContainer parent = ((IDiffElement) input).getParent();
				if (parent instanceof ICompareInput) {
					ICompareInput ci = (ICompareInput) parent;

					if (ci.getAncestor() instanceof IDocumentRange
							|| ci.getLeft() instanceof IDocumentRange
							|| ci.getRight() instanceof IDocumentRange) {
						if (left instanceof IDocumentRange) {
							leftRange = ((IDocumentRange) left).getRange();
						}
						if (right instanceof IDocumentRange) {
							rightRange = ((IDocumentRange) right).getRange();
						}

						ancestor = ci.getAncestor();
						left = this.getCompareConfiguration().isMirrored() ? ci.getRight() : ci.getLeft();
						right = this.getCompareConfiguration().isMirrored() ? ci.getLeft() : ci.getRight();
					}
				}
			}
		}

		this.fHighlightRanges = left != null && right != null;

		this.resetDiffs();
		this.fHasErrors = false; // start with no errors

		IMergeViewerContentProvider cp = this.getMergeContentProvider();

		if (cp instanceof MergeViewerContentProvider) {
			MergeViewerContentProvider mcp = (MergeViewerContentProvider) cp;
			mcp.setAncestorError(null);
			mcp.setLeftError(null);
			mcp.setRightError(null);
		}

		// Record current contributors so we disconnect after creating the new ones.
		// This is done in case the old and new use the same document.
		ContributorInfo oldLeftContributor = this.fLeftContributor;
		ContributorInfo oldRightContributor = this.fRightContributor;
		ContributorInfo oldAncestorContributor = this.fAncestorContributor;

		// Create the new contributor
		this.fLeftContributor = this.createLegInfoFor(left, LEFT_CONTRIBUTOR);
		this.fRightContributor = this.createLegInfoFor(right, RIGHT_CONTRIBUTOR);
		this.fAncestorContributor = this.createLegInfoFor(ancestor, ANCESTOR_CONTRIBUTOR);

		this.fLeftContributor.transferContributorStateFrom(oldLeftContributor);
		this.fRightContributor.transferContributorStateFrom(oldRightContributor);
		this.fAncestorContributor.transferContributorStateFrom(oldAncestorContributor);

		// Now disconnect the old ones
		this.disconnect(oldLeftContributor);
		this.disconnect(oldRightContributor);
		this.disconnect(oldAncestorContributor);

		// Get encodings from streams. If an encoding is null, abide by the other one
		// Defaults to workbench encoding only if both encodings are null
		this.fLeftContributor.setEncodingIfAbsent(this.fRightContributor);
		this.fRightContributor.setEncodingIfAbsent(this.fLeftContributor);
		this.fAncestorContributor.setEncodingIfAbsent(this.fLeftContributor);

		if (!this.isConfigured) {
			this.configureSourceViewer(this.fAncestor.getSourceViewer(), false, null);
			this.configureSourceViewer(this.fLeft.getSourceViewer(), this.isLeftEditable() && cp.isLeftEditable(input),
					this.fLeftContributor);
			this.configureSourceViewer(this.fRight.getSourceViewer(),
					this.isRightEditable() && cp.isRightEditable(input), this.fRightContributor);
			this.isConfigured = true; // configure once
		}

		// set new documents
		this.fLeftContributor.setDocument(this.fLeft, this.isLeftEditable() && cp.isLeftEditable(input));
		this.fLeftLineCount = this.fLeft.getLineCount();

		this.fRightContributor.setDocument(this.fRight, this.isRightEditable() && cp.isRightEditable(input));
		this.fRightLineCount = this.fRight.getLineCount();

		this.fAncestorContributor.setDocument(this.fAncestor, false);

		this.setSyncScrolling(this.fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING));

		this.update(false);

		if (!this.fHasErrors && !emptyInput && !this.fComposite.isDisposed()) {
			if (this.isRefreshing()) {
				this.fLeftContributor.updateSelection(this.fLeft, !this.fSynchronizedScrolling);
				this.fRightContributor.updateSelection(this.fRight, !this.fSynchronizedScrolling);
				this.fAncestorContributor.updateSelection(this.fAncestor, !this.fSynchronizedScrolling);
				if (this.fSynchronizedScrolling && this.fSynchronziedScrollPosition != -1) {
					this.synchronizedScrollVertical(this.fSynchronziedScrollPosition);
				}
			} else {
				if (this.isPatchHunk()) {
					if (right != null && Adapters.adapt(right, IHunk.class) != null) {
						this.fLeft.getSourceViewer().setTopIndex(this.getHunkStart());
					} else {
						this.fRight.getSourceViewer().setTopIndex(this.getHunkStart());
					}
				} else {
					Diff selectDiff = null;
					if (FIX_47640) {
						if (leftRange != null) {
							selectDiff = this.fMerger.findDiff(LEFT_CONTRIBUTOR, leftRange);
						} else if (rightRange != null) {
							selectDiff = this.fMerger.findDiff(RIGHT_CONTRIBUTOR, rightRange);
						}
					}
					if (selectDiff != null) {
						this.setCurrentDiff(selectDiff, true);
					} else {
						this.selectFirstDiff(true);
					}
				}
			}
		}

	}

	private void configureSourceViewer(final SourceViewer sourceViewer, final boolean editable,
			final ContributorInfo contributor) {
		this.setEditable(sourceViewer, editable);
		this.configureTextViewer(sourceViewer);
		if (editable && contributor != null) {
			IDocument document = sourceViewer.getDocument();
			if (document != null) {
				contributor.connectPositionUpdater(document);
			}
		}
		if (!this.isCursorLinePainterInstalled(sourceViewer)) {
			this.getSourceViewerDecorationSupport(sourceViewer).install(this.fPreferenceStore);
		}
	}

	private boolean isCursorLinePainterInstalled(final SourceViewer viewer) {
		Listener[] listeners = viewer.getTextWidget().getListeners(3001/*StyledText.LineGetBackground*/);
		for (Listener l : listeners) {
			if (l instanceof TypedListener) {
				TypedListener listener = (TypedListener) l;
				if (listener.getEventListener() instanceof CursorLinePainter) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Sets the editable state of the given source viewer.
	 *
	 * @param sourceViewer
	 *            the source viewer
	 * @param state
	 *            the state
	 * @since 3.5
	 */
	protected void setEditable(final ISourceViewer sourceViewer, final boolean state) {
		sourceViewer.setEditable(state);
	}

	private boolean isRefreshing() {
		return this.isRefreshing > 0;
	}

	private ContributorInfo createLegInfoFor(final Object element, final char leg) {
		return new ContributorInfo(this, element, leg);
	}

	private boolean updateDiffBackground(final Diff diff) {

		if (!this.fHighlightRanges) {
			return false;
		}

		if (diff == null || diff.isToken()) {
			return false;
		}

		if (this.fShowCurrentOnly && !this.isCurrentDiff(diff)) {
			return false;
		}

		return true;
	}

	/*
	 * Called whenever one of the documents changes.
	 * Sets the dirty state of this viewer and updates the lines.
	 * Implements IDocumentListener.
	 */
	private void documentChanged(final DocumentEvent e, final boolean dirty) {
		final IDocument doc = e.getDocument();

		if (doc == this.fLeft.getSourceViewer().getDocument()) {
			this.setLeftDirty(dirty);
		} else if (doc == this.fRight.getSourceViewer().getDocument()) {
			this.setRightDirty(dirty);
		}
		if (!this.isLeftDirty() && !this.isRightDirty()) {
			this.fRedoDiff = false;
			final Diff oldDiff = this.getLastDiff();
			new UIJob(CompareMessages.DocumentMerger_0) {
				@Override
				public IStatus runInUIThread(final IProgressMonitor monitor) {
					if (!TextMergeViewer.this.getControl().isDisposed()) {
						TextMergeViewer.this.doDiff();
						if (!TextMergeViewer.this.getControl().isDisposed()) {
							Diff newDiff = TextMergeViewer.this.findNewDiff(oldDiff);
							if (newDiff != null) {
								TextMergeViewer.this.updateStatus(newDiff);
								TextMergeViewer.this.setCurrentDiff(newDiff, true);
							}
							TextMergeViewer.this.invalidateLines();
							TextMergeViewer.this.updateLines(doc);
						}
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		} else {
			this.updateLines(doc);
		}
	}

	private void saveDiff() {
		this.fSavedDiff = this.fCurrentDiff;
	}

	private Diff getLastDiff() {
		if (this.fCurrentDiff != null) {
			return this.fCurrentDiff;
		}
		return this.fSavedDiff;
	}

	private Diff findNewDiff(final Diff oldDiff) {
		if (oldDiff == null) {
			return null;
		}
		Diff newDiff = this.findNewDiff(oldDiff, LEFT_CONTRIBUTOR);
		if (newDiff == null) {
			newDiff = this.findNewDiff(oldDiff, RIGHT_CONTRIBUTOR);
		}
		return newDiff;
	}

	private Diff findNewDiff(final Diff oldDiff, final char type) {
		int offset = oldDiff.getPosition(type).offset;
		int length = oldDiff.getPosition(type).length;

		// DocumentMerger.findDiff method doesn't really work well with 0-length
		// diffs
		if (length == 0) {
			if (offset > 0) {
				offset--;
			}
			length = 1;
		}
		return this.fMerger.findDiff(type, offset, offset + length);
	}

	/*
	 * This method is called if a range of text on one side is copied into an empty sub-document
	 * on the other side. The method returns the position where the sub-document is placed into the base document.
	 * This default implementation determines the position by using the text range differencer.
	 * However this position is not always optimal for specific types of text.
	 * So subclasses (which are aware of the type of text they are dealing with)
	 * may override this method to find a better position where to insert a newly added
	 * piece of text.
	 * @param type the side for which the insertion position should be determined: 'A' for ancestor, 'L' for left hand side, 'R' for right hand side.
	 * @param input the current input object of this viewer
	 * @since 2.0
	 */
	protected int findInsertionPosition(final char type, final ICompareInput input) {

		ITypedElement other = null;
		char otherType = 0;

		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			other = input.getLeft();
			otherType = LEFT_CONTRIBUTOR;
			if (other == null) {
				other = input.getRight();
				otherType = RIGHT_CONTRIBUTOR;
			}
			break;
		case LEFT_CONTRIBUTOR:
			other = input.getRight();
			otherType = RIGHT_CONTRIBUTOR;
			if (other == null) {
				other = input.getAncestor();
				otherType = ANCESTOR_CONTRIBUTOR;
			}
			break;
		case RIGHT_CONTRIBUTOR:
			other = input.getLeft();
			otherType = LEFT_CONTRIBUTOR;
			if (other == null) {
				other = input.getAncestor();
				otherType = ANCESTOR_CONTRIBUTOR;
			}
			break;
		default:
			break;
		}

		if (other instanceof IDocumentRange) {
			IDocumentRange dr = (IDocumentRange) other;
			Position p = dr.getRange();
			Diff diff = this.findDiff(otherType, p.offset);
			return this.fMerger.findInsertionPoint(diff, type);
		}
		return 0;
	}

	private void setError(final char type, final String message) {
		IMergeViewerContentProvider cp = this.getMergeContentProvider();
		if (cp instanceof MergeViewerContentProvider) {
			MergeViewerContentProvider mcp = (MergeViewerContentProvider) cp;
			switch (type) {
			case ANCESTOR_CONTRIBUTOR:
				mcp.setAncestorError(message);
				break;
			case LEFT_CONTRIBUTOR:
				mcp.setLeftError(message);
				break;
			case RIGHT_CONTRIBUTOR:
				mcp.setRightError(message);
				break;
			default:
				break;
			}
		}
		this.fHasErrors = true;
	}

	private void updateDirtyState(final IEditorInput key,
			final IDocumentProvider documentProvider, final char type) {
		boolean dirty = documentProvider.canSaveDocument(key);
		boolean oldLeftDirty = this.isLeftDirty();
		boolean oldRightDirty = this.isRightDirty();
		if (type == LEFT_CONTRIBUTOR) {
			this.setLeftDirty(dirty);
		} else if (type == RIGHT_CONTRIBUTOR) {
			this.setRightDirty(dirty);
		}
		if ((oldLeftDirty && !this.isLeftDirty())
				|| (oldRightDirty && !this.isRightDirty())) {
			/*
			 * Dirty state has changed from true to false, combined dirty state
			 * is false. _In most cases_ this means that save has taken place
			 * outside compare editor. Ask to redo diff calculation when the
			 * editor gets focus.
			 *
			 * However, undoing all the changes made in another editor would
			 * result in asking for redo diff as well. In this case, we set the
			 * flag back to false, see
			 * TextMergeViewer.documentChanged(DocumentEvent, boolean)
			 */
			this.fRedoDiff = true;
		}
	}

	private Position getNewRange(final char type, final Object input) {
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			return this.fNewAncestorRanges.get(input);
		case LEFT_CONTRIBUTOR:
			return this.fNewLeftRanges.get(input);
		case RIGHT_CONTRIBUTOR:
			return this.fNewRightRanges.get(input);
		default:
			return null;
		}
	}

	private void addNewRange(final char type, final Object input, final Position range) {
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			this.fNewAncestorRanges.put(input, range);
			break;
		case LEFT_CONTRIBUTOR:
			this.fNewLeftRanges.put(input, range);
			break;
		case RIGHT_CONTRIBUTOR:
			this.fNewRightRanges.put(input, range);
			break;
		default:
			break;
		}
	}

	/**
	 * Returns the contents of the underlying document as an array of bytes using the current workbench encoding.
	 *
	 * @param left if <code>true</code> the contents of the left side is returned; otherwise the right side
	 * @return the contents of the left or right document or null
	 */
	@Override
	protected byte[] getContents(final boolean left) {
		MergeSourceViewer v = left ? this.fLeft : this.fRight;
		if (v != null) {
			IDocument d = v.getSourceViewer().getDocument();
			if (d != null) {
				String contents = d.get();
				if (contents != null) {
					byte[] bytes;
					try {
						bytes = contents.getBytes(left ? this.fLeftContributor.internalGetEncoding()
								: this.fRightContributor.internalGetEncoding());
					} catch (UnsupportedEncodingException ex) {
						// use default encoding
						bytes = contents.getBytes();
					}
					return bytes;
				}
			}
		}
		return null;
	}

	private IRegion normalizeDocumentRegion(final IDocument doc, final IRegion region) {

		if (region == null || doc == null) {
			return region;
		}

		int maxLength = doc.getLength();

		int start = region.getOffset();
		if (start < 0) {
			start = 0;
		} else if (start > maxLength) {
			start = maxLength;
		}

		int length = region.getLength();
		if (length < 0) {
			length = 0;
		} else if (start + length > maxLength) {
			length = maxLength - start;
		}

		return new Region(start, length);
	}

	@Override
	protected final void handleResizeAncestor(int x, final int y, int width, final int height) {
		if (width > 0) {
			Rectangle trim = this.fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
			int scrollbarHeight = trim.height;
			if (Utilities.okToUse(this.fAncestorCanvas)) {
				this.fAncestorCanvas.setVisible(true);
			}
			if (this.fAncestor.isControlOkToUse()) {
				this.fAncestor.getSourceViewer().getTextWidget().setVisible(true);
			}

			if (this.fAncestorCanvas != null) {
				this.fAncestorCanvas.setBounds(x, y, this.fMarginWidth, height - scrollbarHeight);
				x += this.fMarginWidth;
				width -= this.fMarginWidth;
			}
			this.fAncestor.setBounds(x, y, width, height);
		} else {
			if (Utilities.okToUse(this.fAncestorCanvas)) {
				this.fAncestorCanvas.setVisible(false);
			}
			if (this.fAncestor.isControlOkToUse()) {
				StyledText t = this.fAncestor.getSourceViewer().getTextWidget();
				t.setVisible(false);
				this.fAncestor.setBounds(0, 0, 0, 0);
				if (this.fFocusPart == this.fAncestor) {
					this.fFocusPart = this.fLeft;
					this.fFocusPart.getSourceViewer().getTextWidget().setFocus();
				}
			}
		}
	}

	@Override
	protected final void handleResizeLeftRight(int x, int y, final int width1, final int centerWidth, int width2,
			final int height) {
		if (this.fBirdsEyeCanvas != null) {
			width2 -= BIRDS_EYE_VIEW_WIDTH;
		}

		Rectangle trim = this.fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
		int scrollbarHeight = trim.height + trim.x;

		Composite composite = (Composite) this.getControl();

		int leftTextWidth = width1;
		if (this.fLeftCanvas != null) {
			this.fLeftCanvas.setBounds(x, y, this.fMarginWidth, height - scrollbarHeight);
			x += this.fMarginWidth;
			leftTextWidth -= this.fMarginWidth;
		}

		this.fLeft.setBounds(x, y, leftTextWidth, height);
		x += leftTextWidth;

		if (this.fCenter == null || this.fCenter.isDisposed()) {
			this.fCenter = this.createCenterControl(composite);
		}
		this.fCenter.setBounds(x, y, centerWidth, height - scrollbarHeight);
		x += centerWidth;

		if (!this.fSynchronizedScrolling) { // canvas is to the left of text
			if (this.fRightCanvas != null) {
				this.fRightCanvas.setBounds(x, y, this.fMarginWidth, height - scrollbarHeight);
				this.fRightCanvas.redraw();
				x += this.fMarginWidth;
			}
			// we draw the canvas to the left of the text widget
		}

		int scrollbarWidth = 0;
		if (this.fSynchronizedScrolling && this.fScrollCanvas != null) {
			trim = this.fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
			// one pixel was cut off
			scrollbarWidth = trim.width + 2 * trim.x + 1;
		}
		int rightTextWidth = width2 - scrollbarWidth;
		if (this.fRightCanvas != null) {
			rightTextWidth -= this.fMarginWidth;
		}
		this.fRight.setBounds(x, y, rightTextWidth, height);
		x += rightTextWidth;

		if (this.fSynchronizedScrolling) {
			if (this.fRightCanvas != null) { // canvas is to the right of the text
				this.fRightCanvas.setBounds(x, y, this.fMarginWidth, height - scrollbarHeight);
				x += this.fMarginWidth;
			}
			if (this.fScrollCanvas != null) {
				this.fScrollCanvas.setBounds(x, y, scrollbarWidth, height - scrollbarHeight);
			}
		}

		if (this.fBirdsEyeCanvas != null) {
			int verticalScrollbarButtonHeight = scrollbarWidth;
			int horizontalScrollbarButtonHeight = scrollbarHeight;
			if (this.fIsMac) {
				verticalScrollbarButtonHeight += 2;
				horizontalScrollbarButtonHeight = 18;
			}
			if (this.fSummaryHeader != null) {
				this.fSummaryHeader.setBounds(x + scrollbarWidth, y, BIRDS_EYE_VIEW_WIDTH,
						verticalScrollbarButtonHeight);
			}
			y += verticalScrollbarButtonHeight;
			this.fBirdsEyeCanvas.setBounds(x + scrollbarWidth, y, BIRDS_EYE_VIEW_WIDTH,
					height - (2 * verticalScrollbarButtonHeight + horizontalScrollbarButtonHeight));
		}

		// doesn't work since TextEditors don't have their correct size yet.
		this.updateVScrollBar();
		this.refreshBirdsEyeView();
	}

	/*
	 * Track selection changes to update the current Diff.
	 */
	private void handleSelectionChanged(final MergeSourceViewer tw) {
		Point p = tw.getSourceViewer().getSelectedRange();
		Diff d = this.findDiff(tw, p.x, p.x + p.y);
		this.updateStatus(d);
		this.setCurrentDiff(d, false); // don't select or reveal
	}

	private static IRegion toRegion(final Position position) {
		if (position != null) {
			return new Region(position.getOffset(), position.getLength());
		}
		return null;
	}

	//---- the differencing

	/**
	 * Perform a two level 2- or 3-way diff.
	 * The first level is based on line comparison, the second level on token comparison.
	 */
	private void doDiff() {
		IDocument lDoc = this.fLeft.getSourceViewer().getDocument();
		IDocument rDoc = this.fRight.getSourceViewer().getDocument();
		//		System.out.println("Left document: " + lDoc);
		//		System.out.println("Right document: " + rDoc);
		if (lDoc == null || rDoc == null) {
			return;
		}
		this.fAncestor.resetLineBackground();
		this.fLeft.resetLineBackground();
		this.fRight.resetLineBackground();
		this.saveDiff();
		this.fCurrentDiff = null;
		try {
			this.fMerger.doDiff();
			// After diff, let BEXView know about this, so can update scroll position
			BEXView.INSTANCE.setMergeViewer(this);
		} catch (CoreException e) {
			CompareUIPlugin.log(e.getStatus());
			String title = Utilities.getString(this.getResourceBundle(), "tooComplexError.title"); //$NON-NLS-1$
			String msg = Utilities.getString(this.getResourceBundle(), "tooComplexError.message"); //$NON-NLS-1$
			MessageDialog.openError(this.fComposite.getShell(), title, msg);
		}

		this.invalidateTextPresentation();
	}

	private Diff findDiff(final char type, final int pos) {
		try {
			return this.fMerger.findDiff(type, pos);
		} catch (CoreException e) {
			CompareUIPlugin.log(e.getStatus());
			String title = Utilities.getString(this.getResourceBundle(), "tooComplexError.title"); //$NON-NLS-1$
			String msg = Utilities.getString(this.getResourceBundle(), "tooComplexError.message"); //$NON-NLS-1$
			MessageDialog.openError(this.fComposite.getShell(), title, msg);
			return null;
		}
	}

	private void resetPositions(final IDocument doc) {
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

	//---- update UI stuff

	private void updateControls() {
		if (this.getControl().isDisposed()) {
			return;
		}

		boolean leftToRight = false;
		boolean rightToLeft = false;

		this.updateStatus(this.fCurrentDiff);
		this.updateResolveStatus();

		if (this.fCurrentDiff != null) {
			IMergeViewerContentProvider cp = this.getMergeContentProvider();
			if (cp != null) {
				if (!this.isPatchHunk()) {
					rightToLeft = cp.isLeftEditable(this.getInput());
					leftToRight = cp.isRightEditable(this.getInput());
				}
			}
		}

		if (this.fDirectionLabel != null) {
			if (this.fHighlightRanges && this.fCurrentDiff != null && this.isThreeWay() && !this.isIgnoreAncestor()) {
				this.fDirectionLabel.setImage(this.fCurrentDiff.getImage());
			} else {
				this.fDirectionLabel.setImage(null);
			}
		}

		if (this.fCopyDiffLeftToRightItem != null) {
			this.fCopyDiffLeftToRightItem.getAction().setEnabled(leftToRight);
		}
		if (this.fCopyDiffRightToLeftItem != null) {
			this.fCopyDiffRightToLeftItem.getAction().setEnabled(rightToLeft);
		}

		if (this.fNextDiff != null) {
			IAction a = this.fNextDiff.getAction();
			a.setEnabled(this.isNavigationButtonEnabled(true, false));
		}
		if (this.fPreviousDiff != null) {
			IAction a = this.fPreviousDiff.getAction();
			a.setEnabled(this.isNavigationButtonEnabled(false, false));
		}
		if (this.fNextChange != null) {
			IAction a = this.fNextChange.getAction();
			a.setEnabled(this.isNavigationButtonEnabled(true, true));
		}
		if (this.fPreviousChange != null) {
			IAction a = this.fPreviousChange.getAction();
			a.setEnabled(this.isNavigationButtonEnabled(false, true));
		}
	}

	private boolean isNavigationButtonEnabled(final boolean down, final boolean deep) {
		String value = this.fPreferenceStore
				.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		if (value.equals(ICompareUIConstants.PREF_VALUE_DO_NOTHING)) {
			return this.getNextVisibleDiff(down, deep) != null;
		} else if (value.equals(ICompareUIConstants.PREF_VALUE_LOOP)) {
			return this.isNavigationPossible();
		} else if (value.equals(ICompareUIConstants.PREF_VALUE_NEXT)) {
			return this.getNextVisibleDiff(down, deep) != null || this.hasNextElement(down);
		} else if (value.equals(ICompareUIConstants.PREF_VALUE_PROMPT)) {
			return this.isNavigationPossible() || this.hasNextElement(true);
		}
		Assert.isTrue(false);
		return false;
	}

	private void updateResolveStatus() {

		RGB rgb = null;

		if (this.showResolveUI()) {
			// we only show red or green if there is at least one incoming or conflicting change
			int unresolvedIncoming = 0;
			int unresolvedConflicting = 0;

			if (this.fMerger.hasChanges()) {
				for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
					Diff d = (Diff) iterator.next();
					if (!d.isResolved()) {
						if (d.getKind() == RangeDifference.CONFLICT) {
							unresolvedConflicting++;
							break; // we can stop here because a conflict has the maximum priority
						}
						unresolvedIncoming++;
					}
				}
			}

			if (unresolvedConflicting > 0) {
				rgb = this.conflictPalette.selected;
			} else if (unresolvedIncoming > 0) {
				rgb = this.incomingPalette.selected;
			} else {
				rgb = this.RESOLVED;
			}
		}

		if (this.fHeaderPainter.setColor(rgb)) {
			this.fSummaryHeader.redraw();
		}
	}

	private void updateStatus(Diff diff) {

		String diffDescription;

		if (diff == null) {
			diffDescription = CompareMessages.TextMergeViewer_diffDescription_noDiff_format;
		} else {

			if (diff.isToken()) {
				diff = diff.getParent();
			}

			String format = CompareMessages.TextMergeViewer_diffDescription_diff_format;
			diffDescription = MessageFormat.format(format,
					this.getDiffType(diff), // 0: diff type
					this.getDiffNumber(diff), // 1: diff number
					this.getDiffRange(this.fLeft, diff.getPosition(LEFT_CONTRIBUTOR)), // 2: left start line
					this.getDiffRange(this.fRight, diff.getPosition(RIGHT_CONTRIBUTOR)) // 3: left end line
			);
		}

		String format = CompareMessages.TextMergeViewer_statusLine_format;
		String s = MessageFormat.format(format,
				this.getCursorPosition(this.fLeft), // 0: left column
				this.getCursorPosition(this.fRight), // 1: right column
				diffDescription // 2: diff description
		);

		this.getCompareConfiguration().getContainer().setStatusMessage(s);
	}

	private String getDiffType(final Diff diff) {
		String s = ""; //$NON-NLS-1$
		switch (diff.getKind()) {
		case RangeDifference.LEFT:
			s = this.getCompareConfiguration().isMirrored() ? CompareMessages.TextMergeViewer_direction_incoming
					: CompareMessages.TextMergeViewer_direction_outgoing;
			break;
		case RangeDifference.RIGHT:
			s = this.getCompareConfiguration().isMirrored() ? CompareMessages.TextMergeViewer_direction_outgoing
					: CompareMessages.TextMergeViewer_direction_incoming;
			break;
		case RangeDifference.CONFLICT:
			s = CompareMessages.TextMergeViewer_direction_conflicting;
			break;
		default:
			break;
		}
		String format = CompareMessages.TextMergeViewer_diffType_format;
		return MessageFormat.format(format, s, diff.changeType());
	}

	private String getDiffNumber(final Diff diff) {
		// find the diff's number
		int diffNumber = 0;
		if (this.fMerger.hasChanges()) {
			for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
				Diff d = (Diff) iterator.next();
				diffNumber++;
				if (d == diff) {
					break;
				}
			}
		}
		return Integer.toString(diffNumber);
	}

	private String getDiffRange(final MergeSourceViewer v, final Position pos) {
		Point p = v.getLineRange(pos, new Point(0, 0));
		int startLine = p.x + 1;
		int endLine = p.x + p.y;

		String format;
		if (endLine < startLine) {
			format = CompareMessages.TextMergeViewer_beforeLine_format;
		} else {
			format = CompareMessages.TextMergeViewer_range_format;
		}
		return MessageFormat.format(format, Integer.toString(startLine), Integer.toString(endLine));
	}

	/*
	 * Returns a description of the cursor position.
	 *
	 * @return a description of the cursor position
	 */
	private String getCursorPosition(final MergeSourceViewer v) {
		if (v != null) {
			StyledText styledText = v.getSourceViewer().getTextWidget();

			IDocument document = v.getSourceViewer().getDocument();
			if (document != null) {
				int offset = v.getSourceViewer().getVisibleRegion().getOffset();
				int caret = offset + styledText.getCaretOffset();

				try {
					int line = document.getLineOfOffset(caret);

					int lineOffset = document.getLineOffset(line);
					int occurrences = 0;
					for (int i = lineOffset; i < caret; i++) {
						if ('\t' == document.getChar(i)) {
							++occurrences;
						}
					}

					int tabWidth = styledText.getTabs();
					int column = caret - lineOffset + (tabWidth - 1) * occurrences;

					String format = CompareMessages.TextMergeViewer_cursorPosition_format;
					return MessageFormat.format(format,
							Integer.toString(line + 1), Integer.toString(column + 1));
				} catch (BadLocationException x) {
					// silently ignored
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	protected void updateHeader() {
		super.updateHeader();

		this.updateControls();
	}

	/*
	 * Creates the two items for copying a difference range from one side to the other
	 * and adds them to the given toolbar manager.
	 */
	@Override
	protected void createToolItems(final ToolBarManager tbm) {
		this.fHandlerService = CompareHandlerService.createFor(this.getCompareConfiguration().getContainer(),
				this.fLeft.getSourceViewer().getControl().getShell());

		final String ignoreAncestorActionKey = "action.IgnoreAncestor."; //$NON-NLS-1$
		Action ignoreAncestorAction = new Action() {
			@Override
			public void run() {
				// First make sure the ancestor is hidden
				if (!TextMergeViewer.this.isIgnoreAncestor()) {
					TextMergeViewer.this.getCompareConfiguration()
							.setProperty(ICompareUIConstants.PROP_ANCESTOR_VISIBLE, Boolean.FALSE);
				}
				// Then set the property to ignore the ancestor
				TextMergeViewer.this.getCompareConfiguration()
						.setProperty(ICompareUIConstants.PROP_IGNORE_ANCESTOR,
								Boolean.valueOf(!TextMergeViewer.this.isIgnoreAncestor()));
				Utilities.initToggleAction(this, TextMergeViewer.this.getResourceBundle(), ignoreAncestorActionKey,
						TextMergeViewer.this.isIgnoreAncestor());
			}
		};
		ignoreAncestorAction.setChecked(this.isIgnoreAncestor());
		Utilities.initAction(ignoreAncestorAction, this.getResourceBundle(), ignoreAncestorActionKey);
		Utilities.initToggleAction(ignoreAncestorAction, this.getResourceBundle(), ignoreAncestorActionKey,
				this.isIgnoreAncestor());

		this.fIgnoreAncestorItem = new ActionContributionItem(ignoreAncestorAction);
		this.fIgnoreAncestorItem.setVisible(false);
		tbm.appendToGroup("modes", this.fIgnoreAncestorItem); //$NON-NLS-1$

		tbm.add(new Separator());

		Action a = new Action() {
			@Override
			public void run() {
				if (TextMergeViewer.this.navigate(true, false, false)) {
					TextMergeViewer.this.endOfDocumentReached(true);
				}
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.NextDiff."); //$NON-NLS-1$
		this.fNextDiff = new ActionContributionItem(a);
		tbm.appendToGroup("navigation", this.fNextDiff); //$NON-NLS-1$
		// Don't register this action since it is probably registered by the container

		a = new Action() {
			@Override
			public void run() {
				if (TextMergeViewer.this.navigate(false, false, false)) {
					TextMergeViewer.this.endOfDocumentReached(false);
				}
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.PrevDiff."); //$NON-NLS-1$
		this.fPreviousDiff = new ActionContributionItem(a);
		tbm.appendToGroup("navigation", this.fPreviousDiff); //$NON-NLS-1$
		// Don't register this action since it is probably registered by the container

		a = new Action() {
			@Override
			public void run() {
				if (TextMergeViewer.this.navigate(true, false, true)) {
					TextMergeViewer.this.endOfDocumentReached(true);
				}
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.NextChange."); //$NON-NLS-1$
		this.fNextChange = new ActionContributionItem(a);
		tbm.appendToGroup("navigation", this.fNextChange); //$NON-NLS-1$
		this.fHandlerService.registerAction(a, "org.eclipse.compare.selectNextChange"); //$NON-NLS-1$

		a = new Action() {
			@Override
			public void run() {
				if (TextMergeViewer.this.navigate(false, false, true)) {
					TextMergeViewer.this.endOfDocumentReached(false);
				}
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.PrevChange."); //$NON-NLS-1$
		this.fPreviousChange = new ActionContributionItem(a);
		tbm.appendToGroup("navigation", this.fPreviousChange); //$NON-NLS-1$
		this.fHandlerService.registerAction(a, "org.eclipse.compare.selectPreviousChange"); //$NON-NLS-1$

		a = new Action() {
			@Override
			public void run() {
				TextMergeViewer.this.copyDiffLeftToRight();
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.CopyDiffLeftToRight."); //$NON-NLS-1$
		this.fCopyDiffLeftToRightItem = new ActionContributionItem(a);
		this.fCopyDiffLeftToRightItem.setVisible(this.isRightEditable());
		tbm.appendToGroup("merge", this.fCopyDiffLeftToRightItem); //$NON-NLS-1$
		this.fHandlerService.registerAction(a, "org.eclipse.compare.copyLeftToRight"); //$NON-NLS-1$

		a = new Action() {
			@Override
			public void run() {
				TextMergeViewer.this.copyDiffRightToLeft();
			}
		};
		Utilities.initAction(a, this.getResourceBundle(), "action.CopyDiffRightToLeft."); //$NON-NLS-1$
		this.fCopyDiffRightToLeftItem = new ActionContributionItem(a);
		this.fCopyDiffRightToLeftItem.setVisible(this.isLeftEditable());
		tbm.appendToGroup("merge", this.fCopyDiffRightToLeftItem); //$NON-NLS-1$
		this.fHandlerService.registerAction(a, "org.eclipse.compare.copyRightToLeft"); //$NON-NLS-1$

		this.fIgnoreWhitespace = ChangePropertyAction.createIgnoreWhiteSpaceAction(this.getResourceBundle(),
				this.getCompareConfiguration());
		this.fIgnoreWhitespace.setActionDefinitionId(ICompareUIConstants.COMMAND_IGNORE_WHITESPACE);
		this.fLeft.addTextAction(this.fIgnoreWhitespace);
		this.fRight.addTextAction(this.fIgnoreWhitespace);
		this.fAncestor.addTextAction(this.fIgnoreWhitespace);
		this.fHandlerService.registerAction(this.fIgnoreWhitespace, this.fIgnoreWhitespace.getActionDefinitionId());

		// By default, check ignore whitespace
		//		this.fIgnoreWhitespace.setChecked(true);

		boolean needsLeftPainter = !this.isEditorBacked(this.fLeft.getSourceViewer());
		boolean needsRightPainter = !this.isEditorBacked(this.fRight.getSourceViewer());
		boolean needsAncestorPainter = !this.isEditorBacked(this.fAncestor.getSourceViewer());
		this.showWhitespaceAction = new ShowWhitespaceAction(
				new MergeSourceViewer[] { this.fLeft, this.fRight, this.fAncestor },
				new boolean[] { needsLeftPainter, needsRightPainter, needsAncestorPainter });
		// showWhitespaceAction is registered as global action in connectGlobalActions
		this.fLeft.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, this.showWhitespaceAction);
		this.fRight.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, this.showWhitespaceAction);
		this.fAncestor.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, this.showWhitespaceAction);
		this.fHandlerService.registerAction(this.showWhitespaceAction,
				ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS);

		this.toggleLineNumbersAction = new LineNumberRulerToggleAction(CompareMessages.TextMergeViewer_16,
				new MergeSourceViewer[] { this.fLeft, this.fRight, this.fAncestor },
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER);
		this.fHandlerService.registerAction(this.toggleLineNumbersAction,
				ITextEditorActionDefinitionIds.LINENUMBER_TOGGLE);
	}

	private void configureCompareFilterActions(final Object input, final Object ancestor,
			final Object left, final Object right) {
		if (this.getCompareConfiguration() == null) {
			return;
		}

		CompareFilterDescriptor[] compareFilterDescriptors = CompareUIPlugin.getDefault().findCompareFilters(input);
		Object current = this.getCompareConfiguration()
				.getProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS);
		boolean currentFiltersMatch = false;
		if (current != null && current instanceof List
				&& ((List<?>) current).size() == compareFilterDescriptors.length) {
			currentFiltersMatch = true;
			@SuppressWarnings("unchecked")
			List<ChangeCompareFilterPropertyAction> currentFilterActions = (List<ChangeCompareFilterPropertyAction>) current;
			for (CompareFilterDescriptor compareFilterDescriptor : compareFilterDescriptors) {
				boolean match = false;
				for (ChangeCompareFilterPropertyAction currentFilterAction : currentFilterActions) {
					if (compareFilterDescriptor.getFilterId().equals(currentFilterAction.getFilterId())) {
						match = true;
						break;
					}
				}
				if (!match) {
					currentFiltersMatch = false;
					break;
				}
			}
		}
		if (!currentFiltersMatch) {
			this.getCompareConfiguration()
					.setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING,
							Boolean.TRUE);
			this.disposeCompareFilterActions(true);
			this.fCompareFilterActions.clear();
			for (CompareFilterDescriptor compareFilterDescriptor : compareFilterDescriptors) {
				ChangeCompareFilterPropertyAction compareFilterAction = new ChangeCompareFilterPropertyAction(
						compareFilterDescriptor, this.getCompareConfiguration());
				compareFilterAction.setInput(input, ancestor, left, right);
				this.fCompareFilterActions.add(compareFilterAction);
				this.fLeft.addTextAction(compareFilterAction);
				this.fRight.addTextAction(compareFilterAction);
				this.fAncestor.addTextAction(compareFilterAction);

				if (this.getCompareConfiguration().getContainer().getActionBars() != null) {
					this.getCompareConfiguration()
							.getContainer()
							.getActionBars()
							.getToolBarManager()
							.appendToGroup(CompareEditorContributor.FILTER_SEPARATOR, compareFilterAction);
					if (compareFilterAction.getActionDefinitionId() != null) {
						this.getCompareConfiguration()
								.getContainer()
								.getActionBars()
								.setGlobalActionHandler(compareFilterAction.getActionDefinitionId(),
										compareFilterAction);
					}
				}
			}
			if (!this.fCompareFilterActions.isEmpty()
					&& this.getCompareConfiguration().getContainer().getActionBars() != null) {
				this.getCompareConfiguration().getContainer().getActionBars().getToolBarManager().markDirty();
				this.getCompareConfiguration().getContainer().getActionBars().getToolBarManager().update(true);
				this.getCompareConfiguration().getContainer().getActionBars().updateActionBars();
			}
			this.getCompareConfiguration()
					.setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS,
							this.fCompareFilterActions);
			this.getCompareConfiguration()
					.setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING, null);
		} else {
			for (ChangeCompareFilterPropertyAction action : this.fCompareFilterActions) {
				action.setInput(input, ancestor, left, right);
			}
		}
	}

	private void disposeCompareFilterActions(final boolean updateActionBars) {
		Iterator<ChangeCompareFilterPropertyAction> compareFilterActionsIterator = this.fCompareFilterActions
				.iterator();
		while (compareFilterActionsIterator.hasNext()) {
			ChangeCompareFilterPropertyAction compareFilterAction = compareFilterActionsIterator
					.next();
			this.fLeft.removeTextAction(compareFilterAction);
			this.fRight.removeTextAction(compareFilterAction);
			this.fAncestor.removeTextAction(compareFilterAction);
			if (updateActionBars
					&& this.getCompareConfiguration().getContainer().getActionBars() != null) {
				this.getCompareConfiguration()
						.getContainer()
						.getActionBars()
						.getToolBarManager()
						.remove(compareFilterAction.getId());
				if (compareFilterAction.getActionDefinitionId() != null) {
					this.getCompareConfiguration()
							.getContainer()
							.getActionBars()
							.setGlobalActionHandler(
									compareFilterAction.getActionDefinitionId(),
									null);
				}
			}
			compareFilterAction.dispose();
		}
		if (updateActionBars
				&& !this.fCompareFilterActions.isEmpty()
				&& this.getCompareConfiguration().getContainer().getActionBars() != null) {
			this.getCompareConfiguration()
					.getContainer()
					.getActionBars()
					.getToolBarManager()
					.markDirty();
			this.getCompareConfiguration()
					.getContainer()
					.getActionBars()
					.getToolBarManager()
					.update(true);
		}
		this.fCompareFilterActions.clear();
		this.getCompareConfiguration()
				.setProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTERS, null);
		this.getCompareConfiguration()
				.setProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS, null);
	}

	@Override
	protected void handlePropertyChangeEvent(final PropertyChangeEvent event) {
		String key = event.getProperty();

		if (key.equals(CompareConfiguration.IGNORE_WHITESPACE)
				|| key.equals(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS)
				|| (key.equals(ChangeCompareFilterPropertyAction.COMPARE_FILTERS) && this.getCompareConfiguration()
						.getProperty(
								ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING) == null)) {

			this.fShowPseudoConflicts = this.fPreferenceStore.getBoolean(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS);

			this.update(true);
			// selectFirstDiff(true);
			if (this.fFocusPart != null) {
				this.handleSelectionChanged(this.fFocusPart);
			}

			//		} else if (key.equals(ComparePreferencePage.USE_SPLINES)) {
			//			fUseSplines= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SPLINES);
			//			invalidateLines();

		} else if (key.equals(ComparePreferencePage.USE_SINGLE_LINE)) {
			this.fUseSingleLine = this.fPreferenceStore.getBoolean(ComparePreferencePage.USE_SINGLE_LINE);
			//			fUseResolveUI= fUseSingleLine;
			this.fBasicCenterCurve = null;
			this.updateControls();
			this.invalidateLines();

		} else if (key.equals(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES)) {
			this.fHighlightTokenChanges = this.fPreferenceStore
					.getBoolean(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES);
			this.updateControls();
			this.updatePresentation();

			//		} else if (key.equals(ComparePreferencePage.USE_RESOLVE_UI)) {
			//			fUseResolveUI= fPreferenceStore.getBoolean(ComparePreferencePage.USE_RESOLVE_UI);
			//			updateResolveStatus();
			//			invalidateLines();

		} else if (key.equals(this.fSymbolicFontName)) {
			this.updateFont();
			this.invalidateLines();

		} else if (key.equals(INCOMING_COLOR) || key.equals(OUTGOING_COLOR) || key.equals(CONFLICTING_COLOR)
				|| key.equals(RESOLVED_COLOR) || key.equals(ADDITION_COLOR) || key.equals(DELETION_COLOR)
				|| key.equals(EDITION_COLOR)) {
			this.updateColors(null);
			this.invalidateLines();
			this.invalidateTextPresentation();

		} else if (key.equals(ComparePreferencePage.SYNCHRONIZE_SCROLLING)) {
			boolean b = this.fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING);
			this.setSyncScrolling(b);

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND)) {
			if (!this.fIsUsingSystemBackground) {
				this.setBackgroundColor(
						createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND));
			}

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
			this.fIsUsingSystemBackground = this.fPreferenceStore
					.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
			if (this.fIsUsingSystemBackground) {
				this.setBackgroundColor(null);
			} else {
				this.setBackgroundColor(
						createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND));
			}
		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND)) {
			if (!this.fIsUsingSystemForeground) {
				this.setForegroundColor(
						createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND));
			}

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)) {
			this.fIsUsingSystemForeground = this.fPreferenceStore
					.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT);
			if (this.fIsUsingSystemForeground) {
				this.setForegroundColor(null);
			} else {
				this.setForegroundColor(
						createColor(this.fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND));
			}
		} else if (key.equals(ICompareUIConstants.PREF_NAVIGATION_END_ACTION)) {
			this.updateControls();
		} else if (key.equals(CompareContentViewerSwitchingPane.DISABLE_CAPPING_TEMPORARILY)) {
			if (Boolean.TRUE.equals(event.getNewValue())) {
				this.getCompareConfiguration()
						.setProperty(CompareContentViewerSwitchingPane.DISABLE_CAPPING_TEMPORARILY, null);
				this.handleCompareInputChange();
			}
		} else {
			super.handlePropertyChangeEvent(event);

			if (key.equals(ICompareUIConstants.PROP_IGNORE_ANCESTOR)) {
				this.update(true);
				this.selectFirstDiff(true);
			}
		}
	}

	private void selectFirstDiff(final boolean first) {

		if (this.fLeft == null || this.fRight == null) {
			return;
		}
		if (this.fLeft.getSourceViewer().getDocument() == null || this.fRight.getSourceViewer().getDocument() == null) {
			return;
		}

		Diff firstDiff = null;
		if (first) {
			firstDiff = this.findNext(this.fRight, -1, -1, false);
		} else {
			firstDiff = this.findPrev(this.fRight, 9999999, 9999999, false);
		}
		this.setCurrentDiff(firstDiff, true);
	}

	private void setSyncScrolling(final boolean newMode) {
		if (this.fSynchronizedScrolling != newMode) {
			this.fSynchronizedScrolling = newMode;

			this.scrollVertical(0, 0, 0, null);

			// throw away central control (Sash or Canvas)
			Control center = this.getCenterControl();
			if (center != null && !center.isDisposed()) {
				center.dispose();
			}

			this.fLeft.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!this.fSynchronizedScrolling);
			this.fRight.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!this.fSynchronizedScrolling);

			this.fComposite.layout(true);
		}
	}

	@Override
	protected void updateToolItems() {
		if (this.fCopyDiffLeftToRightItem != null) {
			this.fCopyDiffLeftToRightItem.setVisible(this.isRightEditable());
		}
		if (this.fCopyDiffRightToLeftItem != null) {
			this.fCopyDiffRightToLeftItem.setVisible(this.isLeftEditable());
		}

		//only update toolbar items if diffs need to be calculated (which
		//dictates whether a toolbar gets added at all)
		if (!this.isPatchHunk()) {
			if (this.fIgnoreAncestorItem != null) {
				this.fIgnoreAncestorItem.setVisible(this.isThreeWay());
			}

			if (this.fCopyDiffLeftToRightItem != null) {
				IAction a = this.fCopyDiffLeftToRightItem.getAction();
				if (a != null) {
					a.setEnabled(a.isEnabled() && !this.fHasErrors);
				}
			}
			if (this.fCopyDiffRightToLeftItem != null) {
				IAction a = this.fCopyDiffRightToLeftItem.getAction();
				if (a != null) {
					a.setEnabled(a.isEnabled() && !this.fHasErrors);
				}
			}
		}
		super.updateToolItems();
	}

	//---- painting lines

	private void updateLines(final IDocument d) {
		boolean left = false;
		boolean right = false;

		// FIXME: this optimization is incorrect because
		// it doesn't take replace operations into account where
		// the old and new line count does not differ
		if (d == this.fLeft.getSourceViewer().getDocument()) {
			int l = this.fLeft.getLineCount();
			left = this.fLeftLineCount != l;
			this.fLeftLineCount = l;
		} else if (d == this.fRight.getSourceViewer().getDocument()) {
			int l = this.fRight.getLineCount();
			right = this.fRightLineCount != l;
			this.fRightLineCount = l;
		}

		if (left || right) {
			if (left) {
				if (this.fLeftCanvas != null) {
					this.fLeftCanvas.redraw();
				}
			} else {
				if (this.fRightCanvas != null) {
					this.fRightCanvas.redraw();
				}
			}
			Control center = this.getCenterControl();
			if (center != null) {
				center.redraw();
			}

			this.updateVScrollBar();
			this.refreshBirdsEyeView();
		}
	}

	private void invalidateLines() {
		if (this.isThreeWay() && this.isAncestorVisible()) {
			if (Utilities.okToUse(this.fAncestorCanvas)) {
				this.fAncestorCanvas.redraw();
			}
			if (this.fAncestor != null && this.fAncestor.isControlOkToUse()) {
				this.fAncestor.getSourceViewer().getTextWidget().redraw();
			}
		}

		if (Utilities.okToUse(this.fLeftCanvas)) {
			this.fLeftCanvas.redraw();
		}

		if (this.fLeft != null && this.fLeft.isControlOkToUse()) {
			this.fLeft.getSourceViewer().getTextWidget().redraw();
		}

		if (Utilities.okToUse(this.getCenterControl())) {
			this.getCenterControl().redraw();
		}

		if (this.fRight != null && this.fRight.isControlOkToUse()) {
			this.fRight.getSourceViewer().getTextWidget().redraw();
		}

		if (Utilities.okToUse(this.fRightCanvas)) {
			this.fRightCanvas.redraw();
		}
	}

	private boolean showResolveUI() {
		if (!this.isThreeWay() || this.isIgnoreAncestor()) {
			return false;
		}
		return this.isAnySideEditable();
	}

	private boolean isAnySideEditable() {
		// we only enable the new resolve UI if exactly one side is editable
		return this.isLeftEditable() || this.isRightEditable();
	}

	private void paintCenter(final Canvas canvas, final GC g) {

		Display display = canvas.getDisplay();

		this.checkForColorUpdate(display);

		if (!this.fSynchronizedScrolling) {
			return;
		}

		int lineHeightLeft = this.fLeft.getSourceViewer().getTextWidget().getLineHeight();
		int lineHeightRight = this.fRight.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight = this.fRight.getViewportHeight();

		Point size = canvas.getSize();
		int x = 0;
		int w = size.x;

		g.setBackground(canvas.getBackground());
		g.fillRectangle(x + 1, 0, w - 2, size.y);

		// draw thin line between center ruler and both texts
		g.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		g.fillRectangle(0, 0, 1, size.y);
		g.fillRectangle(w - 1, 0, 1, size.y);

		if (!this.fHighlightRanges) {
			return;
		}

		if (this.fMerger.hasChanges()) {
			int lshift = this.fLeft.getVerticalScrollOffset();
			int rshift = this.fRight.getVerticalScrollOffset();

			Point region = new Point(0, 0);

			boolean allOutgoing = this.allOutgoing();
			for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted()) {
					continue;
				}

				if (this.fShowCurrentOnly2 && !this.isCurrentDiff(diff)) {
					continue;
				}

				this.fLeft.getLineRange(diff.getPosition(LEFT_CONTRIBUTOR), region);
				int ly = (region.x * lineHeightLeft) + lshift;
				int lh = region.y * lineHeightLeft;

				this.fRight.getLineRange(diff.getPosition(RIGHT_CONTRIBUTOR), region);
				int ry = (region.x * lineHeightRight) + rshift;
				int rh = region.y * lineHeightRight;

				if (Math.max(ly + lh, ry + rh) < 0) {
					continue;
				}
				if (Math.min(ly, ry) >= visibleHeight) {
					break;
				}

				this.fPts[0] = x;
				this.fPts[1] = ly;
				this.fPts[2] = w;
				this.fPts[3] = ry;
				this.fPts[6] = x;
				this.fPts[7] = ly + lh;
				this.fPts[4] = w;
				this.fPts[5] = ry + rh;

				Color fillColor = this.getColor(display, this.getFillColor(diff, allOutgoing));
				Color strokeColor = this.getColor(display, this.getStrokeColor(diff, allOutgoing));

				if (this.fUseSingleLine) {
					int w2 = 3;

					g.setBackground(fillColor);
					g.fillRectangle(0, ly, w2, lh); // left
					g.fillRectangle(w - w2, ry, w2, rh); // right

					g.setLineWidth(0 /* LW */);
					g.setForeground(strokeColor);
					g.drawRectangle(0 - 1, ly, w2, lh); // left
					g.drawRectangle(w - w2, ry, w2, rh); // right

					if (this.fUseSplines) {
						int[] points = this.getCenterCurvePoints(w2, ly + lh / 2, w - w2, ry + rh / 2);
						for (int i = 1; i < points.length; i++) {
							g.drawLine(w2 + i - 1, points[i - 1], w2 + i, points[i]);
						}
					} else {
						g.drawLine(w2, ly + lh / 2, w - w2, ry + rh / 2);
					}
				} else {
					// two lines
					if (this.fUseSplines) {
						g.setBackground(fillColor);

						g.setLineWidth(0 /* LW */);
						g.setForeground(strokeColor);

						int[] topPoints = this.getCenterCurvePoints(this.fPts[0], this.fPts[1], this.fPts[2],
								this.fPts[3]);
						int[] bottomPoints = this.getCenterCurvePoints(this.fPts[6], this.fPts[7], this.fPts[4],
								this.fPts[5]);
						g.setForeground(fillColor);
						g.drawLine(0, bottomPoints[0], 0, topPoints[0]);
						for (int i = 1; i < bottomPoints.length; i++) {
							g.setForeground(fillColor);
							g.drawLine(i, bottomPoints[i], i, topPoints[i]);
							g.setForeground(strokeColor);
							g.drawLine(i - 1, topPoints[i - 1], i, topPoints[i]);
							g.drawLine(i - 1, bottomPoints[i - 1], i, bottomPoints[i]);
						}
					} else {
						g.setBackground(fillColor);
						g.fillPolygon(this.fPts);

						g.setLineWidth(0 /* LW */);
						g.setForeground(strokeColor);
						g.drawLine(this.fPts[0], this.fPts[1], this.fPts[2], this.fPts[3]);
						g.drawLine(this.fPts[6], this.fPts[7], this.fPts[4], this.fPts[5]);
					}
				}

				if (this.fUseSingleLine && this.isAnySideEditable()) {
					// draw resolve state
					int cx = (w - RESOLVE_SIZE) / 2;
					int cy = ((ly + lh / 2) + (ry + rh / 2) - RESOLVE_SIZE) / 2;

					g.setBackground(fillColor);
					g.fillRectangle(cx, cy, RESOLVE_SIZE, RESOLVE_SIZE);

					g.setForeground(strokeColor);
					g.drawRectangle(cx, cy, RESOLVE_SIZE, RESOLVE_SIZE);
				}
			}
		}
	}

	private int[] getCenterCurvePoints(final int startx, final int starty, final int endx, final int endy) {
		if (this.fBasicCenterCurve == null) {
			this.buildBaseCenterCurve(endx - startx);
		}
		double height = endy - starty;
		height = height / 2;
		int width = endx - startx;
		int[] points = new int[width];
		for (int i = 0; i < width; i++) {
			points[i] = (int) (-height * this.fBasicCenterCurve[i] + height + starty);
		}
		return points;
	}

	private void buildBaseCenterCurve(final int w) {
		double width = w;
		this.fBasicCenterCurve = new double[this.getCenterWidth()];
		for (int i = 0; i < this.getCenterWidth(); i++) {
			double r = i / width;
			this.fBasicCenterCurve[i] = Math.cos(Math.PI * r);
		}
	}

	private void paintSides(final GC g, final MergeSourceViewer tp, final Canvas canvas, final boolean right) {

		Display display = canvas.getDisplay();

		int lineHeight = tp.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight = tp.getViewportHeight();

		Point size = canvas.getSize();
		int x = 0;
		int w = this.fMarginWidth;
		int w2 = w / 2;

		g.setBackground(canvas.getBackground());
		g.fillRectangle(x, 0, w, size.y);

		// draw thin line between ruler and text
		g.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		if (right) {
			g.fillRectangle(0, 0, 1, size.y);
		} else {
			g.fillRectangle(size.x - 1, 0, 1, size.y);
		}

		if (!this.fHighlightRanges) {
			return;
		}

		if (this.fMerger.hasChanges()) {
			boolean allOutgoing = this.allOutgoing();
			int shift = tp.getVerticalScrollOffset() + (2 - LW);

			Point region = new Point(0, 0);
			char leg = this.getLeg(tp);
			for (Iterator<Diff> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = iterator.next();
				if (diff.isDeleted()) {
					continue;
				}

				if (this.fShowCurrentOnly2 && !this.isCurrentDiff(diff)) {
					continue;
				}

				tp.getLineRange(diff.getPosition(leg), region);
				int y = (region.x * lineHeight) + shift;
				int h = region.y * lineHeight;

				if (y + h < 0) {
					continue;
				}
				if (y >= visibleHeight) {
					break;
				}

				g.setBackground(this.getColor(display, this.getFillColor(diff, allOutgoing)));
				if (right) {
					g.fillRectangle(x, y, w2, h);
				} else {
					g.fillRectangle(x + w2, y, w2, h);
				}

				g.setLineWidth(0 /* LW */);
				g.setForeground(this.getColor(display, this.getStrokeColor(diff, allOutgoing)));
				if (right) {
					g.drawRectangle(x - 1, y - 1, w2, h);
				} else {
					g.drawRectangle(x + w2, y - 1, w2, h);
				}
			}
		}
	}

	private boolean allOutgoing() {
		boolean allOutgoing = !this.isThreeWay();
		for (Iterator<Diff> iterator = this.fMerger.changesIterator(); allOutgoing && iterator.hasNext();) {
			Diff diff = iterator.next();
			allOutgoing &= !diff.isDeleted()
					&& (diff.getKind() == RangeDifference.NOCHANGE || diff.getKind() == RangeDifference.CHANGE);
		}
		return allOutgoing;
	}

	private void paint(final PaintEvent event, final MergeSourceViewer tp) {

		if (!this.fHighlightRanges) {
			return;
		}
		if (!this.fMerger.hasChanges()) {
			return;
		}

		Control canvas = (Control) event.widget;
		GC g = event.gc;

		Display display = canvas.getDisplay();

		int lineHeight = tp.getSourceViewer().getTextWidget().getLineHeight();
		int w = canvas.getSize().x;
		int shift = tp.getVerticalScrollOffset() + (2 - LW);
		int maxh = event.y + event.height; // visibleHeight

		shift += this.fTopInset;

		Point range = new Point(0, 0);

		char leg = this.getLeg(tp);
		boolean allOutgoing = this.allOutgoing();
		for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
			Diff diff = (Diff) iterator.next();
			if (diff.isDeleted()) {
				continue;
			}

			if (this.fShowCurrentOnly && !this.isCurrentDiff(diff)) {
				continue;
			}

			tp.getLineRange(diff.getPosition(leg), range);
			int y = (range.x * lineHeight) + shift;
			int h = range.y * lineHeight;

			if (y + h < event.y) {
				continue;
			}
			if (y > maxh) {
				break;
			}

			g.setBackground(this.getColor(display, this.getStrokeColor(diff, allOutgoing)));
			g.fillRectangle(0, y - 1, w, LW);
			g.fillRectangle(0, y + h - 1, w, LW);
		}
	}

	private RGB getFillColor(final Diff diff, final boolean showAdditionRemoval) {
		boolean selected = this.fCurrentDiff != null && this.fCurrentDiff.getParent() == diff;
		if (selected) {
			return this.getBackground(null);
		}
		ColorPalette palette = this.getColorPalette(diff, showAdditionRemoval);
		if (palette == null) {
			return null;
		}
		return palette.fill;
	}

	private ColorPalette getColorPalette(final Diff diff, final boolean showAdditionRemoval) {
		if (this.isThreeWay() && !this.isIgnoreAncestor()) {
			switch (diff.getKind()) {
			case RangeDifference.RIGHT:
				if (!this.getCompareConfiguration().isMirrored()) {
					return this.incomingPalette;
				}
				return this.outgoingPalette;
			case RangeDifference.ANCESTOR:
			case RangeDifference.CONFLICT:
				return this.conflictPalette;
			case RangeDifference.LEFT:
				if (!this.getCompareConfiguration().isMirrored()) {
					return this.outgoingPalette;
				}
				return this.incomingPalette;
			default:
				return null;
			}
		}
		if (showAdditionRemoval) {
			if (diff.getPosition(LEFT_CONTRIBUTOR).getLength() == 0) {
				if (this.getCompareConfiguration().isMirrored()) {
					return this.additionPalette;
				}
				return this.deletionPalette;
			}
			if (diff.getPosition(RIGHT_CONTRIBUTOR).getLength() == 0) {
				if (this.getCompareConfiguration().isMirrored()) {
					return this.deletionPalette;
				}
				return this.additionPalette;
			}
			return this.editionPalette;
		}
		return this.outgoingPalette;
	}

	private RGB getStrokeColor(final Diff diff, final boolean showAdditionRemoval) {
		ColorPalette palette = this.getColorPalette(diff, showAdditionRemoval);
		if (palette == null) {
			return null;
		}
		boolean selected = this.fCurrentDiff != null && this.fCurrentDiff.getParent() == diff;
		return selected ? palette.selected : palette.normal;
	}

	private Color getColor(final Display display, final RGB rgb) {
		if (rgb == null) {
			return null;
		}
		if (this.fColors == null) {
			this.fColors = new HashMap<>(20);
		}
		Color c = this.fColors.get(rgb);
		if (c == null) {
			c = new Color(display, rgb);
			this.fColors.put(rgb, c);
		}
		return c;
	}

	//---- Navigating and resolving Diffs

	private Diff getNextVisibleDiff(final boolean down, final boolean deep) {
		Diff diff = null;
		MergeSourceViewer part = this.getNavigationPart();
		if (part == null) {
			return null;
		}
		Point s = part.getSourceViewer().getSelectedRange();
		char leg = this.getLeg(part);
		for (;;) {
			diff = null;
			diff = this.internalGetNextDiff(down, deep, part, s);
			if (diff != null && diff.getKind() == RangeDifference.ANCESTOR
					&& !this.isAncestorVisible()) {
				Position position = diff.getPosition(leg);
				s = new Point(position.getOffset(), position.getLength());
				diff = null;
				continue;
			}
			break;
		}
		return diff;
	}

	private Diff internalGetNextDiff(final boolean down, final boolean deep, final MergeSourceViewer part,
			final Point s) {
		if (this.fMerger.hasChanges()) {
			if (down) {
				return this.findNext(part, s.x, s.x + s.y, deep);
			}
			return this.findPrev(part, s.x, s.x + s.y, deep);
		}
		return null;
	}

	private MergeSourceViewer getNavigationPart() {
		MergeSourceViewer part = this.fFocusPart;
		if (part == null) {
			part = this.fRight;
		}
		return part;
	}

	private Diff getWrappedDiff(final Diff diff, final boolean down) {
		return this.fMerger.getWrappedDiff(diff, down);
	}

	/*
	 * Returns true if end (or beginning) of document reached.
	 */
	private boolean navigate(final boolean down, final boolean wrap, final boolean deep) {
		Diff diff = null;
		boolean wrapped = false;
		for (;;) {
			diff = this.getNextVisibleDiff(down, deep);
			if (diff == null && wrap) {
				if (wrapped) {
					// We've already wrapped once so break out
					break;
				}
				wrapped = true;
				diff = this.getWrappedDiff(diff, down);
			}
			if (diff != null) {
				this.setCurrentDiff(diff, true, deep);
			}
			if (diff != null && diff.getKind() == RangeDifference.ANCESTOR
					&& !this.isAncestorVisible()) {
				continue;
			}
			break;
		}
		return diff == null;
	}

	private void endOfDocumentReached(final boolean down) {
		Control c = this.getControl();
		if (Utilities.okToUse(c)) {
			this.handleEndOfDocumentReached(c.getShell(), down);
		}
	}

	private void handleEndOfDocumentReached(final Shell shell, final boolean next) {
		IPreferenceStore store = CompareUIPlugin.getDefault().getPreferenceStore();
		String value = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		if (!value.equals(ICompareUIConstants.PREF_VALUE_PROMPT)) {
			this.performEndOfDocumentAction(shell, store, ICompareUIConstants.PREF_NAVIGATION_END_ACTION, next);
		} else {
			shell.getDisplay().beep();
			String loopMessage;
			String nextMessage;
			String message;
			String title;
			if (next) {
				title = CompareMessages.TextMergeViewer_0;
				message = CompareMessages.TextMergeViewer_1;
				loopMessage = CompareMessages.TextMergeViewer_2;
				nextMessage = CompareMessages.TextMergeViewer_3;
			} else {
				title = CompareMessages.TextMergeViewer_4;
				message = CompareMessages.TextMergeViewer_5;
				loopMessage = CompareMessages.TextMergeViewer_6;
				nextMessage = CompareMessages.TextMergeViewer_7;
			}
			String[] localLoopOption = new String[] { loopMessage, ICompareUIConstants.PREF_VALUE_LOOP };
			String[] nextElementOption = new String[] { nextMessage, ICompareUIConstants.PREF_VALUE_NEXT };
			String[] doNothingOption = new String[] { CompareMessages.TextMergeViewer_17,
					ICompareUIConstants.PREF_VALUE_DO_NOTHING };
			NavigationEndDialog dialog = new NavigationEndDialog(shell,
					title,
					null,
					message,
					new String[][] {
							localLoopOption,
							nextElementOption,
							doNothingOption
					});
			int result = dialog.open();
			if (result == Window.OK) {
				this.performEndOfDocumentAction(shell, store, ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL,
						next);
				if (dialog.getToggleState()) {
					String oldValue = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
					store.putValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION,
							store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL));
					store.firePropertyChangeEvent(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, oldValue,
							store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL));
				}
			}
		}
	}

	private void performEndOfDocumentAction(final Shell shell, final IPreferenceStore store, final String key,
			final boolean next) {
		String value = store.getString(key);
		if (value.equals(ICompareUIConstants.PREF_VALUE_DO_NOTHING)) {
			return;
		}
		if (value.equals(ICompareUIConstants.PREF_VALUE_NEXT)) {
			ICompareNavigator navigator = this.getCompareConfiguration()
					.getContainer()
					.getNavigator();
			if (this.hasNextElement(next)) {
				navigator.selectChange(next);
			}
		} else {
			this.selectFirstDiff(next);
		}
	}

	private boolean hasNextElement(final boolean down) {
		ICompareNavigator navigator = this.getCompareConfiguration().getContainer().getNavigator();
		if (navigator instanceof CompareNavigator) {
			CompareNavigator n = (CompareNavigator) navigator;
			return n.hasChange(down);
		}
		return false;
	}

	/*
	 * Find the Diff that overlaps with the given TextPart's text range.
	 * If the range doesn't overlap with any range <code>null</code>
	 * is returned.
	 */
	private Diff findDiff(final MergeSourceViewer tp, final int rangeStart, final int rangeEnd) {
		char contributor = this.getLeg(tp);
		return this.fMerger.findDiff(contributor, rangeStart, rangeEnd);
	}

	private Diff findNext(final MergeSourceViewer tp, final int start, final int end, final boolean deep) {
		return this.fMerger.findNext(this.getLeg(tp), start, end, deep);
	}

	private Diff findPrev(final MergeSourceViewer tp, final int start, final int end, final boolean deep) {
		return this.fMerger.findPrev(this.getLeg(tp), start, end, deep);
	}

	/*
	 * Set the currently active Diff and update the toolbars controls and lines.
	 * If <code>revealAndSelect</code> is <code>true</code> the Diff is revealed and
	 * selected in both TextParts.
	 */
	private void setCurrentDiff(final Diff d, final boolean revealAndSelect) {
		this.setCurrentDiff(d, revealAndSelect, false);
	}

	/*
	 * Set the currently active Diff and update the toolbars controls and lines.
	 * If <code>revealAndSelect</code> is <code>true</code> the Diff is revealed and
	 * selected in both TextParts.
	 */
	private void setCurrentDiff(final Diff d, final boolean revealAndSelect, final boolean deep) {

		//		if (d == fCurrentDiff)
		//			return;
		boolean diffChanged = this.fCurrentDiff != d;

		if (this.fLeftToRightButton != null && !this.fLeftToRightButton.isDisposed()) {
			this.fLeftToRightButton.setVisible(false);
		}
		if (this.fRightToLeftButton != null && !this.fRightToLeftButton.isDisposed()) {
			this.fRightToLeftButton.setVisible(false);
		}

		if (d != null && revealAndSelect) {

			// before we set fCurrentDiff we change the selection
			// so that the paint code uses the old background colors
			// otherwise selection isn't drawn correctly
			if (d.isToken() || !this.fHighlightTokenChanges || deep || !d.hasChildren()) {
				if (this.isThreeWay() && !this.isIgnoreAncestor()) {
					this.fAncestor.setSelection(d.getPosition(ANCESTOR_CONTRIBUTOR));
				}
				this.fLeft.setSelection(d.getPosition(LEFT_CONTRIBUTOR));
				this.fRight.setSelection(d.getPosition(RIGHT_CONTRIBUTOR));
			} else {
				if (this.isThreeWay() && !this.isIgnoreAncestor()) {
					this.fAncestor.setSelection(new Position(d.getPosition(ANCESTOR_CONTRIBUTOR).offset, 0));
				}
				this.fLeft.setSelection(new Position(d.getPosition(LEFT_CONTRIBUTOR).offset, 0));
				this.fRight.setSelection(new Position(d.getPosition(RIGHT_CONTRIBUTOR).offset, 0));
			}

			// now switch diffs
			this.saveDiff();
			this.fCurrentDiff = d;
			this.revealDiff(d, d.isToken());
		} else {
			this.saveDiff();
			this.fCurrentDiff = d;
		}

		this.updateControls();
		if (diffChanged) {
			this.invalidateLines();
		}
		this.refreshBirdsEyeView();
	}

	/*
	 * Smart determines whether
	 */
	private void revealDiff(final Diff d, final boolean smart) {

		boolean ancestorIsVisible = false;
		boolean leftIsVisible = false;
		boolean rightIsVisible = false;

		if (smart) {
			Point region = new Point(0, 0);
			// find the starting line of the diff in all text widgets
			int ls = this.fLeft.getLineRange(d.getPosition(LEFT_CONTRIBUTOR), region).x;
			int rs = this.fRight.getLineRange(d.getPosition(RIGHT_CONTRIBUTOR), region).x;

			if (this.isThreeWay() && !this.isIgnoreAncestor()) {
				int as = this.fAncestor.getLineRange(d.getPosition(ANCESTOR_CONTRIBUTOR), region).x;
				if (as >= this.fAncestor.getSourceViewer().getTopIndex()
						&& as <= this.fAncestor.getSourceViewer().getBottomIndex()) {
					ancestorIsVisible = true;
				}
			}

			if (ls >= this.fLeft.getSourceViewer().getTopIndex()
					&& ls <= this.fLeft.getSourceViewer().getBottomIndex()) {
				leftIsVisible = true;
			}

			if (rs >= this.fRight.getSourceViewer().getTopIndex()
					&& rs <= this.fRight.getSourceViewer().getBottomIndex()) {
				rightIsVisible = true;
			}
		}

		// vertical scrolling
		if (!leftIsVisible || !rightIsVisible) {
			int avpos = 0, lvpos = 0, rvpos = 0;

			MergeSourceViewer allButThis = null;
			if (leftIsVisible) {
				avpos = lvpos = rvpos = this.realToVirtualPosition(LEFT_CONTRIBUTOR,
						this.fLeft.getSourceViewer().getTopIndex());
				allButThis = this.fLeft;
			} else if (rightIsVisible) {
				avpos = lvpos = rvpos = this.realToVirtualPosition(RIGHT_CONTRIBUTOR,
						this.fRight.getSourceViewer().getTopIndex());
				allButThis = this.fRight;
			} else if (ancestorIsVisible) {
				avpos = lvpos = rvpos = this.realToVirtualPosition(ANCESTOR_CONTRIBUTOR,
						this.fAncestor.getSourceViewer().getTopIndex());
				allButThis = this.fAncestor;
			} else {
				int vpos = 0;
				for (Iterator<?> iterator = this.fMerger.rangesIterator(); iterator.hasNext();) {
					Diff diff = (Diff) iterator.next();
					if (diff == d) {
						break;
					}
					if (this.fSynchronizedScrolling) {
						vpos += diff.getMaxDiffHeight();
					} else {
						avpos += diff.getAncestorHeight();
						lvpos += diff.getLeftHeight();
						rvpos += diff.getRightHeight();
					}
				}
				if (this.fSynchronizedScrolling) {
					avpos = lvpos = rvpos = vpos;
				}
				int delta = this.fRight.getViewportLines() / 4;
				avpos -= delta;
				if (avpos < 0) {
					avpos = 0;
				}
				lvpos -= delta;
				if (lvpos < 0) {
					lvpos = 0;
				}
				rvpos -= delta;
				if (rvpos < 0) {
					rvpos = 0;
				}
			}

			this.scrollVertical(avpos, lvpos, rvpos, allButThis);

			if (this.fVScrollBar != null) {
				this.fVScrollBar.setSelection(avpos);
			}
		}

		// horizontal scrolling
		if (d.isToken()) {
			// we only scroll horizontally for token diffs
			reveal(this.fAncestor, d.getPosition(ANCESTOR_CONTRIBUTOR));
			reveal(this.fLeft, d.getPosition(LEFT_CONTRIBUTOR));
			reveal(this.fRight, d.getPosition(RIGHT_CONTRIBUTOR));
		} else {
			// in all other cases we reset the horizontal offset
			hscroll(this.fAncestor);
			hscroll(this.fLeft);
			hscroll(this.fRight);
		}
	}

	private static void reveal(final MergeSourceViewer v, final Position p) {
		if (v != null && p != null) {
			StyledText st = v.getSourceViewer().getTextWidget();
			if (st != null) {
				Rectangle r = st.getClientArea();
				if (!r.isEmpty()) {
					v.getSourceViewer().revealRange(p.offset, p.length);
				}
			}
		}
	}

	private static void hscroll(final MergeSourceViewer v) {
		if (v != null) {
			StyledText st = v.getSourceViewer().getTextWidget();
			if (st != null) {
				st.setHorizontalIndex(0);
			}
		}
	}

	//--------------------------------------------------------------------------------

	void copyAllUnresolved(final boolean leftToRight) {
		if (this.fMerger.hasChanges() && this.isThreeWay() && !this.isIgnoreAncestor()) {
			IRewriteTarget target = leftToRight ? this.fRight.getSourceViewer().getRewriteTarget()
					: this.fLeft.getSourceViewer().getRewriteTarget();
			boolean compoundChangeStarted = false;
			try {
				for (Iterator<?> iterator = this.fMerger.changesIterator(); iterator.hasNext();) {
					Diff diff = (Diff) iterator.next();
					switch (diff.getKind()) {
					case RangeDifference.LEFT:
						if (leftToRight) {
							if (!compoundChangeStarted) {
								target.beginCompoundChange();
								compoundChangeStarted = true;
							}
							this.copy(diff, leftToRight);
						}
						break;
					case RangeDifference.RIGHT:
						if (!leftToRight) {
							if (!compoundChangeStarted) {
								target.beginCompoundChange();
								compoundChangeStarted = true;
							}
							this.copy(diff, leftToRight);
						}
						break;
					default:
						continue;
					}
				}
			} finally {
				if (compoundChangeStarted) {
					target.endCompoundChange();
				}
			}
		}
	}

	/*
	 * Copy whole document from one side to the other.
	 */
	@Override
	protected void copy(final boolean leftToRight) {
		if (!this.validateChange(!leftToRight)) {
			return;
		}
		if (this.showResolveUI()) {
			this.copyAllUnresolved(leftToRight);
			this.invalidateLines();
			return;
		}
		this.copyOperationInProgress = true;
		if (leftToRight) {
			if (this.fLeft.getEnabled()) {
				// copy text
				String text = this.fLeft.getSourceViewer().getTextWidget().getText();
				this.fRight.getSourceViewer().getTextWidget().setText(text);
				this.fRight.setEnabled(true);
			} else {
				// delete
				this.fRight.getSourceViewer().getTextWidget().setText(""); //$NON-NLS-1$
				this.fRight.setEnabled(false);
			}
			this.fRightLineCount = this.fRight.getLineCount();
			this.setRightDirty(true);
		} else {
			if (this.fRight.getEnabled()) {
				// copy text
				String text = this.fRight.getSourceViewer().getTextWidget().getText();
				this.fLeft.getSourceViewer().getTextWidget().setText(text);
				this.fLeft.setEnabled(true);
			} else {
				// delete
				this.fLeft.getSourceViewer().getTextWidget().setText(""); //$NON-NLS-1$
				this.fLeft.setEnabled(false);
			}
			this.fLeftLineCount = this.fLeft.getLineCount();
			this.setLeftDirty(true);
		}
		this.copyOperationInProgress = false;
		this.update(false);
		this.selectFirstDiff(true);
	}

	private void historyNotification(final OperationHistoryEvent event) {
		switch (event.getEventType()) {
		case OperationHistoryEvent.OPERATION_ADDED:
			if (this.copyOperationInProgress) {
				this.copyUndoable = event.getOperation();
			}
			break;
		case OperationHistoryEvent.UNDONE:
			if (this.copyUndoable == event.getOperation()) {
				this.update(false);
			}
			break;
		default:
			// Nothing to do
			break;
		}
	}

	private void copyDiffLeftToRight() {
		this.copy(this.fCurrentDiff, true, false);
	}

	private void copyDiffRightToLeft() {
		this.copy(this.fCurrentDiff, false, false);
	}

	/*
	 * Copy the contents of the given diff from one side to the other.
	 */
	private void copy(final Diff diff, final boolean leftToRight, final boolean gotoNext) {
		if (this.copy(diff, leftToRight)) {
			if (gotoNext) {
				this.navigate(true, true, false /* don't step in */);
			} else {
				this.revealDiff(diff, true);
				this.updateControls();
			}
		}
	}

	/*
	 * Copy the contents of the given diff from one side to the other but
	 * doesn't reveal anything.
	 * Returns true if copy was successful.
	 */
	private boolean copy(final Diff diff, final boolean leftToRight) {

		if (diff != null) {
			if (!this.validateChange(!leftToRight)) {
				return false;
			}
			if (leftToRight) {
				this.fRight.setEnabled(true);
			} else {
				this.fLeft.setEnabled(true);
			}
			boolean result = this.fMerger.copy(diff, leftToRight);
			if (result) {
				this.updateResolveStatus();
			}
			return result;
		}
		return false;
	}

	private boolean validateChange(final boolean left) {
		ContributorInfo info;
		if (left) {
			info = this.fLeftContributor;
		} else {
			info = this.fRightContributor;
		}

		return info.validateChange();
	}

	//---- scrolling

	/*
	 * The height of the TextEditors in lines.
	 */
	private int getViewportHeight() {
		StyledText te = this.fLeft.getSourceViewer().getTextWidget();

		int vh = te.getClientArea().height;
		if (vh == 0) {
			Rectangle trim = te.computeTrim(0, 0, 0, 0);
			int scrollbarHeight = trim.height;

			int headerHeight = this.getHeaderHeight();

			Composite composite = (Composite) this.getControl();
			Rectangle r = composite.getClientArea();

			vh = r.height - headerHeight - scrollbarHeight;
		}

		return vh / te.getLineHeight();
	}

	/*
	 * Returns the virtual position for the given view position.
	 */
	private int realToVirtualPosition(final char contributor, final int vpos) {
		if (!this.fSynchronizedScrolling) {
			return vpos;
		}
		return this.fMerger.realToVirtualPosition(contributor, vpos);
	}

	private void scrollVertical(final int avpos, final int lvpos, final int rvpos, final MergeSourceViewer allBut) {

		int s = 0;

		if (this.fSynchronizedScrolling) {
			s = this.fMerger.getVirtualHeight() - rvpos;
			int height = this.fRight.getViewportLines() / 4;
			if (s < 0) {
				s = 0;
			}
			if (s > height) {
				s = height;
			}
		}

		this.fInScrolling = true;

		if (this.isThreeWay() && allBut != this.fAncestor) {
			if (this.fSynchronizedScrolling || allBut == null) {
				int y = this.virtualToRealPosition(ANCESTOR_CONTRIBUTOR, avpos + s) - s;
				this.fAncestor.vscroll(y);
			}
		}

		if (allBut != this.fLeft) {
			if (this.fSynchronizedScrolling || allBut == null) {
				int y = this.virtualToRealPosition(LEFT_CONTRIBUTOR, lvpos + s) - s;
				this.fLeft.vscroll(y);
			}
		}

		if (allBut != this.fRight) {
			if (this.fSynchronizedScrolling || allBut == null) {
				int y = this.virtualToRealPosition(RIGHT_CONTRIBUTOR, rvpos + s) - s;
				this.fRight.vscroll(y);
			}
		}

		this.fInScrolling = false;

		if (this.isThreeWay() && this.fAncestorCanvas != null) {
			this.fAncestorCanvas.repaint();
		}

		if (this.fLeftCanvas != null) {
			this.fLeftCanvas.repaint();
		}

		Control center = this.getCenterControl();
		if (center instanceof BufferedCanvas) {
			((BufferedCanvas) center).repaint();
		}

		if (this.fRightCanvas != null) {
			this.fRightCanvas.repaint();
		}
	}

	/*
	 * Updates Scrollbars with viewports.
	 */
	private void syncViewport(final MergeSourceViewer w) {

		if (this.fInScrolling) {
			return;
		}

		int ix = w.getSourceViewer().getTopIndex();
		int ix2 = w.getDocumentRegionOffset();

		int viewPosition = this.realToVirtualPosition(this.getLeg(w), ix - ix2);

		this.scrollVertical(viewPosition, viewPosition, viewPosition, w); // scroll all but the given views

		if (this.fVScrollBar != null) {
			int value = Math.max(0, Math.min(viewPosition, this.fMerger.getVirtualHeight() - this.getViewportHeight()));
			this.fVScrollBar.setSelection(value);
			//refreshBirdEyeView();
		}
	}

	/**
	 */
	private void updateVScrollBar() {

		if (Utilities.okToUse(this.fVScrollBar) && this.fSynchronizedScrolling) {
			int virtualHeight = this.fMerger.getVirtualHeight();
			int viewPortHeight = this.getViewportHeight();
			int pageIncrement = viewPortHeight - 1;
			int thumb = (viewPortHeight > virtualHeight) ? virtualHeight : viewPortHeight;

			this.fVScrollBar.setPageIncrement(pageIncrement);
			this.fVScrollBar.setMaximum(virtualHeight);
			this.fVScrollBar.setThumb(thumb);
		}
	}

	/*
	 * maps given virtual position into a real view position of this view.
	 */
	private int virtualToRealPosition(final char contributor, final int v) {
		if (!this.fSynchronizedScrolling) {
			return v;
		}
		return this.fMerger.virtualToRealPosition(contributor, v);
	}

	@Override
	void flushLeftSide(final Object oldInput, final IProgressMonitor monitor) {
		IMergeViewerContentProvider content = this.getMergeContentProvider();
		Object leftContent = content.getLeftContent(oldInput);

		if (leftContent != null && this.isLeftEditable() && this.isLeftDirty()) {
			if (this.fLeftContributor.hasSharedDocument(leftContent)) {
				if (this.flush(this.fLeftContributor)) {
					this.setLeftDirty(false);
				}
			}
		}

		if (!(content instanceof MergeViewerContentProvider) || this.isLeftDirty()) {
			super.flushLeftSide(oldInput, monitor);
		}
	}

	@Override
	void flushRightSide(final Object oldInput, final IProgressMonitor monitor) {
		IMergeViewerContentProvider content = this.getMergeContentProvider();
		Object rightContent = content.getRightContent(oldInput);

		if (rightContent != null && this.isRightEditable() && this.isRightDirty()) {
			if (this.fRightContributor.hasSharedDocument(rightContent)) {
				if (this.flush(this.fRightContributor)) {
					this.setRightDirty(false);
				}
			}
		}

		if (!(content instanceof MergeViewerContentProvider) || this.isRightDirty()) {
			super.flushRightSide(oldInput, monitor);
		}
	}

	@Override
	protected void flushContent(final Object oldInput, final IProgressMonitor monitor) {
		this.flushLeftSide(oldInput, monitor);
		this.flushRightSide(oldInput, monitor);

		IMergeViewerContentProvider content = this.getMergeContentProvider();

		if (!(content instanceof MergeViewerContentProvider) || this.isLeftDirty() || this.isRightDirty()) {
			super.flushContent(oldInput, monitor);
		}
	}

	private boolean flush(final ContributorInfo info) {
		try {
			return info.flush();
		} catch (CoreException e) {
			this.handleException(e);
		}
		return false;
	}

	private void handleException(final Throwable throwable) {
		// TODO: Should show error to the user
		if (throwable instanceof InvocationTargetException) {
			InvocationTargetException ite = (InvocationTargetException) throwable;
			this.handleException(ite.getTargetException());
			return;
		}
		CompareUIPlugin.log(throwable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IMergeViewerTestAdapter.class) {
			return (T) new IMergeViewerTestAdapter() {
				@Override
				public IDocument getDocument(final char leg) {
					switch (leg) {
					case LEFT_CONTRIBUTOR:
						return TextMergeViewer.this.fLeft.getSourceViewer().getDocument();
					case RIGHT_CONTRIBUTOR:
						return TextMergeViewer.this.fRight.getSourceViewer().getDocument();
					case ANCESTOR_CONTRIBUTOR:
						return TextMergeViewer.this.fAncestor.getSourceViewer().getDocument();
					default:
						return null;
					}
				}

				@Override
				public int getChangesCount() {
					return TextMergeViewer.this.fMerger.changesCount();
				}
			};
		}
		if (adapter == OutlineViewerCreator.class) {
			if (this.fOutlineViewerCreator == null) {
				this.fOutlineViewerCreator = new InternalOutlineViewerCreator();
			}
			return (T) this.fOutlineViewerCreator;

		}
		if (adapter == IFindReplaceTarget.class) {
			return (T) this.getFindReplaceTarget();
		}
		if (adapter == CompareHandlerService.class) {
			return (T) this.fHandlerService;
		}
		if (adapter == CompareHandlerService[].class) {
			return (T) new CompareHandlerService[] { this.fHandlerService,
					super.getCompareHandlerService() };
		}
		if (adapter == IEditorInput.class) {
			// return active editor input
			if (this.fLeft != null && this.fLeft == this.fFocusPart) {
				if (this.fLeftContributor != null) {
					return (T) this.fLeftContributor.getDocumentKey();
				}
			}
			if (this.fRight != null && this.fRight == this.fFocusPart) {
				if (this.fRightContributor != null) {
					return (T) this.fRightContributor.getDocumentKey();
				}
			}
			if (this.fAncestor != null && this.fAncestor == this.fFocusPart) {
				if (this.fAncestorContributor != null) {
					return (T) this.fAncestorContributor.getDocumentKey();
				}
			}
		}
		return null;
	}

	@Override
	protected void handleCompareInputChange() {
		try {
			this.beginRefresh();
			super.handleCompareInputChange();
		} finally {
			this.endRefresh();
		}
	}

	private void beginRefresh() {
		this.isRefreshing++;
		this.fLeftContributor.cacheSelection(this.fLeft);
		this.fRightContributor.cacheSelection(this.fRight);
		this.fAncestorContributor.cacheSelection(this.fAncestor);
		if (this.fSynchronizedScrolling) {
			this.fSynchronziedScrollPosition = this.fVScrollBar.getSelection();
		}

	}

	private void endRefresh() {
		this.isRefreshing--;
		this.fLeftContributor.cacheSelection(null);
		this.fRightContributor.cacheSelection(null);
		this.fAncestorContributor.cacheSelection(null);
		this.fSynchronziedScrollPosition = -1;
	}

	private void synchronizedScrollVertical(final int vpos) {
		this.scrollVertical(vpos, vpos, vpos, null);
	}

	private boolean isIgnoreAncestor() {
		return Utilities.getBoolean(this.getCompareConfiguration(), ICompareUIConstants.PROP_IGNORE_ANCESTOR, false);
	}

	/* package */ void update(final boolean includeControls) {
		if (this.getControl().isDisposed()) {
			return;
		}
		if (this.fHasErrors) {
			this.resetDiffs();
		} else {
			this.doDiff();
		}

		if (includeControls) {
			this.updateControls();
		}

		this.updateVScrollBar();
		this.updatePresentation();
	}

	private void resetDiffs() {
		// clear stuff
		this.saveDiff();
		this.fCurrentDiff = null;
		this.fMerger.reset();
		this.resetPositions(this.fLeft.getSourceViewer().getDocument());
		this.resetPositions(this.fRight.getSourceViewer().getDocument());
		this.resetPositions(this.fAncestor.getSourceViewer().getDocument());
	}

	private boolean isPatchHunk() {
		return Utilities.isHunk(this.getInput());
	}

	private boolean isPatchHunkOk() {
		if (this.isPatchHunk()) {
			return Utilities.isHunkOk(this.getInput());
		}
		return false;
	}

	/**
	 * Return the provided start position of the hunk in the target file.
	 * @return the provided start position of the hunk in the target file
	 */
	private int getHunkStart() {
		Object input = this.getInput();
		if (input != null && input instanceof DiffNode) {
			ITypedElement right = ((DiffNode) input).getRight();
			if (right != null) {
				Object element = Adapters.adapt(right, IHunk.class);
				if (element instanceof IHunk) {
					return ((IHunk) element).getStartPosition();
				}
			}
			ITypedElement left = ((DiffNode) input).getLeft();
			if (left != null) {
				Object element = Adapters.adapt(left, IHunk.class);
				if (element instanceof IHunk) {
					return ((IHunk) element).getStartPosition();
				}
			}
		}
		return 0;
	}

	private IFindReplaceTarget getFindReplaceTarget() {
		if (this.fFindReplaceTarget == null) {
			this.fFindReplaceTarget = new FindReplaceTarget();
		}
		return this.fFindReplaceTarget;
	}

	/* package */ char getLeg(final MergeSourceViewer w) {
		if (w == this.fLeft) {
			return LEFT_CONTRIBUTOR;
		}
		if (w == this.fRight) {
			return RIGHT_CONTRIBUTOR;
		}
		if (w == this.fAncestor) {
			return ANCESTOR_CONTRIBUTOR;
		}
		return ANCESTOR_CONTRIBUTOR;
	}

	private boolean isCurrentDiff(final Diff diff) {
		if (diff == null) {
			return false;
		}
		if (diff == this.fCurrentDiff) {
			return true;
		}
		if (this.fCurrentDiff != null && this.fCurrentDiff.getParent() == diff) {
			return true;
		}
		return false;
	}

	private boolean isNavigationPossible() {
		if (this.fCurrentDiff == null && this.fMerger.hasChanges()) {
			return true;
		} else if (this.fMerger.changesCount() > 1) {
			return true;
		} else if (this.fCurrentDiff != null && this.fCurrentDiff.hasChildren()) {
			return true;
		} else if (this.fCurrentDiff != null && this.fCurrentDiff.isToken()) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns {@link ITextEditor} used in the
	 * {@link ChangeEncodingAction}. It provides implementation of methods that
	 * are used by the action by delegating them to {@link ContributorInfo} that
	 * corresponds to the side that has focus.
	 *
	 * @return the text editor adapter
	 */
	private ITextEditor getTextEditorAdapter() {
		return new ITextEditor() {
			@Override
			public void close(final boolean save) {
			}

			@Override
			public void doRevertToSaved() {
			}

			@Override
			public IAction getAction(final String actionId) {
				return null;
			}

			@Override
			public IDocumentProvider getDocumentProvider() {
				return null;
			}

			@Override
			public IRegion getHighlightRange() {
				return null;
			}

			@Override
			public ISelectionProvider getSelectionProvider() {
				return null;
			}

			@Override
			public boolean isEditable() {
				return false;
			}

			@Override
			public void removeActionActivationCode(final String actionId) {
			}

			@Override
			public void resetHighlightRange() {
			}

			@Override
			public void selectAndReveal(final int offset, final int length) {
			}

			@Override
			public void setAction(final String actionId, final IAction action) {
			}

			@Override
			public void setActionActivationCode(final String actionId,
					final char activationCharacter, final int activationKeyCode,
					final int activationStateMask) {
			}

			@Override
			public void setHighlightRange(final int offset, final int length, final boolean moveCursor) {
			}

			@Override
			public void showHighlightRangeOnly(final boolean showHighlightRangeOnly) {
			}

			@Override
			public boolean showsHighlightRangeOnly() {
				return false;
			}

			@Override
			public IEditorInput getEditorInput() {
				if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fAncestor
						&& TextMergeViewer.this.fAncestorContributor != null) {
					return TextMergeViewer.this.fAncestorContributor.getDocumentKey();
				} else if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fLeft
						&& TextMergeViewer.this.fLeftContributor != null) {
					return TextMergeViewer.this.fLeftContributor.getDocumentKey();
				} else if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fRight
						&& TextMergeViewer.this.fRightContributor != null) {
					return TextMergeViewer.this.fRightContributor.getDocumentKey();
				} else {
					return null;
				}
			}

			@Override
			public IEditorSite getEditorSite() {
				return null;
			}

			@Override
			public void init(final IEditorSite site, final IEditorInput input)
					throws PartInitException {
			}

			@Override
			public void addPropertyListener(final IPropertyListener listener) {
			}

			@Override
			public void createPartControl(final Composite parent) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public IWorkbenchPartSite getSite() {
				return new IWorkbenchPartSite() {
					@Override
					public String getId() {
						return null;
					}

					@Override
					@Deprecated
					public IKeyBindingService getKeyBindingService() {
						return null;
					}

					@Override
					public IWorkbenchPart getPart() {
						return null;
					}

					@Override
					public String getPluginId() {
						return null;
					}

					@Override
					public String getRegisteredName() {
						return null;
					}

					@Override
					public void registerContextMenu(final MenuManager menuManager,
							final ISelectionProvider selectionProvider) {
					}

					@Override
					public void registerContextMenu(final String menuId,
							final MenuManager menuManager,
							final ISelectionProvider selectionProvider) {
					}

					@Override
					public IWorkbenchPage getPage() {
						return null;
					}

					@Override
					public ISelectionProvider getSelectionProvider() {
						return null;
					}

					@Override
					public Shell getShell() {
						return TextMergeViewer.this.fComposite.getShell();
					}

					@Override
					public IWorkbenchWindow getWorkbenchWindow() {
						return null;
					}

					@Override
					public void setSelectionProvider(final ISelectionProvider provider) {
					}

					@Override
					public <T> T getAdapter(final Class<T> adapter) {
						return null;
					}

					@Override
					public <T> T getService(final Class<T> api) {
						return null;
					}

					@Override
					public boolean hasService(final Class<?> api) {
						return false;
					}
				};
			}

			@Override
			public String getTitle() {
				return null;
			}

			@Override
			public Image getTitleImage() {
				return null;
			}

			@Override
			public String getTitleToolTip() {
				return null;
			}

			@Override
			public void removePropertyListener(final IPropertyListener listener) {
			}

			@Override
			public void setFocus() {
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> T getAdapter(final Class<T> adapter) {
				if (adapter == IEncodingSupport.class) {
					if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fAncestor) {
						return (T) this.getEncodingSupport(TextMergeViewer.this.fAncestorContributor);
					} else if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fLeft) {
						return (T) this.getEncodingSupport(TextMergeViewer.this.fLeftContributor);
					} else if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fRight) {
						return (T) this.getEncodingSupport(TextMergeViewer.this.fRightContributor);
					}
				}
				return null;
			}

			private IEncodingSupport getEncodingSupport(final ContributorInfo contributor) {
				if (contributor != null && contributor.getDefaultEncoding() != null) {
					return contributor;
				}
				return null;
			}

			@Override
			public void doSave(final IProgressMonitor monitor) {
			}

			@Override
			public void doSaveAs() {
			}

			@Override
			public boolean isDirty() {
				if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fLeft) {
					return TextMergeViewer.this.isLeftDirty();
				} else if (TextMergeViewer.this.fFocusPart == TextMergeViewer.this.fRight) {
					return TextMergeViewer.this.isRightDirty();
				}
				return false;
			}

			@Override
			public boolean isSaveAsAllowed() {
				// Implementing interface method
				return false;
			}

			@Override
			public boolean isSaveOnCloseNeeded() {
				// Implementing interface method
				return false;
			}
		};
	}

	private void updateStructure() {
		this.getCompareConfiguration().setProperty("ALL_STRUCTURE_REFRESH", null); //$NON-NLS-1$
	}

	private void updateStructure(final char leg) {
		String key = null;
		switch (leg) {
		case ANCESTOR_CONTRIBUTOR:
			key = "ANCESTOR_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		case LEFT_CONTRIBUTOR:
			key = "LEFT_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		case RIGHT_CONTRIBUTOR:
			key = "RIGHT_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		default:
			break;
		}
		Assert.isNotNull(key);
		this.getCompareConfiguration().setProperty(key, null);
	}

	public void scroll(final int leftLineNumber, final int rightLineNumber) {
		char contributor;
		//		boolean isLeft;
		int line;
		MergeSourceViewer mergeSourceViewer;

		if (leftLineNumber != -1) {
			contributor = LEFT_CONTRIBUTOR;
			//			isLeft = true;
			line = leftLineNumber;
			mergeSourceViewer = this.fLeft;
		} else if (rightLineNumber != -1) {
			contributor = RIGHT_CONTRIBUTOR;
			//			isLeft = false;
			line = rightLineNumber;
			mergeSourceViewer = this.fRight;
		} else {
			return;
		}

		// Subtract 1 to change to 0-based
		line--;

		//		line = this.fMerger.realToVirtualPosition(contributor, line);
		int offset = this.fMerger.getOffset(contributor, line);
		Position position = new Position(offset);
		//		Diff diff = this.fMerger.findDiff(position, isLeft);

		if (leftLineNumber != -1 && rightLineNumber != -1) {
			// Doesn't correct align left and right
			//			this.scrollVertical(0, leftLineNumber - 1, rightLineNumber - 1, this.fAncestor);

			reveal(mergeSourceViewer, position);
			this.fLeft.vscroll(leftLineNumber - 1);
			//			this.fRight.vscroll(rightLineNumber - 1);
			//			this.fLeft.vscroll(leftLineNumber - 1);
			//			this.fLeft.setSelection(position);
			//			this.fRight.setSelection(new Position(rightLineNumber - 1));
			//			this.fLeft.setRegion(new Position(leftLineNumber - 1));
			//			this.fRight.setRegion(new Position(rightLineNumber - 1));
		} else {
			reveal(mergeSourceViewer, position);
		}

		mergeSourceViewer.setSelection(position);

		//		this.revealDiff(diff, true);
	}
}
