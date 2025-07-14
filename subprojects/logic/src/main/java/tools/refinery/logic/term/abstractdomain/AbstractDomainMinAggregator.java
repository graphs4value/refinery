/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.ComparableAbstractDomain;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.PartialAggregator;

public class AbstractDomainMinAggregator<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		implements PartialAggregator<A, C, A, C> {
	private final ComparableAbstractDomain<A, C> abstractDomain;

	public AbstractDomainMinAggregator(ComparableAbstractDomain<A, C> abstractDomain) {
		this.abstractDomain = abstractDomain;
	}

	@Override
	public ComparableAbstractDomain<A, C> getBodyDomain() {
		return abstractDomain;
	}

	@Override
	public AbstractDomain<A, C> getResultDomain() {
		return getBodyDomain();
	}
}
