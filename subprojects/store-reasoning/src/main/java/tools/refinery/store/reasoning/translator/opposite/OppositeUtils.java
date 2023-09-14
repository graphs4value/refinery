/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.opposite;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple2;

final class OppositeUtils {
	private OppositeUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Tuple flip(Tuple tuple) {
		if (!(tuple instanceof Tuple2 tuple2)) {
			throw new IllegalArgumentException("Cannot flip tuple: " + tuple);
		}
		return Tuple.of(tuple2.value1(), tuple2.value0());
	}
}
