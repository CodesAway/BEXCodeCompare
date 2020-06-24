/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package info.codesaway.eclipse.compare.rangedifferencer;

/**
 * @since org.eclipse.compare.core 3.5
 */
public abstract class AbstractRangeDifferenceFactory {
	protected abstract RangeDifference createRangeDifference();

	RangeDifference createRangeDifference(final int changeKind) {
		RangeDifference rangeDifference = this.createRangeDifference();
		rangeDifference.kind = changeKind;
		return rangeDifference;
	}

	// Changed to public since used in RangeComparatorBEX
	public RangeDifference createRangeDifference(final int kind, final int rightStart,
			final int rightLength, final int leftStart, final int leftLength) {
		RangeDifference rangeDifference = this.createRangeDifference();
		rangeDifference.kind = kind;
		rangeDifference.rightStart = rightStart;
		rangeDifference.rightLength = rightLength;
		rangeDifference.leftStart = leftStart;
		rangeDifference.leftLength = leftLength;
		return rangeDifference;
	}

	RangeDifference createRangeDifference(final int kind, final int rightStart,
			final int rightLength, final int leftStart, final int leftLength, final int ancestorStart,
			final int ancestorLength) {
		RangeDifference rangeDifference = this.createRangeDifference();
		rangeDifference.kind = kind;
		rangeDifference.rightStart = rightStart;
		rangeDifference.rightLength = rightLength;
		rangeDifference.leftStart = leftStart;
		rangeDifference.leftLength = leftLength;
		rangeDifference.ancestorStart = ancestorStart;
		rangeDifference.ancestorLength = ancestorLength;
		return rangeDifference;
	}
}