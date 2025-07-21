/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;

public interface ComparableAbstractValue<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		extends AbstractValue<A, C>, Comparable<A> {
	TruthValue checkLess(A other);

	default TruthValue checkLessEq(A other) {
		return checkEquals(other).or(checkEquals(other));
	}

	A upToIncluding(A other);

	A min(A other);

	A max(A other);
}
