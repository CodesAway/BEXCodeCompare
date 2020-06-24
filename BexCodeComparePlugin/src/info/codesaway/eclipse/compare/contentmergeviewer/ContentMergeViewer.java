/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Stefan Xenos <sxenos@gmail.com> (Google) - bug 448968 - Add diagnostic logging
 *     Conrad Groth - Bug 213780 - Compare With direction should be configurable
 *******************************************************************************/

package info.codesaway.eclipse.compare.contentmergeviewer;

import java.io.IOException;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.ICompareInputLabelProvider;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;
import org.eclipse.compare.internal.ChangePropertyAction;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareHandlerService;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.IFlushable2;
import org.eclipse.compare.internal.ISavingSaveable;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.MirroredMergeViewerContentProvider;
import org.eclipse.compare.internal.Policy;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.ViewerSwitchingCancelled;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.Saveable;

/**
 * An abstract compare and merge viewer with two side-by-side content areas and
 * an optional content area for the ancestor. The implementation makes no
 * assumptions about the content type.
 * <p>
 * <code>ContentMergeViewer</code>
 * </p>
 * <ul>
 * <li>implements the overall layout and defines hooks so that subclasses can
 * easily provide an implementation for a specific content type,
 * <li>implements the UI for making the areas resizable,
 * <li>has an action for controlling whether the ancestor area is visible or
 * not,
 * <li>has actions for copying one side of the input to the other side,
 * <li>tracks the dirty state of the left and right sides and send out
 * notification on state changes.
 * </ul>
 * <p>
 * A <code>ContentMergeViewer</code> accesses its model by means of a content
 * provider which must implement the <code>IMergeViewerContentProvider</code>
 * interface.
 * </p>
 * <p>
 * Clients may wish to use the standard concrete subclass
 * <code>TextMergeViewer</code>, or define their own subclass.
 * </p>
 *
 * @see IMergeViewerContentProvider
 * @see TextMergeViewer
 */
