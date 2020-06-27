/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Adapts an options {@link IEclipsePreferences} to {@link org.eclipse.jface.preference.IPreferenceStore}.
 * <p>
 * This preference store is read-only i.e. write access
 * throws an {@link java.lang.UnsupportedOperationException}.
 * </p>
 *
 * @since 3.1
 */
class EclipsePreferencesAdapter implements IPreferenceStore {

	/**
	 * Preference change listener. Listens for events preferences
	 * fires a {@link org.eclipse.jface.util.PropertyChangeEvent}
	 * on this adapter with arguments from the received event.
	 */
	private class PreferenceChangeListener implements IEclipsePreferences.IPreferenceChangeListener {

		@Override
		public void preferenceChange(final IEclipsePreferences.PreferenceChangeEvent event) {
			if (Display.getCurrent() == null) {
				Display.getDefault()
						.asyncExec(() -> EclipsePreferencesAdapter.this.firePropertyChangeEvent(event.getKey(),
								event.getOldValue(), event.getNewValue()));
			} else {
				EclipsePreferencesAdapter.this.firePropertyChangeEvent(event.getKey(), event.getOldValue(),
						event.getNewValue());
			}
		}
	}

	/** Listeners on on this adapter */
	private final ListenerList<IPropertyChangeListener> fListeners = new ListenerList<>(ListenerList.IDENTITY);

	/** Listener on the node */
	private final IEclipsePreferences.IPreferenceChangeListener fListener = new PreferenceChangeListener();

	/** wrapped node */
	private final IScopeContext fContext;
	private final String fQualifier;

	/**
	 * Initialize with the node to wrap
	 *
	 * @param context The context to access
	 */
	public EclipsePreferencesAdapter(final IScopeContext context, final String qualifier) {
		this.fContext = context;
		this.fQualifier = qualifier;
	}

	private IEclipsePreferences getNode() {
		return this.fContext.getNode(this.fQualifier);
	}

	@Override
	public void addPropertyChangeListener(final IPropertyChangeListener listener) {
		if (this.fListeners.size() == 0) {
			this.getNode().addPreferenceChangeListener(this.fListener);
		}
		this.fListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(final IPropertyChangeListener listener) {
		this.fListeners.remove(listener);
		if (this.fListeners.size() == 0) {
			this.getNode().removePreferenceChangeListener(this.fListener);
		}
	}

	@Override
	public boolean contains(final String name) {
		return this.getNode().get(name, null) != null;
	}

	@Override
	public void firePropertyChangeEvent(final String name, final Object oldValue, final Object newValue) {
		PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
		for (IPropertyChangeListener listener : this.fListeners) {
			listener.propertyChange(event);
		}
	}

	@Override
	public boolean getBoolean(final String name) {
		return this.getNode().getBoolean(name, BOOLEAN_DEFAULT_DEFAULT);
	}

	@Override
	public boolean getDefaultBoolean(final String name) {
		return BOOLEAN_DEFAULT_DEFAULT;
	}

	@Override
	public double getDefaultDouble(final String name) {
		return DOUBLE_DEFAULT_DEFAULT;
	}

	@Override
	public float getDefaultFloat(final String name) {
		return FLOAT_DEFAULT_DEFAULT;
	}

	@Override
	public int getDefaultInt(final String name) {
		return INT_DEFAULT_DEFAULT;
	}

	@Override
	public long getDefaultLong(final String name) {
		return LONG_DEFAULT_DEFAULT;
	}

	@Override
	public String getDefaultString(final String name) {
		return STRING_DEFAULT_DEFAULT;
	}

	@Override
	public double getDouble(final String name) {
		return this.getNode().getDouble(name, DOUBLE_DEFAULT_DEFAULT);
	}

	@Override
	public float getFloat(final String name) {
		return this.getNode().getFloat(name, FLOAT_DEFAULT_DEFAULT);
	}

	@Override
	public int getInt(final String name) {
		return this.getNode().getInt(name, INT_DEFAULT_DEFAULT);
	}

	@Override
	public long getLong(final String name) {
		return this.getNode().getLong(name, LONG_DEFAULT_DEFAULT);
	}

	@Override
	public String getString(final String name) {
		return this.getNode().get(name, STRING_DEFAULT_DEFAULT);
	}

	@Override
	public boolean isDefault(final String name) {
		return false;
	}

	@Override
	public boolean needsSaving() {
		try {
			return this.getNode().keys().length > 0;
		} catch (BackingStoreException e) {
			// ignore
		}
		return true;
	}

	@Override
	public void putValue(final String name, final String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final String defaultObject) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(final String name, final boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setToDefault(final String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(final String name, final boolean value) {
		throw new UnsupportedOperationException();
	}

}