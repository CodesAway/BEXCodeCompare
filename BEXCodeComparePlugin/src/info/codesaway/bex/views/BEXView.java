package info.codesaway.bex.views;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import info.codesaway.bex.Activator;
import info.codesaway.bex.BEXPair;
import info.codesaway.bex.BEXSide;
import info.codesaway.bex.IntRange;
import info.codesaway.bex.compare.BEXChangeInfo;
import info.codesaway.bex.diff.DiffChange;
import info.codesaway.bex.diff.DiffEdit;
import info.codesaway.bex.diff.DiffHelper;
import info.codesaway.eclipse.compare.contentmergeviewer.TextMergeViewer;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public final class BEXView extends ViewPart {
	// TODO: odd diff when compare BEXView to 6/12/2020 6:55 AM
	// (commented out code was shown as addition and didn't align correctly for some reason)

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "info.codesaway.bex.views.BEXView";

	private static BEXView INSTANCE;

	@Inject
	IWorkbench workbench;

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	//	private Action action1;
	//	private Action action2;
	private Action doubleClickAction;

	private ViewContentProvider viewContentProvider;

	private TextMergeViewer mergeViewer;
	private List<DiffChange<BEXChangeInfo>> changes;

	class TreeObject implements IAdaptable {
		private final String name;
		private TreeParent parent;
		private final DiffEdit diffEdit;

		public TreeObject(final String name, final DiffEdit diffEdit) {
			this.name = name;
			this.diffEdit = diffEdit;
		}

		public String getName() {
			return this.name;
		}

		public void setParent(final TreeParent parent) {
			this.parent = parent;
		}

		public TreeParent getParent() {
			return this.parent;
		}

		@Override
		public String toString() {
			return this.getName();
		}

		@Override
		public <T> T getAdapter(final Class<T> key) {
			return null;
		}

		public void clear() {
			this.parent = null;
		}
	}

	class TreeParent extends TreeObject {
		private final List<TreeObject> children;

		public TreeParent(final String name) {
			this(name, null);
		}

		public TreeParent(final String name, final DiffEdit diffEdit) {
			super(name, diffEdit);
			this.children = new ArrayList<>();
		}

		public void addChild(final TreeObject child) {
			this.children.add(child);
			child.setParent(this);
		}

		public void removeChild(final TreeObject child) {
			this.children.remove(child);
			child.setParent(null);
		}

		public TreeObject[] getChildren() {
			return this.children.toArray(new TreeObject[this.children.size()]);
		}

		public boolean hasChildren() {
			return this.children.size() > 0;
		}

		/**
		 * Recursively clear tree
		 */
		@Override
		public void clear() {
			super.clear();

			for (TreeObject child : this.children) {
				child.clear();
			}

			this.children.clear();
		}
	}

	class ViewContentProvider implements ITreeContentProvider {
		// TODO: see about lazy initializing
		private final TreeParent invisibleRoot = new TreeParent("");

		@Override
		public Object[] getElements(final Object parent) {
			if (parent.equals(BEXView.this.getViewSite())) {
				return this.getChildren(this.invisibleRoot);
			}
			return this.getChildren(parent);
		}

		@Override
		public Object getParent(final Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject) child).getParent();
			}
			return null;
		}

		@Override
		public Object[] getChildren(final Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent) parent).getChildren();
			}
			return new Object[0];
		}

		@Override
		public boolean hasChildren(final Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent) parent).hasChildren();
			}
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(final Object obj) {
			return obj.toString();
		}

		@Override
		public Image getImage(final Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof TreeParent) {
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			}
			return BEXView.this.workbench.getSharedImages().getImage(imageKey);
		}
	}

	@Override
	public void createPartControl(final Composite parent) {
		this.viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.drillDownAdapter = new DrillDownAdapter(this.viewer);

		this.viewContentProvider = new ViewContentProvider();
		// TODO: look into virtual or lazy loaded TreeViewer
		this.viewer.setContentProvider(this.viewContentProvider);
		this.viewer.setInput(this.getViewSite());
		// TODO: look into styling text
		// This way, when showing both lines of substitution, can bold the differences
		// https://stackoverflow.com/questions/26211705/java-swt-treeviewer-with-one-column-that-needs-to-be-styledtext
		// https://stackoverflow.com/questions/26173834/highlight-particular-string-in-swt-tree-node
		this.viewer.setLabelProvider(new ViewLabelProvider());
		// https://www.vogella.com/tutorials/EclipseJFaceTree/article.html
		this.viewer.getTree().setFont(JFaceResources.getTextFont());

		// Create the help context id for the viewer's control
		this.workbench.getHelpSystem().setHelp(this.viewer.getControl(), "info.codesaway.viewer");
		this.getSite().setSelectionProvider(this.viewer);
		this.makeActions();
		//		this.hookContextMenu();
		this.hookDoubleClickAction();
		this.contributeToActionBars();

		INSTANCE = this;
	}

	//	private void hookContextMenu() {
	//		MenuManager menuMgr = new MenuManager("#PopupMenu");
	//		menuMgr.setRemoveAllWhenShown(true);
	//		menuMgr.addMenuListener(BEXView.this::fillContextMenu);
	//		Menu menu = menuMgr.createContextMenu(this.viewer.getControl());
	//		this.viewer.getControl().setMenu(menu);
	//		this.getSite().registerContextMenu(menuMgr, this.viewer);
	//	}

	private void contributeToActionBars() {
		IActionBars bars = this.getViewSite().getActionBars();
		this.fillLocalPullDown(bars.getMenuManager());
		this.fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(final IMenuManager manager) {
		//		manager.add(this.action1);
		//		manager.add(new Separator());
		//		manager.add(this.action2);
	}

	//	private void fillContextMenu(final IMenuManager manager) {
	//		//		manager.add(this.action1);
	//		//		manager.add(this.action2);
	//		manager.add(new Separator());
	//		this.drillDownAdapter.addNavigationActions(manager);
	//		// Other plug-ins can contribute there actions here
	//		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	//	}

	private void fillLocalToolBar(final IToolBarManager manager) {
		//		manager.add(this.action1);
		//		manager.add(this.action2);
		//		manager.add(new Separator());
		this.drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		//		this.action1 = new Action() {
		//			@Override
		//			public void run() {
		//				BEXView.this.showMessage("Action 1 executed");
		//			}
		//		};
		//		this.action1.setText("Action 1");
		//		this.action1.setToolTipText("Action 1 tooltip");
		//		this.action1.setImageDescriptor(
		//				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		//
		//		this.action2 = new Action() {
		//			@Override
		//			public void run() {
		//				BEXView.this.showMessage("Action 2 executed");
		//			}
		//		};
		//		this.action2.setText("Action 2");
		//		this.action2.setToolTipText("Action 2 tooltip");
		//		this.action2.setImageDescriptor(
		//				this.workbench.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		this.doubleClickAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = BEXView.this.viewer.getStructuredSelection();
				Object obj = selection.getFirstElement();
				//				BEXView.this.showMessage("Double-click detected on " + obj.toString());

				if (BEXView.this.mergeViewer != null && obj instanceof TreeObject) {
					TreeObject object = (TreeObject) obj;

					if (object.diffEdit != null) {
						try {
							BEXView.this.mergeViewer.scroll(object.diffEdit.getLeftLineNumber(),
									object.diffEdit.getRightLineNumber());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(event -> BEXView.this.doubleClickAction.run());
	}

	//	private void showMessage(final String message) {
	//		MessageDialog.openInformation(
	//				this.viewer.getControl().getShell(),
	//				"BEX View",
	//				message);
	//	}

	@Override
	public void setFocus() {
		this.viewer.getControl().setFocus();
	}

	private void setChanges(final List<DiffChange<BEXChangeInfo>> changes) {
		// TODO: takes a few seconds to run for big changes
		// Move most of this to background thread, except for UI parts
		this.changes = changes;

		TreeParent invisibleRoot = this.viewContentProvider.invisibleRoot;
		invisibleRoot.clear();

		List<TreeParent> expandedElements = new ArrayList<>();

		boolean shouldShowBothSidesOfSubstitution = Activator.shouldShowBothSidesOfSubstitution();
		System.out.println("In BEXView.setChanges");

		for (DiffChange<BEXChangeInfo> change : changes) {
			TreeParent changeParent = new TreeParent(change.toString());
			invisibleRoot.addChild(changeParent);

			boolean isImportantChange = change.getInfo().isImportantChange();

			if (isImportantChange) {
				expandedElements.add(changeParent);
			}

			// Collapse consecutive ignored differences
			List<DiffEdit> diffEdits = change.getEdits();

			for (int i = 0; i < diffEdits.size(); i++) {
				DiffEdit changeEdit = diffEdits.get(i);
				char symbol = changeEdit.shouldIgnore() ? ' ' : changeEdit.getSymbol();

				if (isImportantChange && changeEdit.shouldIgnore() && i < diffEdits.size() - 1) {
					// TODO: if there are multiple continuous DiffEdit which should be ignored
					// Group them together, collapsed
					// This way, can expand if want to, but otherwise will collapse

					DiffEdit nextChangeEdit = diffEdits.get(i + 1);

					if (nextChangeEdit.shouldIgnore()
							&& DiffHelper.hasConsecutiveLines(changeEdit, nextChangeEdit, true)) {
						//						System.out.println("Create ignore block starting with " + changeEdit);
						List<DiffEdit> ignoreEdits = new ArrayList<>();
						ignoreEdits.add(changeEdit);

						do {
							changeEdit = diffEdits.get(++i);
							ignoreEdits.add(changeEdit);

							if (i == diffEdits.size() - 1) {
								break;
							}

							nextChangeEdit = diffEdits.get(i + 1);
						} while (nextChangeEdit.shouldIgnore()
								&& DiffHelper.hasConsecutiveLines(changeEdit, nextChangeEdit, true));

						this.addIgnoreParent(changeParent, ignoreEdits);

						continue;
					}
				}

				if (changeEdit.isSubstitution() && shouldShowBothSidesOfSubstitution) {
					// TODO: maybe give option to have like this, but this is information overload
					// Put the left / right substitution each on their own line
					TreeParent substitutionParent = new TreeParent(changeEdit.toString(), changeEdit);

					// TODO: add extra spacing (is there a better way to do this?)
					TreeObject leftChangeLeaf = new TreeObject("  " + changeEdit.toString(BEXSide.LEFT), changeEdit);
					TreeObject rightChangeLeaf = new TreeObject("  " + changeEdit.toString(BEXSide.RIGHT), changeEdit);
					substitutionParent.addChild(leftChangeLeaf);
					substitutionParent.addChild(rightChangeLeaf);
					//					expandedElements.add(substitutionParent);

					changeParent.addChild(substitutionParent);
				} else {
					TreeObject changeLeaf = new TreeObject(changeEdit.toString(symbol), changeEdit);
					changeParent.addChild(changeLeaf);
				}
			}

			// Have two levels of parents (second level shows the block)
			//			for (DiffUnit changeUnit : change.getChanges()) {
			//				TreeParent changeUnitParent = new TreeParent(changeUnit.getType().toString());
			//				changeParent.addChild(changeUnitParent);
			//
			//				for (DiffEdit changeEdit : changeUnit.getEdits()) {
			//					TreeObject changeLeaf = new TreeObject(changeEdit.toString());
			//					changeUnitParent.addChild(changeLeaf);
			//				}
			//			}
		}

		this.viewer.refresh();
		this.viewer.setExpandedElements(expandedElements.toArray());
	}

	private void addIgnoreParent(final TreeParent changeParent, final List<DiffEdit> ignoreEdits) {
		// TODO: Get the first non-empty line?
		DiffEdit initialEdit = ignoreEdits.get(0);

		BEXPair<IntRange> enclosedRange = DiffHelper.determineEnclosedRange(ignoreEdits);
		IntRange leftRange = enclosedRange.getLeft();
		IntRange rightRange = enclosedRange.getRight();

		String name = String.format("Ignore%s%n",
				leftRange.getLeft() != -1 ? " LEFT " + leftRange : "",
				rightRange.getLeft() != -1 ? " RIGHT " + rightRange : "");

		TreeParent ignoreParent = new TreeParent(name, initialEdit);
		changeParent.addChild(ignoreParent);

		for (DiffEdit diffEdit : ignoreEdits) {
			ignoreParent.addChild(new TreeObject(diffEdit.toString(' '), diffEdit));
		}
	}

	public void refreshChanges() {
		if (this.changes != null) {
			show(this.changes);
		}
	}

	public static void show(final List<DiffChange<BEXChangeInfo>> changes) {
		if (Display.getCurrent() == null) {
			// If not called from UI thread, run async on UI thread
			Display.getDefault().asyncExec(() -> privateShow(changes));
		} else {
			privateShow(changes);
		}
	}

	private static void privateShow(final List<DiffChange<BEXChangeInfo>> changes) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {
			page.showView(ID, null, IWorkbenchPage.VIEW_ACTIVATE);

			if (INSTANCE != null) {
				INSTANCE.setChanges(changes);
			}
		} catch (PartInitException e) {
		}
	}

	public void setMergeViewer(final TextMergeViewer textMergeViewer) {
		this.mergeViewer = textMergeViewer;

		if (textMergeViewer == null) {
			this.viewContentProvider.invisibleRoot.clear();
			this.viewer.refresh();
			this.changes = null;
		}
	}

	public void refreshMergeViewer() {
		if (this.mergeViewer != null) {
			// TODO: does this work as expected?
			Display.getDefault().asyncExec(this.mergeViewer::refresh);
		}
	}

	public static BEXView getInstance() {
		return INSTANCE;
	}
}
