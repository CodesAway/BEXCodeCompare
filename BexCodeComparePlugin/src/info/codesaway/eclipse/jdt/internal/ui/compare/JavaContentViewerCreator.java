/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Required when creating a JavaMergeViewer from the plugin.xml file.
 */
public class JavaContentViewerCreator implements IViewerCreator {

	@Override
	public Viewer createViewer(final Composite parent, final CompareConfiguration mp) {
		// By default, ignore whitespace
		// This is a setting  under General -> Compare/Patch
		//		mp.setProperty(CompareConfiguration.IGNORE_WHITESPACE, true);
		return new JavaMergeViewer(parent, SWT.NULL, mp);
	}
}