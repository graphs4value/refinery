/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.term.StatelessAggregator;

// Singleton implementation, since there is only one way to sum integers.
@SuppressWarnings("squid:S6548")
public final class IntSumAggregator implements StatelessAggregator<Integer, Integer> {
	public static final IntSumAggregator INSTANCE = new IntSumAggregator();

	private IntSumAggregator() {
	}

	@Override
	public Class<Integer> getResultType() {
		return Integer.class;
	}

	@Override
	public Class<Integer> getInputType() {
		return Integer.class;
	}

	@NotNull
	@Override
	public Integer getEmptyResult() {
		return 0;
	}

	@Override
	public Integer add(Integer current, Integer value) {
		return current + value;
	}

	@Override
	public Integer remove(Integer current, Integer value) {
		return current - value;
	}
}
