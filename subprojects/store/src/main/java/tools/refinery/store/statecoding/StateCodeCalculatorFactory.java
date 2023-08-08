/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.model.Interpretation;

import java.util.List;

public interface StateCodeCalculatorFactory {
	StateCodeCalculator create(List<? extends Interpretation<?>> interpretations, IntSet individuals);
}
