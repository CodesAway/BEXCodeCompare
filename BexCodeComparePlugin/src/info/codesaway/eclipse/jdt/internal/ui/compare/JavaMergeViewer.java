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
 *******************************************************************************/
package info.codesaway.eclipse.jdt.internal.ui.compare;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.PartEventAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import info.codesaway.eclipse.compare.contentmergeviewer.TextMergeViewer;

public class JavaMergeViewer extends TextMergeViewer {

	private IPropertyChangeListener fPreferenceChangeListener;
	private IPreferenceStore fPreferenceStore;
	private Map<SourceViewer, JavaSourceViewerConfiguration> fSourceViewerConfiguration;
	private Map<SourceViewer, CompilationUnitEditorAdapter> fEditor;
	private ArrayList<SourceViewer> fSourceViewer;

	private IWorkbenchPartSite fSite;

	public JavaMergeViewer(final Composite parent, final int styles, final CompareConfiguration mp) {
		super(parent, styles | SWT.LEFT_TO_RIGHT, mp);
		// System.out.println(getMethodName());
	}

	private IPreferenceStore getPreferenceStore() {
		// System.out.println(getMethodName());
		if (this.fPreferenceStore == null) {
			this.setPreferenceStore(this.createChainedPreferenceStore(null));
		}
		return this.fPreferenceStore;
	}

	@Override
	protected void handleDispose(final DisposeEvent event) {
		// System.out.println(getMethodName());
		this.setPreferenceStore(null);
		super.handleDispose(event);
	}

	public IJavaProject getJavaProject(final ICompareInput input) {
		// System.out.println(getMethodName());

		if (input == null) {
			return null;
		}

		IResourceProvider rp = null;
		ITypedElement te = input.getLeft();
		if (te instanceof IResourceProvider) {
			rp = (IResourceProvider) te;
		}
		if (rp == null) {
			te = input.getRight();
			if (te instanceof IResourceProvider) {
				rp = (IResourceProvider) te;
			}
		}
		if (rp == null) {
			te = input.getAncestor();
			if (te instanceof IResourceProvider) {
				rp = (IResourceProvider) te;
			}
		}
		if (rp != null) {
			IResource resource = rp.getResource();
			if (resource != null) {
				IJavaElement element = JavaCore.create(resource);
				if (element != null) {
					return element.getJavaProject();
				}
			}
		}
		return null;
	}

	@Override
	public void setInput(final Object input) {
		// System.out.println(getMethodName());
		if (input instanceof ICompareInput) {
			IJavaProject project = this.getJavaProject((ICompareInput) input);
			if (project != null) {
				this.setPreferenceStore(this.createChainedPreferenceStore(project));
			}
		}
		super.setInput(input);
	}

	private ChainedPreferenceStore createChainedPreferenceStore(final IJavaProject project) {
		// System.out.println(getMethodName());
		ArrayList<IPreferenceStore> stores = new ArrayList<>(4);
		if (project != null) {
			stores.add(new EclipsePreferencesAdapter(new ProjectScope(project.getProject()), JavaCore.PLUGIN_ID));
		}
		stores.add(JavaPlugin.getDefault().getPreferenceStore());
		stores.add(new PreferencesAdapter(JavaPlugin.getJavaCorePluginPreferences()));
		stores.add(EditorsUI.getPreferenceStore());
		return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}

	private void handlePropertyChange(final PropertyChangeEvent event) {
		// System.out.println(getMethodName());
		if (this.fSourceViewerConfiguration != null) {
			for (Entry<SourceViewer, JavaSourceViewerConfiguration> entry : this.fSourceViewerConfiguration
					.entrySet()) {
				JavaSourceViewerConfiguration configuration = entry.getValue();
				if (configuration.affectsTextPresentation(event)) {
					configuration.handlePropertyChangeEvent(event);
					ITextViewer viewer = entry.getKey();
					viewer.invalidateTextPresentation();
				}
			}
		}
	}

	@Override
	public String getTitle() {
		// System.out.println(getMethodName());

		return "BEX Code Compare";
		//		return CompareMessages.JavaMergeViewer_title;
	}

