/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.term.ExtremeValueAggregator;

import java.util.Comparator;

public final class IntExtremeValueAggregator {
	public static final ExtremeValueAggregator<Integer> MINIMUM = new ExtremeValueAggregator<>(Integer.class,
			Integer.MAX_VALUE);

	public static final ExtremeValueAggregator<Integer> MAXIMUM = new ExtremeValueAggregator<>(Integer.class,
			Integer.MIN_VALUE, Comparator.reverseOrder());

	private IntExtremeValueAggregator() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}
}
