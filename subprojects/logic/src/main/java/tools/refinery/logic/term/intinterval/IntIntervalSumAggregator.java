/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.term.PartialAggregator;

public class IntIntervalSumAggregator implements PartialAggregator<IntInterval, Integer, IntInterval, Integer> {
	@Override
	public AbstractDomain<IntInterval, Integer> getBodyDomain() {
		return IntIntervalDomain.INSTANCE;
	}

	@Override
	public AbstractDomain<IntInterval, Integer> getResultDomain() {
		return IntIntervalDomain.INSTANCE;
	}
}