	@Override
	public ITokenComparator createTokenComparator(final String s) {
		// System.out.println(getMethodName());
		return new JavaTokenComparator(s);
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner() {
		// System.out.println(getMethodName());
		return JavaCompareUtilities.createJavaPartitioner();
	}

	@Override
	protected String getDocumentPartitioning() {
		// System.out.println(getMethodName());
		return IJavaPartitions.JAVA_PARTITIONING;
	}

	@Override
	protected void configureTextViewer(final TextViewer viewer) {
		// System.out.println(getMethodName());
		if (viewer instanceof SourceViewer) {
			SourceViewer sourceViewer = (SourceViewer) viewer;
			if (this.fSourceViewer == null) {
				this.fSourceViewer = new ArrayList<>();
			}
			if (!this.fSourceViewer.contains(sourceViewer)) {
				this.fSourceViewer.add(sourceViewer);
			}
			JavaTextTools tools = JavaCompareUtilities.getJavaTextTools();
			if (tools != null) {
				IEditorInput editorInput = this.getEditorInput(sourceViewer);
				sourceViewer.unconfigure();
				if (editorInput == null) {
					sourceViewer.configure(this.getSourceViewerConfiguration(sourceViewer, null));
					return;
				}
				this.getSourceViewerConfiguration(sourceViewer, editorInput);
			}
		}
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#setEditable(org.eclipse.jface.text.source.ISourceViewer, boolean)
	 * @since 3.5
	 */
	@Override
	protected void setEditable(final ISourceViewer sourceViewer, final boolean state) {
		// System.out.println(getMethodName());
		super.setEditable(sourceViewer, state);
		if (this.fEditor != null) {
			Object editor = this.fEditor.get(sourceViewer);
			if (editor instanceof CompilationUnitEditorAdapter) {
				((CompilationUnitEditorAdapter) editor).setEditable(state);
			}
		}
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#isEditorBacked(org.eclipse.jface.text.ITextViewer)
	 * @since 3.5
	 */
	@Override
	protected boolean isEditorBacked(final ITextViewer textViewer) {
		// System.out.println(getMethodName());
		return this.getSite() != null;
	}

	@Override
	protected IEditorInput getEditorInput(final ISourceViewer sourceViewer) {
		// System.out.println(getMethodName());
		IEditorInput editorInput = super.getEditorInput(sourceViewer);
		if (editorInput == null) {
			return null;
		}
		if (this.getSite() == null) {
			return null;
		}
		if (!(editorInput instanceof IStorageEditorInput)) {
			return null;
		}
		return editorInput;
	}

	private IWorkbenchPartSite getSite() {
		//		System.out.println(getMethodName());
		if (this.fSite == null) {
			IWorkbenchPart workbenchPart = this.getCompareConfiguration().getContainer().getWorkbenchPart();
			this.fSite = workbenchPart != null ? workbenchPart.getSite() : null;
		}
		return this.fSite;
	}

	private JavaSourceViewerConfiguration getSourceViewerConfiguration(final SourceViewer sourceViewer,
			final IEditorInput editorInput) {
		// System.out.println(getMethodName());
		if (this.fSourceViewerConfiguration == null) {
			this.fSourceViewerConfiguration = new HashMap<>(3);
		}
		if (this.fPreferenceStore == null) {
			this.getPreferenceStore();
		}
		JavaTextTools tools = JavaCompareUtilities.getJavaTextTools();
		JavaSourceViewerConfiguration configuration = new JavaSourceViewerConfiguration(tools.getColorManager(),
				this.fPreferenceStore, null, this.getDocumentPartitioning());
		if (editorInput != null) {
			// when input available, use editor
			CompilationUnitEditorAdapter editor = this.fEditor.get(sourceViewer);
			try {
				editor.init((IEditorSite) editor.getSite(), editorInput);
				editor.createActions();
				configuration = new JavaSourceViewerConfiguration(tools.getColorManager(), this.fPreferenceStore,
						editor, this.getDocumentPartitioning());
			} catch (PartInitException e) {
				JavaPlugin.log(e);
			}
		}
		this.fSourceViewerConfiguration.put(sourceViewer, configuration);
		return this.fSourceViewerConfiguration.get(sourceViewer);
	}

	@Override
	protected int findInsertionPosition(final char type, final ICompareInput input) {
		// System.out.println(getMethodName());

		int pos = super.findInsertionPosition(type, input);
		if (pos != 0) {
			return pos;
		}

		if (input instanceof IDiffElement) {

			// find the other (not deleted) element
			JavaNode otherJavaElement = null;
			ITypedElement otherElement = null;
			switch (type) {
			case 'L':
				otherElement = input.getRight();
				break;
			case 'R':
				otherElement = input.getLeft();
				break;
			}
			if (otherElement instanceof JavaNode) {
				otherJavaElement = (JavaNode) otherElement;
			}

			// find the parent of the deleted elements
			JavaNode javaContainer = null;
			IDiffElement diffElement = (IDiffElement) input;
			IDiffContainer container = diffElement.getParent();
			if (container instanceof ICompareInput) {

				ICompareInput parent = (ICompareInput) container;
				ITypedElement element = null;

				switch (type) {
				case 'L':
					element = parent.getLeft();
					break;
				case 'R':
					element = parent.getRight();
					break;
				}

				if (element instanceof JavaNode) {
					javaContainer = (JavaNode) element;
				}
			}

			if (otherJavaElement != null && javaContainer != null) {

				Object[] children;
				Position p;

				switch (otherJavaElement.getTypeCode()) {

				case JavaNode.PACKAGE:
					return 0;

				case JavaNode.IMPORT_CONTAINER:
					// we have to find the place after the package declaration
					children = javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode packageDecl = null;
						for (JavaNode child : (JavaNode[]) children) {
							switch (child.getTypeCode()) {
							case JavaNode.PACKAGE:
								packageDecl = child;
								break;
							case JavaNode.CLASS:
								return child.getRange().getOffset();
							}
						}
						if (packageDecl != null) {
							p = packageDecl.getRange();
							return p.getOffset() + p.getLength();
						}
					}
					return javaContainer.getRange().getOffset();

				case JavaNode.IMPORT:
					// append after last import
					p = javaContainer.getRange();
					return p.getOffset() + p.getLength();

				case JavaNode.CLASS:
					// append after last class
					children = javaContainer.getChildren();
					for (int i = children.length - 1; i >= 0; i--) {
						JavaNode child = (JavaNode) children[i];
						switch (child.getTypeCode()) {
						case JavaNode.CLASS:
						case JavaNode.IMPORT_CONTAINER:
						case JavaNode.PACKAGE:
						case JavaNode.FIELD:
							p = child.getRange();
							return p.getOffset() + p.getLength();
						}
					}
					return javaContainer.getAppendPosition().getOffset();

				case JavaNode.METHOD:
					// append in next line after last child
					children = javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode child = (JavaNode) children[children.length - 1];
						p = child.getRange();
						return this.findEndOfLine(javaContainer, p.getOffset() + p.getLength());
					}
					// otherwise use position from parser
					return javaContainer.getAppendPosition().getOffset();

				case JavaNode.FIELD:
					// append after last field
					children = javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode method = null;
						for (int i = children.length - 1; i >= 0; i--) {
							JavaNode child = (JavaNode) children[i];
							switch (child.getTypeCode()) {
							case JavaNode.METHOD:
								method = child;
								break;
							case JavaNode.FIELD:
								p = child.getRange();
								return p.getOffset() + p.getLength();
							}
						}
						if (method != null) {
							return method.getRange().getOffset();
						}
					}
					return javaContainer.getAppendPosition().getOffset();
				}
			}

			if (javaContainer != null) {
				// return end of container
				Position p = javaContainer.getRange();
				return p.getOffset() + p.getLength();
			}
		}

		// we give up
		return 0;
	}

