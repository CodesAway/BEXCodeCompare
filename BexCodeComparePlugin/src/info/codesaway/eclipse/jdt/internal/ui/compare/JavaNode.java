/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.CompareMessages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

/**
 * Comparable Java elements are represented as JavaNodes.
 * Extends the DocumentRangeNode with method signature information.
 */
class JavaNode extends DocumentRangeNode implements ITypedElement {

	public static final int CU = 0;
	public static final int PACKAGE = 1;
	public static final int IMPORT_CONTAINER = 2;
	public static final int IMPORT = 3;
	public static final int INTERFACE = 4;
	public static final int CLASS = 5;
	public static final int ENUM = 6;
	public static final int ANNOTATION = 7;
	public static final int FIELD = 8;
	public static final int INIT = 9;
	public static final int CONSTRUCTOR = 10;
	public static final int METHOD = 11;

	private int fInitializerCount = 1;

	/**
	 * Creates a JavaNode under the given parent.
	 * @param parent the parent node
	 * @param type the Java elements type. Legal values are from the range CU to METHOD of this class.
	 * @param name the name of the Java element
	 * @param start the starting position of the java element in the underlying document
	 * @param length the number of characters of the java element in the underlying document
	 */
	public JavaNode(final JavaNode parent, final int type, final String name, final int start, final int length) {
		super(parent, type, JavaCompareUtilities.buildID(type, name), parent.getDocument(), start, length);
		parent.addChild(this);
	}

	/**
	 * Creates a JavaNode for a CU. It represents the root of a
	 * JavaNode tree, so its parent is null.
	 * @param document the document which contains the Java element
	 */
	public JavaNode(final IDocument document) {
		super(CU, JavaCompareUtilities.buildID(CU, "root"), document, 0, document.getLength()); //$NON-NLS-1$
	}

	public String getInitializerCount() {
		return Integer.toString(this.fInitializerCount++);
	}

	/**
	 * Extracts the method name from the signature.
	 * Used for smart matching.
	 */
	public String extractMethodName() {
		String id = this.getId();
		int pos = id.indexOf('(');
		if (pos > 0) {
			return id.substring(1, pos);
		}
		return id.substring(1);
	}

	/**
	 * Extracts the method's arguments name the signature.
	 * Used for smart matching.
	 */
	public String extractArgumentList() {
		String id = this.getId();
		int pos = id.indexOf('(');
		if (pos >= 0) {
			return id.substring(pos + 1);
		}
		return id.substring(1);
	}

	/**
	 * Returns a name which is presented in the UI.
	 * @see ITypedElement#getName()
	 */
	@Override
	public String getName() {

		switch (this.getTypeCode()) {
		case INIT:
			return CompareMessages.JavaNode_initializer;
		case IMPORT_CONTAINER:
			return CompareMessages.JavaNode_importDeclarations;
		case CU:
			return CompareMessages.JavaNode_compilationUnit;
		case PACKAGE:
			return CompareMessages.JavaNode_packageDeclaration;
		}
		return this.getId().substring(1); // we strip away the type character
	}

	/*
	 * @see ITypedElement#getType()
	 */
	@Override
	public String getType() {
		return "java2"; //$NON-NLS-1$
	}

	/**
	 * Returns a shared image for this Java element.
	 *
	 * see ITypedInput.getImage
	 */
	@Override
	public Image getImage() {

		ImageDescriptor id = null;

		switch (this.getTypeCode()) {
		case CU:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.COMPILATION_UNIT);
			break;
		case PACKAGE:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.PACKAGE_DECLARATION);
			break;
		case IMPORT:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.IMPORT_DECLARATION);
			break;
		case IMPORT_CONTAINER:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.IMPORT_CONTAINER);
			break;
		case CLASS:
			id = JavaCompareUtilities.getTypeImageDescriptor(true);
			break;
		case INTERFACE:
			id = JavaCompareUtilities.getTypeImageDescriptor(false);
			break;
		case INIT:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.INITIALIZER);
			break;
		case CONSTRUCTOR:
		case METHOD:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.METHOD);
			break;
		case FIELD:
			id = JavaCompareUtilities.getImageDescriptor(IJavaElement.FIELD);
			break;
		case ENUM:
			id = JavaCompareUtilities.getEnumImageDescriptor();
			break;
		case ANNOTATION:
			id = JavaCompareUtilities.getAnnotationImageDescriptor();
			break;
		}
		return JavaPlugin.getImageDescriptorRegistry().get(id);
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getType() + ": " + this.getName() //$NON-NLS-1$
				+ "[" + this.getRange().offset + "+" + this.getRange().length + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
