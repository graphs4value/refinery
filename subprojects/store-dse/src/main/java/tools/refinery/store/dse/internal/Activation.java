/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.internal;

import tools.refinery.store.tuple.Tuple;

public record Activation(TransformationRule transformationRule, Tuple activation) {
	public boolean fire() {
		return transformationRule.fireActivation(activation);
	}
}