	private int findEndOfLine(final JavaNode container, int pos) {
		// System.out.println(getMethodName());
		int line;
		IDocument doc = container.getDocument();
		try {
			line = doc.getLineOfOffset(pos);
			pos = doc.getLineOffset(line + 1);
		} catch (BadLocationException ex) {
			// silently ignored
		}

		// ensure that position is within container range
		Position containerRange = container.getRange();
		int start = containerRange.getOffset();
		int end = containerRange.getOffset() + containerRange.getLength();
		if (pos < start) {
			return start;
		}
		if (pos >= end) {
			return end - 1;
		}

		return pos;
	}

	private void setPreferenceStore(final IPreferenceStore ps) {
		// System.out.println(getMethodName());
		if (this.fPreferenceChangeListener != null) {
			if (this.fPreferenceStore != null) {
				this.fPreferenceStore.removePropertyChangeListener(this.fPreferenceChangeListener);
			}
			this.fPreferenceChangeListener = null;
		}
		this.fPreferenceStore = ps;
		if (this.fPreferenceStore != null) {
			this.fPreferenceChangeListener = JavaMergeViewer.this::handlePropertyChange;
			this.fPreferenceStore.addPropertyChangeListener(this.fPreferenceChangeListener);
		}
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#createSourceViewer(org.eclipse.swt.widgets.Composite, int)
	 * @since 3.5
	 */
	@Override
	protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
		// System.out.println(getMethodName());
		SourceViewer sourceViewer;
		if (this.getSite() != null) {
			CompilationUnitEditorAdapter editor = new CompilationUnitEditorAdapter(textOrientation);
			editor.createPartControl(parent);

			ISourceViewer iSourceViewer = editor.getViewer();
			Assert.isTrue(iSourceViewer instanceof SourceViewer);
			sourceViewer = (SourceViewer) iSourceViewer;
			if (this.fEditor == null) {
				this.fEditor = new HashMap<>(3);
			}
			this.fEditor.put(sourceViewer, editor);
		} else {
			sourceViewer = super.createSourceViewer(parent, textOrientation);
		}

		if (this.fSourceViewer == null) {
			this.fSourceViewer = new ArrayList<>();
		}
		this.fSourceViewer.add(sourceViewer);

		return sourceViewer;
	}