public abstract class ContentMergeViewer extends ContentViewer
		implements IPropertyChangeNotifier, IFlushable, IFlushable2 {
	/* package */ static final int HORIZONTAL = 1;
	/* package */ static final int VERTICAL = 2;

	static final double HSPLIT = 0.5;
	static final double VSPLIT = 0.3;

	private class ContentMergeViewerLayout extends Layout {
		@Override
		public Point computeSize(final Composite c, final int w, final int h, final boolean force) {
			return new Point(100, 100);
		}

		@Override
		public void layout(final Composite composite, final boolean force) {
			if (ContentMergeViewer.this.fLeftLabel == null) {
				if (composite.isDisposed()) {
					CompareUIPlugin
							.log(new IllegalArgumentException("Attempted to perform a layout on a disposed composite")); //$NON-NLS-1$
				}
				if (Policy.debugContentMergeViewer) {
					ContentMergeViewer.this
							.logTrace("found bad label. Layout = " + System.identityHashCode(this) + ". composite = " //$NON-NLS-1$//$NON-NLS-2$
									+ System.identityHashCode(composite) + ". fComposite = " //$NON-NLS-1$
									+ System.identityHashCode(ContentMergeViewer.this.fComposite)
									+ ". fComposite.isDisposed() = " //$NON-NLS-1$
									+ ContentMergeViewer.this.fComposite.isDisposed());
					ContentMergeViewer.this.logStackTrace();
				}
				// Help to find out the cause for bug 449558
				NullPointerException npe = new NullPointerException(
						"fLeftLabel is 'null';fLeftLabelSet is " + ContentMergeViewer.this.fLeftLabelSet //$NON-NLS-1$
								+ ";fComposite.isDisposed() is " + ContentMergeViewer.this.fComposite.isDisposed()); //$NON-NLS-1$

				// Allow to test whether doing nothing helps
				if (Boolean.getBoolean("ContentMergeViewer.DEBUG")) { //$NON-NLS-1$
					CompareUIPlugin.log(npe);
					return;
				}

				throw npe;
			}

			// determine some derived sizes
			int headerHeight = ContentMergeViewer.this.fLeftLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y;
			Rectangle r = composite.getClientArea();

			int centerWidth = ContentMergeViewer.this.getCenterWidth();
			int width1 = (int) ((r.width - centerWidth) * this.getHorizontalSplitRatio());
			int width2 = r.width - width1 - centerWidth;

			int height1 = 0;
			int height2 = 0;
			if (ContentMergeViewer.this.fIsThreeWay && ContentMergeViewer.this.fAncestorVisible) {
				height1 = (int) ((r.height - (2 * headerHeight)) * ContentMergeViewer.this.fVSplit);
				height2 = r.height - (2 * headerHeight) - height1;
			} else {
				height1 = 0;
				height2 = r.height - headerHeight;
			}

			int y = 0;

			if (ContentMergeViewer.this.fIsThreeWay && ContentMergeViewer.this.fAncestorVisible) {
				ContentMergeViewer.this.fAncestorLabel.setBounds(0, y, r.width, headerHeight);
				ContentMergeViewer.this.fAncestorLabel.setVisible(true);
				y += headerHeight;
				ContentMergeViewer.this.handleResizeAncestor(0, y, r.width, height1);
				y += height1;
			} else {
				ContentMergeViewer.this.fAncestorLabel.setVisible(false);
				ContentMergeViewer.this.handleResizeAncestor(0, 0, 0, 0);
			}

			ContentMergeViewer.this.fLeftLabel.getSize(); // without this resizing would not always work

			if (centerWidth > 3) {
				ContentMergeViewer.this.fLeftLabel.setBounds(0, y, width1 + 1, headerHeight);
				ContentMergeViewer.this.fDirectionLabel.setVisible(true);
				ContentMergeViewer.this.fDirectionLabel.setBounds(width1 + 1, y, centerWidth - 1, headerHeight);
				ContentMergeViewer.this.fRightLabel.setBounds(width1 + centerWidth, y, width2, headerHeight);
			} else {
				ContentMergeViewer.this.fLeftLabel.setBounds(0, y, width1, headerHeight);
				ContentMergeViewer.this.fDirectionLabel.setVisible(false);
				ContentMergeViewer.this.fRightLabel.setBounds(width1, y, r.width - width1, headerHeight);
			}

			y += headerHeight;

			if (ContentMergeViewer.this.fCenter != null && !ContentMergeViewer.this.fCenter.isDisposed()) {
				ContentMergeViewer.this.fCenter.setBounds(width1, y, centerWidth, height2);
			}

			ContentMergeViewer.this.handleResizeLeftRight(0, y, width1, centerWidth, width2, height2);
		}

		private double getHorizontalSplitRatio() {
			if (ContentMergeViewer.this.fHSplit < 0) {
				Object input = ContentMergeViewer.this.getInput();
				if (input instanceof ICompareInput) {
					ICompareInput ci = (ICompareInput) input;
					if (ci.getLeft() == null) {
						return 0.1;
					}
					if (ci.getRight() == null) {
						return 0.9;
					}
				}
				return HSPLIT;
			}
			return ContentMergeViewer.this.fHSplit;
		}
	}

	class Resizer extends MouseAdapter implements MouseMoveListener {
		Control fControl;
		int fX, fY;
		int fWidth1, fWidth2;
		int fHeight1, fHeight2;
		int fDirection;
		boolean fLiveResize;
		boolean fIsDown;

		public Resizer(final Control c, final int dir) {
			this.fDirection = dir;
			this.fControl = c;
			this.fLiveResize = !(this.fControl instanceof Sash);
			ContentMergeViewer.this.updateCursor(c, dir);
			this.fControl.addMouseListener(this);
			this.fControl.addMouseMoveListener(this);
			this.fControl.addDisposeListener(
					e -> this.fControl = null);
		}

		@Override
		public void mouseDoubleClick(final MouseEvent e) {
			if ((this.fDirection & HORIZONTAL) != 0) {
				ContentMergeViewer.this.fHSplit = -1;
			}
			if ((this.fDirection & VERTICAL) != 0) {
				ContentMergeViewer.this.fVSplit = VSPLIT;
			}
			ContentMergeViewer.this.fComposite.layout(true);
		}

		@Override
		public void mouseDown(final MouseEvent e) {
			Composite parent = this.fControl.getParent();

			Point s = parent.getSize();
			Point as = ContentMergeViewer.this.fAncestorLabel.getSize();
			Point ys = ContentMergeViewer.this.fLeftLabel.getSize();
			Point ms = ContentMergeViewer.this.fRightLabel.getSize();

			this.fWidth1 = ys.x;
			this.fWidth2 = ms.x;
			this.fHeight1 = ContentMergeViewer.this.fLeftLabel.getLocation().y - as.y;
			this.fHeight2 = s.y - (ContentMergeViewer.this.fLeftLabel.getLocation().y + ys.y);

			this.fX = e.x;
			this.fY = e.y;
			this.fIsDown = true;
		}

		@Override
		public void mouseUp(final MouseEvent e) {
			this.fIsDown = false;
			if (!this.fLiveResize) {
				this.resize(e);
			}
		}

		@Override
		public void mouseMove(final MouseEvent e) {
			if (this.fIsDown && this.fLiveResize) {
				this.resize(e);
			}
		}

		private void resize(final MouseEvent e) {
			int dx = e.x - this.fX;
			int dy = e.y - this.fY;

			int centerWidth = ContentMergeViewer.this.fCenter.getSize().x;

			if (this.fWidth1 + dx > centerWidth && this.fWidth2 - dx > centerWidth) {
				this.fWidth1 += dx;
				this.fWidth2 -= dx;
				if ((this.fDirection & HORIZONTAL) != 0) {
					ContentMergeViewer.this.fHSplit = (double) this.fWidth1 / (double) (this.fWidth1 + this.fWidth2);
				}
			}
			if (this.fHeight1 + dy > centerWidth && this.fHeight2 - dy > centerWidth) {
				this.fHeight1 += dy;
				this.fHeight2 -= dy;
				if ((this.fDirection & VERTICAL) != 0) {
					ContentMergeViewer.this.fVSplit = (double) this.fHeight1 / (double) (this.fHeight1 + this.fHeight2);
				}
			}

			ContentMergeViewer.this.fComposite.layout(true);
			this.fControl.getDisplay().update();
		}
	}

	/** Style bits for top level composite */
	private final int fStyles;
	private final ResourceBundle fBundle;
	private final CompareConfiguration fCompareConfiguration;
	private IPropertyChangeListener fPropertyChangeListener;
	private IPropertyChangeListener fPreferenceChangeListener;
	private final ICompareInputChangeListener fCompareInputChangeListener;
	private ListenerList<IPropertyChangeListener> fListenerList;
	boolean fConfirmSave = true;

	private double fHSplit = -1; // width ratio of left and right panes
	private double fVSplit = VSPLIT; // height ratio of ancestor and bottom panes

	private boolean fIsThreeWay; // whether their is an ancestor
	private boolean fAncestorVisible; // whether the ancestor pane is visible
	private ActionContributionItem fAncestorItem;

	private ActionContributionItem copyLeftToRightItem; // copy from left to right
	private ActionContributionItem copyRightToLeftItem; // copy from right to left

	private boolean fIsLeftDirty;
	private boolean fIsRightDirty;

	private CompareHandlerService fHandlerService;

	private final MergeViewerContentProvider fDefaultContentProvider;
	private Action fSwitchLeftAndRight;

	// SWT widgets
	/* package */ Composite fComposite;
	private CLabel fAncestorLabel;
	private CLabel fLeftLabel;

	private boolean fLeftLabelSet = false; // needed for debug output for bug 449558
	private CLabel fRightLabel;
	/* package */ CLabel fDirectionLabel;
	/* package */ Control fCenter;

	//---- SWT resources to be disposed
	private Image fRightArrow;
	private Image fLeftArrow;
	private Image fBothArrow;
	private Cursor fNormalCursor;
	private Cursor fHSashCursor;
	private Cursor fVSashCursor;
	private Cursor fHVSashCursor;

	private final ILabelProviderListener labelChangeListener = event -> {
		Object[] elements = event.getElements();
		for (Object object : elements) {
			if (object == this.getInput()) {
				this.updateHeader();
			}
		}
	};

	//---- end

	/**
	 * Creates a new content merge viewer and initializes with a resource bundle and a
	 * configuration.
	 *
	 * @param style SWT style bits
	 * @param bundle the resource bundle
	 * @param cc the configuration object
	 */
	protected ContentMergeViewer(final int style, final ResourceBundle bundle, final CompareConfiguration cc) {
		if (Policy.debugContentMergeViewer) {
			this.logTrace("constructed (fLeftLabel == null)"); //$NON-NLS-1$
			this.logStackTrace();
		}

		this.fStyles = style & ~(SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT); // remove BIDI direction bits
		this.fBundle = bundle;

		this.fAncestorVisible = Utilities.getBoolean(cc, ICompareUIConstants.PROP_ANCESTOR_VISIBLE,
				this.fAncestorVisible);
		this.fConfirmSave = Utilities.getBoolean(cc, CompareEditor.CONFIRM_SAVE_PROPERTY, this.fConfirmSave);

		this.fCompareInputChangeListener = input -> {
			if (input == this.getInput()) {
				this.handleCompareInputChange();
			}
		};

		// Make sure the compare configuration is not null
		this.fCompareConfiguration = cc != null ? cc : new CompareConfiguration();
		this.fPropertyChangeListener = this::handlePropertyChangeEvent;
		this.fCompareConfiguration.addPropertyChangeListener(this.fPropertyChangeListener);
		this.fPreferenceChangeListener = event -> {
			if (event.getProperty().equals(ComparePreferencePage.SWAPPED)) {
				this.getCompareConfiguration().setProperty(CompareConfiguration.MIRRORED, event.getNewValue());
				this.updateContentProvider();
				this.updateToolItems();
			}
		};
		cc.getPreferenceStore().addPropertyChangeListener(this.fPreferenceChangeListener);

		this.fDefaultContentProvider = new MergeViewerContentProvider(this.fCompareConfiguration);
		this.updateContentProvider();

		this.fIsLeftDirty = false;
		this.fIsRightDirty = false;
	}

	private void logStackTrace() {
		new Exception("<Fake exception> in " + this.getClass().getName()).printStackTrace(System.out); //$NON-NLS-1$
	}

	private void logTrace(final String string) {
		System.out.println("ContentMergeViewer " + System.identityHashCode(this) + ": " + string); //$NON-NLS-1$//$NON-NLS-2$
	}

	//---- hooks ---------------------

	/**
	 * Returns the viewer's name.
	 *
	 * @return the viewer's name
	 */
	public String getTitle() {
		return Utilities.getString(this.getResourceBundle(), "title"); //$NON-NLS-1$
	}

	/**
	 * Creates the SWT controls for the ancestor, left, and right
	 * content areas of this compare viewer.
	 * Implementations typically hold onto the controls
	 * so that they can be initialized with the input objects in method
	 * <code>updateContent</code>.
	 *
	 * @param composite the container for the three areas
	 */
	abstract protected void createControls(Composite composite);

	/**
	 * Lays out the ancestor area of the compare viewer.
	 * It is called whenever the viewer is resized or when the sashes between
	 * the areas are moved to adjust the size of the areas.
	 *
	 * @param x the horizontal position of the ancestor area within its container
	 * @param y the vertical position of the ancestor area within its container
	 * @param width the width of the ancestor area
	 * @param height the height of the ancestor area
	 */
	abstract protected void handleResizeAncestor(int x, int y, int width, int height);

	/**
	 * Lays out the left and right areas of the compare viewer.
	 * It is called whenever the viewer is resized or when the sashes between
	 * the areas are moved to adjust the size of the areas.
	 *
	 * @param x the horizontal position of the left area within its container
	 * @param y the vertical position of the left and right area within its container
	 * @param leftWidth the width of the left area
	 * @param centerWidth the width of the gap between the left and right areas
	 * @param rightWidth the width of the right area
	 * @param height the height of the left and right areas
	 */
	abstract protected void handleResizeLeftRight(int x, int y, int leftWidth, int centerWidth,
			int rightWidth, int height);

	/**
	 * Contributes items to the given <code>ToolBarManager</code>.
	 * It is called when this viewer is installed in its container and if the container
	 * has a <code>ToolBarManager</code>.
	 * The <code>ContentMergeViewer</code> implementation of this method does nothing.
	 * Subclasses may reimplement.
	 *
	 * @param toolBarManager the toolbar manager to contribute to
	 */
	protected void createToolItems(final ToolBarManager toolBarManager) {
		// empty implementation
	}

	/**
	 * Initializes the controls of the three content areas with the given input objects.
	 *
	 * @param ancestor the input for the ancestor area
	 * @param left the input for the left area
	 * @param right the input for the right area
	 */
	abstract protected void updateContent(Object ancestor, Object left, Object right);

	/**
	 * Copies the content of one side to the other side.
	 * Called from the (internal) actions for copying the sides of the viewer's input object.
	 *
	 * @param leftToRight if <code>true</code>, the left side is copied to the right side;
	 * if <code>false</code>, the right side is copied to the left side
	 */
	abstract protected void copy(boolean leftToRight);

	/**
	 * Returns the byte contents of the left or right side. If the viewer
	 * has no editable content <code>null</code> can be returned.
	 *
	 * @param left if <code>true</code>, the byte contents of the left area is returned;
	 * 	if <code>false</code>, the byte contents of the right area
	 * @return the content as an array of bytes, or <code>null</code>
	 */
	abstract protected byte[] getContents(boolean left);

	//----------------------------

	/**
	 * Returns the resource bundle of this viewer.
	 *
	 * @return the resource bundle
	 */
	protected ResourceBundle getResourceBundle() {
		return this.fBundle;
	}

	/**
	 * Returns the compare configuration of this viewer.
	 *
	 * @return the compare configuration, never <code>null</code>
	 */
	protected CompareConfiguration getCompareConfiguration() {
		return this.fCompareConfiguration;
	}

	/**
	 * The <code>ContentMergeViewer</code> implementation of this
	 * <code>ContentViewer</code> method
	 * checks to ensure that the content provider is an <code>IMergeViewerContentProvider</code>.
	 * @param contentProvider the content provider to set. Must implement IMergeViewerContentProvider.
	 */
	@Override
	public void setContentProvider(final IContentProvider contentProvider) {
		Assert.isTrue(contentProvider instanceof IMergeViewerContentProvider);
		super.setContentProvider(contentProvider);
	}

	private void updateContentProvider() {
		this.setContentProvider(this.getCompareConfiguration().isMirrored()
				? new MirroredMergeViewerContentProvider(this.getCompareConfiguration(), this.fDefaultContentProvider)
				: this.fDefaultContentProvider);
	}

	/* package */ IMergeViewerContentProvider getMergeContentProvider() {
		return (IMergeViewerContentProvider) this.getContentProvider();
	}

	/**
	 * The <code>ContentMergeViewer</code> implementation of this
	 * <code>Viewer</code> method returns the empty selection. Subclasses may override.
	 * @return empty selection.
	 */
	@Override
	public ISelection getSelection() {
		return () -> true;
	}

	/**
	 * The <code>ContentMergeViewer</code> implementation of this
	 * <code>Viewer</code> method does nothing. Subclasses may reimplement.
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	@Override
	public void setSelection(final ISelection selection, final boolean reveal) {
		// Empty implementation.
	}

	/**
	 * Callback that is invoked when a property in the compare configuration
	 * ({@link #getCompareConfiguration()} changes.
	 * @param event the property change event
	 * @since 3.3
	 */
	protected void handlePropertyChangeEvent(final PropertyChangeEvent event) {
		String key = event.getProperty();

		if (key.equals(ICompareUIConstants.PROP_ANCESTOR_VISIBLE)) {
			this.fAncestorVisible = Utilities.getBoolean(this.getCompareConfiguration(),
					ICompareUIConstants.PROP_ANCESTOR_VISIBLE, this.fAncestorVisible);
			this.fComposite.layout(true);

			this.updateCursor(this.fLeftLabel, VERTICAL);
			this.updateCursor(this.fDirectionLabel, HORIZONTAL | VERTICAL);
			this.updateCursor(this.fRightLabel, VERTICAL);

			return;
		}

		if (key.equals(ICompareUIConstants.PROP_IGNORE_ANCESTOR)) {
			this.setAncestorVisibility(false, !Utilities.getBoolean(this.getCompareConfiguration(),
					ICompareUIConstants.PROP_IGNORE_ANCESTOR, false));
			return;
		}
	}

	void updateCursor(final Control c, final int dir) {
		if (!(c instanceof Sash)) {
			Cursor cursor = null;
			switch (dir) {
			case VERTICAL:
				if (this.fAncestorVisible) {
					if (this.fVSashCursor == null) {
						this.fVSashCursor = c.getDisplay().getSystemCursor(SWT.CURSOR_SIZENS);
					}
					cursor = this.fVSashCursor;
				} else {
					if (this.fNormalCursor == null) {
						this.fNormalCursor = c.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
					}
					cursor = this.fNormalCursor;
				}
				break;
			case HORIZONTAL:
				if (this.fHSashCursor == null) {
					this.fHSashCursor = c.getDisplay().getSystemCursor(SWT.CURSOR_SIZEWE);
				}
				cursor = this.fHSashCursor;
				break;
			case VERTICAL + HORIZONTAL:
				if (this.fAncestorVisible) {
					if (this.fHVSashCursor == null) {
						this.fHVSashCursor = c.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL);
					}
					cursor = this.fHVSashCursor;
				} else {
					if (this.fHSashCursor == null) {
						this.fHSashCursor = c.getDisplay().getSystemCursor(SWT.CURSOR_SIZEWE);
					}
					cursor = this.fHSashCursor;
				}
				break;
			}
			if (cursor != null) {
				c.setCursor(cursor);
			}
		}
	}

	private void setAncestorVisibility(final boolean visible, final boolean enabled) {
		if (this.fAncestorItem != null) {
			Action action = (Action) this.fAncestorItem.getAction();
			if (action != null) {
				action.setChecked(visible);
				action.setEnabled(enabled);
			}
		}
		this.getCompareConfiguration().setProperty(ICompareUIConstants.PROP_ANCESTOR_VISIBLE, Boolean.valueOf(visible));
	}

	//---- input

	/**
	 * Return whether the input is a three-way comparison.
	 * @return whether the input is a three-way comparison
	 * @since 3.3
	 */
	protected boolean isThreeWay() {
		return this.fIsThreeWay;
	}

	/**
	 * Internal hook method called when the input to this viewer is
	 * initially set or subsequently changed.
	 * <p>
	 * The <code>ContentMergeViewer</code> implementation of this <code>Viewer</code>
	 * method tries to save the old input by calling <code>doSave(...)</code> and
	 * then calls <code>internalRefresh(...)</code>.
	 *
	 * @param input the new input of this viewer, or <code>null</code> if there is no new input
	 * @param oldInput the old input element, or <code>null</code> if there was previously no input
	 */
	@Override
	protected final void inputChanged(final Object input, final Object oldInput) {
		if (input != oldInput && oldInput != null) {
			ICompareInputLabelProvider lp = this.getCompareConfiguration().getLabelProvider();
			if (lp != null) {
				lp.removeListener(this.labelChangeListener);
			}
		}

		if (input != oldInput && oldInput instanceof ICompareInput) {
			ICompareContainer container = this.getCompareConfiguration().getContainer();
			container.removeCompareInputChangeListener((ICompareInput) oldInput, this.fCompareInputChangeListener);
		}

		boolean success = this.doSave(input, oldInput);

		if (input != oldInput && input instanceof ICompareInput) {
			ICompareContainer container = this.getCompareConfiguration().getContainer();
			container.addCompareInputChangeListener((ICompareInput) input, this.fCompareInputChangeListener);
		}

		if (input != oldInput && input != null) {
			ICompareInputLabelProvider lp = this.getCompareConfiguration().getLabelProvider();
			if (lp != null) {
				lp.addListener(this.labelChangeListener);
			}
		}

		if (success) {
			this.setLeftDirty(false);
			this.setRightDirty(false);
		}

		if (input != oldInput) {
			this.internalRefresh(input);
		}
	}

	/**
	 * This method is called from the <code>Viewer</code> method <code>inputChanged</code>
	 * to save any unsaved changes of the old input.
	 * <p>
	 * The <code>ContentMergeViewer</code> implementation of this
	 * method calls <code>saveContent(...)</code>. If confirmation has been turned on
	 * with <code>setConfirmSave(true)</code>, a confirmation alert is posted before saving.
	 * </p>
	 * Clients can override this method and are free to decide whether
	 * they want to call the inherited method.
	 * @param newInput the new input of this viewer, or <code>null</code> if there is no new input
	 * @param oldInput the old input element, or <code>null</code> if there was previously no input
	 * @return <code>true</code> if saving was successful, or if the user didn't want to save (by pressing 'NO' in the confirmation dialog).
	 * @since 2.0
	 */
	protected boolean doSave(final Object newInput, final Object oldInput) {
		// before setting the new input we have to save the old
		if (this.isLeftDirty() || this.isRightDirty()) {
			if (Utilities.RUNNING_TESTS) {
				if (Utilities.TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE) {
					this.flushContent(oldInput, null);
				}
			} else if (this.fConfirmSave) {
				// post alert
				Shell shell = this.fComposite.getShell();

				MessageDialog dialog = new MessageDialog(shell,
						Utilities.getString(this.getResourceBundle(), "saveDialog.title"), //$NON-NLS-1$
						null, // accept the default window icon
						Utilities.getString(this.getResourceBundle(), "saveDialog.message"), //$NON-NLS-1$
						MessageDialog.QUESTION,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, },
						0); // default button index

				switch (dialog.open()) { // open returns index of pressed button
				case 0:
					this.flushContent(oldInput, null);
					break;
				case 1:
					this.setLeftDirty(false);
					this.setRightDirty(false);
					break;
				case 2:
					throw new ViewerSwitchingCancelled();
				}
			} else {
				this.flushContent(oldInput, null);
			}
			return true;
		}
		return false;
	}

	/**
	 * Controls whether <code>doSave(Object, Object)</code> asks for confirmation before saving
	 * the old input with <code>saveContent(Object)</code>.
	 * @param enable a value of <code>true</code> enables confirmation
	 * @since 2.0
	 */
	public void setConfirmSave(final boolean enable) {
		this.fConfirmSave = enable;
	}

	@Override
	public void refresh() {
		this.internalRefresh(this.getInput());
	}

	private void internalRefresh(final Object input) {
		IMergeViewerContentProvider content = this.getMergeContentProvider();
		if (content != null) {
			Object ancestor = content.getAncestorContent(input);
			boolean oldFlag = this.fIsThreeWay;
			if (Utilities.isHunk(input)) {
				this.fIsThreeWay = true;
			} else if (input instanceof ICompareInput) {
				this.fIsThreeWay = (((ICompareInput) input).getKind() & Differencer.DIRECTION_MASK) != 0;
			} else {
				this.fIsThreeWay = ancestor != null;
			}

			if (this.fAncestorItem != null) {
				this.fAncestorItem.setVisible(this.fIsThreeWay);
			}

			if (this.fAncestorVisible && oldFlag != this.fIsThreeWay) {
				this.fComposite.layout(true);
			}

			Object left = content.getLeftContent(input);
			Object right = content.getRightContent(input);
			this.updateContent(ancestor, left, right);

			this.updateHeader();
			if (Utilities.okToUse(this.fComposite) && Utilities.okToUse(this.fComposite.getParent())) {
				ToolBarManager tbm = (ToolBarManager) this.getToolBarManager(this.fComposite.getParent());
				if (tbm != null) {
					this.updateToolItems();
					tbm.update(true);
					tbm.getControl().getParent().layout(true);
				}
			}
		}
	}

	@Override
	protected void hookControl(final Control control) {
		if (Policy.debugContentMergeViewer) {
			this.logTrace("Attached dispose listener to control " + System.identityHashCode(control)); //$NON-NLS-1$
		}
		super.hookControl(control);
	}

	//---- layout & SWT control creation

	/**
	 * Builds the SWT controls for the three areas of a compare/merge viewer.
	 * <p>
	 * Calls the hooks <code>createControls</code> and <code>createToolItems</code>
	 * to let subclasses build the specific content areas and to add items to
	 * an enclosing toolbar.
	 * <p>
	 * This method must only be called in the constructor of subclasses.
	 *
	 * @param parent the parent control
	 * @return the new control
	 */
	protected final Control buildControl(final Composite parent) {
		this.fComposite = new Composite(parent, this.fStyles | SWT.LEFT_TO_RIGHT) { // We force a specific direction
			@Override
			public boolean setFocus() {
				return ContentMergeViewer.this.handleSetFocus();
			}
		};
		this.fComposite.setData(CompareUI.COMPARE_VIEWER_TITLE, this.getTitle());

		this.hookControl(this.fComposite); // Hook help & dispose listener.

		this.fComposite.setLayout(new ContentMergeViewerLayout());
		if (Policy.debugContentMergeViewer) {
			this.logTrace("Created composite " + System.identityHashCode(this.fComposite) + " with layout " //$NON-NLS-1$//$NON-NLS-2$
					+ System.identityHashCode(this.fComposite.getLayout()));
			this.logStackTrace();
		}

		int style = SWT.SHADOW_OUT;
		this.fAncestorLabel = new CLabel(this.fComposite, style | Window.getDefaultOrientation());

		this.fLeftLabel = new CLabel(this.fComposite, style | Window.getDefaultOrientation());
		if (Policy.debugContentMergeViewer) {
			this.logTrace("fLeftLabel initialized"); //$NON-NLS-1$
			this.logStackTrace();
		}

		this.fLeftLabelSet = true;
		new Resizer(this.fLeftLabel, VERTICAL);

		this.fDirectionLabel = new CLabel(this.fComposite, style);
		this.fDirectionLabel.setAlignment(SWT.CENTER);
		new Resizer(this.fDirectionLabel, HORIZONTAL | VERTICAL);

		this.fRightLabel = new CLabel(this.fComposite, style | Window.getDefaultOrientation());
		new Resizer(this.fRightLabel, VERTICAL);

		if (this.fCenter == null || this.fCenter.isDisposed()) {
			this.fCenter = this.createCenterControl(this.fComposite);
		}

		this.createControls(this.fComposite);

		this.fHandlerService = CompareHandlerService.createFor(this.getCompareConfiguration().getContainer(),
				this.fComposite.getShell());

		this.initializeToolbars(parent);

		return this.fComposite;
	}

	/**
	 * Returns the toolbar manager for this viewer.
	 *
	 * Subclasses may extend this method and use either the toolbar manager
	 * provided by the inherited method by calling
	 * super.getToolBarManager(parent) or provide an alternate toolbar manager.
	 *
	 * @param parent
	 *            a <code>Composite</code> or <code>null</code>
	 * @return a <code>IToolBarManager</code>
	 * @since 3.4
	 */
	protected IToolBarManager getToolBarManager(final Composite parent) {
		return CompareViewerPane.getToolBarManager(parent);
	}

	private void initializeToolbars(final Composite parent) {
		ToolBarManager tbm = (ToolBarManager) this.getToolBarManager(parent);
		if (tbm != null) {
			tbm.removeAll();

			// Define groups.
			tbm.add(new Separator("modes")); //$NON-NLS-1$
			tbm.add(new Separator("merge")); //$NON-NLS-1$
			tbm.add(new Separator("navigation")); //$NON-NLS-1$

			this.copyLeftToRightItem = this.createCopyAction(true);
			Utilities.initAction(this.copyLeftToRightItem.getAction(), this.getResourceBundle(),
					"action.CopyLeftToRight."); //$NON-NLS-1$
			tbm.appendToGroup("merge", this.copyLeftToRightItem); //$NON-NLS-1$
			this.fHandlerService.registerAction(this.copyLeftToRightItem.getAction(),
					"org.eclipse.compare.copyAllLeftToRight"); //$NON-NLS-1$

			this.copyRightToLeftItem = this.createCopyAction(false);
			Utilities.initAction(this.copyRightToLeftItem.getAction(), this.getResourceBundle(),
					"action.CopyRightToLeft."); //$NON-NLS-1$
			tbm.appendToGroup("merge", this.copyRightToLeftItem); //$NON-NLS-1$
			this.fHandlerService.registerAction(this.copyRightToLeftItem.getAction(),
					"org.eclipse.compare.copyAllRightToLeft"); //$NON-NLS-1$

			this.fSwitchLeftAndRight = new Action() {
				@Override
				public void run() {
					IPreferenceStore preferences = ContentMergeViewer.this.getCompareConfiguration()
							.getPreferenceStore();
					preferences.setValue(ComparePreferencePage.SWAPPED,
							!ContentMergeViewer.this.getCompareConfiguration().isMirrored());
					if (preferences instanceof IPersistentPreferenceStore) {
						try {
							((IPersistentPreferenceStore) preferences).save();
						} catch (IOException e) {
							CompareUIPlugin.log(e);
						}
					}
				}
			};
			Utilities.initAction(this.fSwitchLeftAndRight, this.getResourceBundle(), "action.SwitchLeftAndRight."); //$NON-NLS-1$
			tbm.appendToGroup("modes", this.fSwitchLeftAndRight); //$NON-NLS-1$

			final ChangePropertyAction a = new ChangePropertyAction(this.fBundle, this.getCompareConfiguration(),
					"action.EnableAncestor.", ICompareUIConstants.PROP_ANCESTOR_VISIBLE); //$NON-NLS-1$
			a.setChecked(this.fAncestorVisible);
			this.fAncestorItem = new ActionContributionItem(a);
			this.fAncestorItem.setVisible(false);
			tbm.appendToGroup("modes", this.fAncestorItem); //$NON-NLS-1$
			tbm.getControl().addDisposeListener(a);

			this.createToolItems(tbm);
			this.updateToolItems();

			tbm.update(true);
		}
	}

	private ActionContributionItem createCopyAction(final boolean leftToRight) {
		return new ActionContributionItem(new Action() {
			@Override
			public void run() {
				ContentMergeViewer.this.copy(leftToRight);
			}
		});
	}

	/**
	 * Callback that is invoked when the control of this merge viewer is given focus.
	 * This method should return <code>true</code> if a particular widget was given focus
	 * and false otherwise. By default, <code>false</code> is returned. Subclasses may override.
	 * @return whether  particular widget was given focus
	 * @since 3.3
	 */
	protected boolean handleSetFocus() {
		return false;
	}

	/**
	 * Return the desired width of the center control. This width is used
	 * to calculate the values used to layout the ancestor, left and right sides.
	 * @return the desired width of the center control
	 * @see #handleResizeLeftRight(int, int, int, int, int, int)
	 * @see #handleResizeAncestor(int, int, int, int)
	 * @since 3.3
	 */
	protected int getCenterWidth() {
		return 3;
	}

	/**
	 * Return whether the ancestor pane is visible or not.
	 * @return whether the ancestor pane is visible or not
	 * @since 3.3
	 */
	protected boolean isAncestorVisible() {
		return this.fAncestorVisible;
	}

	/**
	 * Create the control that divides the left and right sides of the merge viewer.
	 * @param parent the parent composite
	 * @return the center control
	 * @since 3.3
	 */
	protected Control createCenterControl(final Composite parent) {
		Sash sash = new Sash(parent, SWT.VERTICAL);
		new Resizer(sash, HORIZONTAL);
		return sash;
	}

	/**
	 * Return the center control that divides the left and right sides of the merge viewer.
	 * This method returns the control that was created by calling {@link #createCenterControl(Composite)}.
	 * @see #createCenterControl(Composite)
	 * @return the center control
	 * @since 3.3
	 */
	protected Control getCenterControl() {
		return this.fCenter;
	}

	@Override
	public Control getControl() {
		return this.fComposite;
	}

	/**
	 * Called on the viewer disposal.
	 * Unregisters from the compare configuration.
	 * Clients may extend if they have to do additional cleanup.
	 * @see org.eclipse.jface.viewers.ContentViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(final DisposeEvent event) {
		if (this.fHandlerService != null) {
			this.fHandlerService.dispose();
		}

		Object input = this.getInput();
		if (input instanceof ICompareInput) {
			ICompareContainer container = this.getCompareConfiguration().getContainer();
			container.removeCompareInputChangeListener((ICompareInput) input, this.fCompareInputChangeListener);
		}
		if (input != null) {
			ICompareInputLabelProvider lp = this.getCompareConfiguration().getLabelProvider();
			if (lp != null) {
				lp.removeListener(this.labelChangeListener);
			}
		}

		if (this.fPropertyChangeListener != null) {
			this.fCompareConfiguration.removePropertyChangeListener(this.fPropertyChangeListener);
			this.fPropertyChangeListener = null;
		}

		if (this.fPreferenceChangeListener != null) {
			this.fCompareConfiguration.getPreferenceStore()
					.removePropertyChangeListener(this.fPreferenceChangeListener);
			this.fPreferenceChangeListener = null;
		}

		this.fAncestorLabel = null;
		this.fLeftLabel = null;
		if (Policy.debugContentMergeViewer) {
			this.logTrace(
					"handleDispose(...) - fLeftLabel = null. event.widget = " + System.identityHashCode(event.widget)); //$NON-NLS-1$
			this.logStackTrace();
		}
		this.fDirectionLabel = null;
		this.fRightLabel = null;
		this.fCenter = null;

		if (this.fRightArrow != null) {
			this.fRightArrow.dispose();
			this.fRightArrow = null;
		}
		if (this.fLeftArrow != null) {
			this.fLeftArrow.dispose();
			this.fLeftArrow = null;
		}
		if (this.fBothArrow != null) {
			this.fBothArrow.dispose();
			this.fBothArrow = null;
		}

		super.handleDispose(event);
	}

	/**
	 * Updates the enabled state of the toolbar items.
	 * <p>
	 * This method is called whenever the state of the items needs updating.
	 * <p>
	 * Subclasses may extend this method, although this is generally not required.
	 */
	protected void updateToolItems() {
		IMergeViewerContentProvider content = this.getMergeContentProvider();

		Object input = this.getInput();

		if (this.copyLeftToRightItem != null) {
			boolean rightEditable = content.isRightEditable(input);
			this.copyLeftToRightItem.setVisible(rightEditable);
			this.copyLeftToRightItem.getAction().setEnabled(rightEditable);
		}

		if (this.copyRightToLeftItem != null) {
			boolean leftEditable = content.isLeftEditable(input);
			this.copyRightToLeftItem.setVisible(leftEditable);
			this.copyRightToLeftItem.getAction().setEnabled(leftEditable);
		}

		if (this.fSwitchLeftAndRight != null) {
			this.fSwitchLeftAndRight.setChecked(this.getCompareConfiguration().isMirrored());
		}
	}

	/**
	 * Updates the headers of the three areas
	 * by querying the content provider for a name and image for
	 * the three sides of the input object.
	 * <p>
	 * This method is called whenever the header must be updated.
	 * <p>
	 * Subclasses may extend this method, although this is generally not required.
	 */
	protected void updateHeader() {
		IMergeViewerContentProvider content = this.getMergeContentProvider();
		Object input = this.getInput();

		// Only change a label if there is a new label available
		if (this.fAncestorLabel != null) {
			Image ancestorImage = content.getAncestorImage(input);
			if (ancestorImage != null) {
				this.fAncestorLabel.setImage(ancestorImage);
			}
			String ancestorLabel = content.getAncestorLabel(input);
			if (ancestorLabel != null) {
				this.fAncestorLabel.setText(LegacyActionTools.escapeMnemonics(TextProcessor.process(ancestorLabel)));
			}
		}
		if (this.fLeftLabel != null) {
			Image leftImage = content.getLeftImage(input);
			if (leftImage != null) {
				this.fLeftLabel.setImage(leftImage);
			}
			String leftLabel = content.getLeftLabel(input);
			if (leftLabel != null) {
				this.fLeftLabel.setText(LegacyActionTools.escapeMnemonics(leftLabel));
			}
		}
		if (this.fRightLabel != null) {
			Image rightImage = content.getRightImage(input);
			if (rightImage != null) {
				this.fRightLabel.setImage(rightImage);
			}
			String rightLabel = content.getRightLabel(input);
			if (rightLabel != null) {
				this.fRightLabel.setText(LegacyActionTools.escapeMnemonics(rightLabel));
			}
		}
	}

	/**
	 * Calculates the height of the header.
	 */
	/* package */ int getHeaderHeight() {
		int headerHeight = this.fLeftLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y;
		headerHeight = Math.max(headerHeight, this.fDirectionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y);
		return headerHeight;
	}

	//---- dirty state & saving state

	@Override
	public void addPropertyChangeListener(final IPropertyChangeListener listener) {
		if (this.fListenerList == null) {
			this.fListenerList = new ListenerList<>();
		}
		this.fListenerList.add(listener);
	}

	@Override
	public void removePropertyChangeListener(final IPropertyChangeListener listener) {
		if (this.fListenerList != null) {
			this.fListenerList.remove(listener);
			if (this.fListenerList.isEmpty()) {
				this.fListenerList = null;
			}
		}
	}

	private void fireDirtyState(final boolean state) {
		Utilities.firePropertyChange(this.fListenerList, this, CompareEditorInput.DIRTY_STATE, null,
				Boolean.valueOf(state));
	}

	/**
	 * Sets the dirty state of the left side of this viewer.
	 * If the new value differs from the old
	 * all registered listener are notified with
	 * a <code>PropertyChangeEvent</code> with the
	 * property name <code>CompareEditorInput.DIRTY_STATE</code>.
	 *
	 * @param dirty the state of the left side dirty flag
	 */
	protected void setLeftDirty(final boolean dirty) {
		if (this.isLeftDirty() != dirty) {
			this.fIsLeftDirty = dirty;
			// Always fire the event if the dirty state has changed
			this.fireDirtyState(dirty);
		}
	}

	/**
	 * Sets the dirty state of the right side of this viewer.
	 * If the new value differs from the old
	 * all registered listener are notified with
	 * a <code>PropertyChangeEvent</code> with the
	 * property name <code>CompareEditorInput.DIRTY_STATE</code>.
	 *
	 * @param dirty the state of the right side dirty flag
	 */
	protected void setRightDirty(final boolean dirty) {
		if (this.isRightDirty() != dirty) {
			this.fIsRightDirty = dirty;
			// Always fire the event if the dirty state has changed
			this.fireDirtyState(dirty);
		}
	}

	/**
	 * Method from the old internal <code>ISavable</code> interface
	 * Save the viewers's content.
	 * Note: this method is for internal use only. Clients should not call this method.
	 *
	 * @param monitor a progress monitor
	 * @throws CoreException not thrown anymore
	 * @deprecated use {@link IFlushable#flush(IProgressMonitor)}.
	 */
	@Deprecated
	public void save(final IProgressMonitor monitor) throws CoreException {
		this.flush(monitor);
	}

	/**
	 * Flush any modifications made in the viewer into the compare input. This method
	 * calls {@link #flushContent(Object, IProgressMonitor)} with the compare input
	 * of the viewer as the first parameter.
	 *
	 * @param monitor a progress monitor
	 * @see org.eclipse.compare.contentmergeviewer.IFlushable#flush(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 3.3
	 */
	@Override
	public final void flush(final IProgressMonitor monitor) {
		this.flushContent(this.getInput(), monitor);
	}

	/**
	 * Flushes the modified content back to input elements via the content provider.
	 * The provided input may be the current input of the viewer or it may be
	 * the previous input (i.e. this method may be called to flush modified content
	 * during an input change).
	 *
	 * @param input the compare input
	 * @param monitor a progress monitor or <code>null</code> if the method
	 * was call from a place where a progress monitor was not available.
	 * @since 3.3
	 */
	protected void flushContent(final Object input, final IProgressMonitor monitor) {
		this.flushLeftSide(input, monitor);
		this.flushRightSide(input, monitor);
	}

	void flushLeftSide(final Object input, final IProgressMonitor monitor) {
		IMergeViewerContentProvider content = (IMergeViewerContentProvider) this.getContentProvider();

		boolean rightEmpty = content.getRightContent(input) == null;

		if (this.getCompareConfiguration().isLeftEditable() && this.isLeftDirty()) {
			byte[] bytes = this.getContents(true);
			if (rightEmpty && bytes != null && bytes.length == 0) {
				bytes = null;
			}
			this.setLeftDirty(false);
			content.saveLeftContent(input, bytes);
		}
	}

	void flushRightSide(final Object input, final IProgressMonitor monitor) {
		IMergeViewerContentProvider content = (IMergeViewerContentProvider) this.getContentProvider();

		boolean leftEmpty = content.getLeftContent(input) == null;

		if (this.getCompareConfiguration().isRightEditable() && this.isRightDirty()) {
			byte[] bytes = this.getContents(false);
			if (leftEmpty && bytes != null && bytes.length == 0) {
				bytes = null;
			}
			this.setRightDirty(false);
			content.saveRightContent(input, bytes);
		}
	}

	/**
	 * @param monitor The progress monitor to report progress.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void flushLeft(final IProgressMonitor monitor) {
		this.flushLeftSide(this.getInput(), monitor);
	}

	/**
	 * @param monitor The progress monitor to report progress.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void flushRight(final IProgressMonitor monitor) {
		this.flushRightSide(this.getInput(), monitor);
	}

	/**
	 * Return the dirty state of the right side of this viewer.
	 * @return the dirty state of the right side of this viewer
	 * @since 3.3
	 */
	protected boolean isRightDirty() {
		return this.fIsRightDirty;
	}

	/**
	 * @return the dirty state of the right side of this viewer
	 * @since 3.7
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public boolean internalIsRightDirty() {
		return this.isRightDirty();
	}

	/**
	 * Return the dirty state of the left side of this viewer.
	 * @return the dirty state of the left side of this viewer
	 * @since 3.3
	 */
	protected boolean isLeftDirty() {
		return this.fIsLeftDirty;
	}

	/**
	 * @return the dirty state of the left side of this viewer
	 * @since 3.7
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public boolean internalIsLeftDirty() {
		return this.isLeftDirty();
	}

	/**
	 * Handle a change to the given input reported from an {@link org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener}.
	 * This class registers a listener with its input and reports any change events through
	 * this method. By default, this method prompts for any unsaved changes and then refreshes
	 * the viewer. Subclasses may override.
	 * @since 3.3
	 */
	protected void handleCompareInputChange() {
		// Before setting the new input we have to save the old.
		Object input = this.getInput();
		if (!this.isSaving() && (this.isLeftDirty() || this.isRightDirty())) {

			if (Utilities.RUNNING_TESTS) {
				if (Utilities.TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE) {
					this.flushContent(input, null);
				}
			} else {
				// post alert
				Shell shell = this.fComposite.getShell();

				MessageDialog dialog = new MessageDialog(shell,
						CompareMessages.ContentMergeViewer_resource_changed_title,
						null, // accept the default window icon
						CompareMessages.ContentMergeViewer_resource_changed_description,
						MessageDialog.QUESTION,
						new String[] {
								IDialogConstants.YES_LABEL, // 0
								IDialogConstants.NO_LABEL, // 1
						},
						0); // default button index

				switch (dialog.open()) { // open returns index of pressed button
				case 0:
					this.flushContent(input, null);
					break;
				case 1:
					this.setLeftDirty(false);
					this.setRightDirty(false);
					break;
				}
			}
		}
		if (this.isSaving() && (this.isLeftDirty() || this.isRightDirty())) {
			return; // Do not refresh until saving both sides is complete.
		}
		this.refresh();
	}

	CompareHandlerService getCompareHandlerService() {
		return this.fHandlerService;
	}

	/**
	 * @return true if any of the Saveables is being saved
	 */
	private boolean isSaving() {
		ICompareContainer container = this.fCompareConfiguration.getContainer();
		ISaveablesSource source = null;
		if (container instanceof ISaveablesSource) {
			source = (ISaveablesSource) container;
		} else {
			IWorkbenchPart part = container.getWorkbenchPart();
			if (part instanceof ISaveablesSource) {
				source = (ISaveablesSource) part;
			}
		}
		if (source != null) {
			Saveable[] saveables = source.getSaveables();
			for (Saveable s : saveables) {
				if (s instanceof ISavingSaveable) {
					ISavingSaveable saveable = (ISavingSaveable) s;
					if (saveable.isSaving()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * If the inputs are mirrored, this asks the right model value.
	 *
	 * @return true if the left viewer is editable
	 * @since 3.7
	 */
	protected boolean isLeftEditable() {
		return this.fCompareConfiguration.isMirrored() ? this.fCompareConfiguration.isRightEditable()
				: this.fCompareConfiguration.isLeftEditable();
	}

	/**
	 * If the inputs are mirrored, this asks the left model value.
	 *
	 * @return true if the right viewer is editable
	 * @since 3.7
	 */
	protected boolean isRightEditable() {
		return this.fCompareConfiguration.isMirrored() ? this.fCompareConfiguration.isLeftEditable()
				: this.fCompareConfiguration.isRightEditable();
	}
}
