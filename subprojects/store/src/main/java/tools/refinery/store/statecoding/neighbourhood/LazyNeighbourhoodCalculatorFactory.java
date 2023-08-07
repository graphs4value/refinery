/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;

import java.util.List;

public class LazyNeighbourhoodCalculatorFactory implements StateCodeCalculatorFactory {
	@Override
	public StateCodeCalculator create(List<? extends Interpretation<?>> interpretations, IntSet individuals) {
		return new LazyNeighbourhoodCalculator(interpretations,individuals);
	}
}
