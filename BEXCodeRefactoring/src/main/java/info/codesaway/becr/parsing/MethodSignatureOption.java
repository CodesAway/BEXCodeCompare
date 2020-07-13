package info.codesaway.becr.parsing;

import org.eclipse.jdt.core.dom.ITypeBinding;

public enum MethodSignatureOption {
	USE_SHORT_NAME,
	/**
	 * Get the erased type
	 * @see ITypeBinding#getErasure()
	 */
	GET_ERASURE;
}
