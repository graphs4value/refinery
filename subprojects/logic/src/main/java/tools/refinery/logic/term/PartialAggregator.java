/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;

public non-sealed interface PartialAggregator<
		A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> extends AnyPartialAggregator {
	@Override
	AbstractDomain<A2, C2> getBodyDomain();

	@Override
	AbstractDomain<A, C> getResultDomain();
}
