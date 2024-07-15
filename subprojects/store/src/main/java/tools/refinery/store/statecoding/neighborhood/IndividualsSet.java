/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighborhood;

import java.util.stream.IntStream;

public interface IndividualsSet {
	IndividualsSet EMPTY = new IndividualsSet() {
		@Override
		public boolean isIndividual(int nodeId) {
			return false;
		}

		@Override
		public IntStream stream() {
			return IntStream.empty();
		}
	};

	boolean isIndividual(int nodeId);

	IntStream stream();
}