	@Override
	protected void setActionsActivated(final SourceViewer sourceViewer, final boolean state) {
		// System.out.println(getMethodName());
		if (this.fEditor != null) {
			Object editor = this.fEditor.get(sourceViewer);
			if (editor instanceof CompilationUnitEditorAdapter) {
				CompilationUnitEditorAdapter cuea = (CompilationUnitEditorAdapter) editor;
				cuea.setActionsActivated(state);

				IAction saveAction = cuea.getAction(ITextEditorActionConstants.SAVE);
				if (saveAction instanceof IPageListener) {
					PartEventAction partEventAction = (PartEventAction) saveAction;
					IWorkbenchPart compareEditorPart = this.getCompareConfiguration().getContainer().getWorkbenchPart();
					if (state) {
						partEventAction.partActivated(compareEditorPart);
					} else {
						partEventAction.partDeactivated(compareEditorPart);
					}
				}
			}
		}
	}

	@Override
	protected void createControls(final Composite composite) {
		// System.out.println(getMethodName());
		super.createControls(composite);
		IWorkbenchPart workbenchPart = this.getCompareConfiguration().getContainer().getWorkbenchPart();
		if (workbenchPart != null) {
			IContextService service = workbenchPart.getSite().getService(IContextService.class);
			if (service != null) {
				service.activateContext("org.eclipse.jdt.ui.javaEditorScope"); //$NON-NLS-1$
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		// System.out.println(getMethodName());
		if (adapter == ITextEditorExtension3.class) {
			IEditorInput activeInput = super.getAdapter(IEditorInput.class);
			if (activeInput != null) {
				for (CompilationUnitEditorAdapter editor : this.fEditor.values()) {
					if (activeInput.equals(editor.getEditorInput())) {
						return (T) editor;
					}
				}
			}
			return null;
		}
		return super.getAdapter(adapter);
	}

	private class CompilationUnitEditorAdapter extends CompilationUnitEditor {
		private boolean fInputSet = false;
		private final int fTextOrientation;
		private boolean fEditable;

		CompilationUnitEditorAdapter(final int textOrientation) {
			super();
			// System.out.println(getMethodName());
			this.fTextOrientation = textOrientation;
			// TODO: has to be set here
			this.setPreferenceStore(JavaMergeViewer.this.createChainedPreferenceStore(null));
		}

		private void setEditable(final boolean editable) {
			// System.out.println(getMethodName());
			this.fEditable = editable;
		}

		@Override
		public IWorkbenchPartSite getSite() {
			//			System.out.println(getMethodName());
			return JavaMergeViewer.this.getSite();
		}

		@Override
		public void createActions() {
			// System.out.println(getMethodName());
			if (this.fInputSet) {
				super.createActions();
				// to avoid handler conflicts disable extra actions
				// we're not handling by CompareHandlerService
				this.getCorrectionCommands().deregisterCommands();
				this.getRefactorActionGroup().dispose();
				this.getGenerateActionGroup().dispose();
			}
			// else do nothing, we will create actions later, when input is available
		}

		@Override
		public void createPartControl(final Composite composite) {
			// System.out.println(getMethodName());
			SourceViewer sourceViewer = (SourceViewer) this.createJavaSourceViewer(composite, new CompositeRuler(),
					null, false, this.fTextOrientation | SWT.H_SCROLL | SWT.V_SCROLL,
					JavaMergeViewer.this.createChainedPreferenceStore(null));
			JavaMergeViewer.this.setSourceViewer(this, sourceViewer);
			this.createNavigationActions();
			this.getSelectionProvider().addSelectionChangedListener(this.getSelectionChangedListener());
		}

		@Override
		protected ISourceViewer createJavaSourceViewer(final Composite parent, final IVerticalRuler verticalRuler,
				final IOverviewRuler overviewRuler, final boolean isOverviewRulerVisible, final int styles,
				final IPreferenceStore store) {
			// System.out.println(getMethodName());
			return new AdaptedSourceViewer(parent, verticalRuler, overviewRuler, isOverviewRulerVisible, styles,
					store) {
				@Override
				protected void handleDispose() {
					super.handleDispose();

					// dispose the compilation unit adapter
					CompilationUnitEditorAdapter.this.dispose();

					JavaMergeViewer.this.fEditor.remove(this);
					if (JavaMergeViewer.this.fEditor.isEmpty()) {
						JavaMergeViewer.this.fEditor = null;
						JavaMergeViewer.this.fSite = null;
					}

					JavaMergeViewer.this.fSourceViewer.remove(this);
					if (JavaMergeViewer.this.fSourceViewer.isEmpty()) {
						JavaMergeViewer.this.fSourceViewer = null;
					}

				}
			};
		}

		@Override
		protected void doSetInput(final IEditorInput input) throws CoreException {
			// System.out.println(getMethodName());
			super.doSetInput(input);
			// the editor input has been explicitly set
			this.fInputSet = true;
		}

		// called by org.eclipse.ui.texteditor.TextEditorAction.canModifyEditor()
		@Override
		public boolean isEditable() {
			// System.out.println(getMethodName());
			return this.fEditable;
		}

		@Override
		public boolean isEditorInputModifiable() {
			//			System.out.println(getMethodName());
			return this.fEditable;
		}

		@Override
		public boolean isEditorInputReadOnly() {
			// System.out.println(getMethodName());
			return !this.fEditable;
		}

		@Override
		protected boolean isWordWrapSupported() {
			// System.out.println(getMethodName());
			return false;
		}

		@Override
		protected void setActionsActivated(final boolean state) {
			//			System.out.println(getMethodName());
			super.setActionsActivated(state);
		}

		@Override
		public void close(final boolean save) {
			// System.out.println(getMethodName());
			this.getDocumentProvider().disconnect(this.getEditorInput());
		}

		@Override
		protected void installCodeMiningProviders() {
			// System.out.println(getMethodName());
			// Don't install code minings to avoid drawing minings in the Java compare editors.
		}
	}

	// no setter to private field AbstractTextEditor.fSourceViewer
	private void setSourceViewer(final ITextEditor editor, final SourceViewer viewer) {
		// System.out.println(getMethodName());
		Field field = null;
		try {
			field = AbstractTextEditor.class.getDeclaredField("fSourceViewer"); //$NON-NLS-1$
		} catch (SecurityException | NoSuchFieldException ex) {
			JavaPlugin.log(ex);
		}
		Objects.requireNonNull(field);
		field.setAccessible(true);
		try {
			field.set(editor, viewer);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			JavaPlugin.log(ex);
		}
	}
}