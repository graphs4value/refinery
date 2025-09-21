/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic;

import tools.refinery.logic.term.ComparableAbstractValue;

public interface ComparableAbstractDomain<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		extends AbstractDomain<A, C> {
	A negativeInfinity();

	A positiveInfinity();
}
