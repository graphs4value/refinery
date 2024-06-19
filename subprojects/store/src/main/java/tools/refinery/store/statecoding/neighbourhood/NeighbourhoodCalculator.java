/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class NeighbourhoodCalculator extends AbstractNeighbourhoodCalculator<Interpretation<?>> {
	private final List<Interpretation<?>> interpretations;

	public NeighbourhoodCalculator(Model model, List<? extends Interpretation<?>> interpretations,
								   IntSet individuals) {
		super(model, individuals);
		this.interpretations = List.copyOf(interpretations);
	}

	@Override
	public List<Interpretation<?>> getInterpretations() {
		return interpretations;
	}

	@Override
	protected int getArity(Interpretation<?> interpretation) {
		return interpretation.getSymbol().arity();
	}

	@Override
	protected Object getNullValue(Interpretation<?> interpretation) {
		return interpretation.get(Tuple.of());
	}

	@Override
	protected Cursor<Tuple, ?> getCursor(Interpretation<?> interpretation) {
		return interpretation.getAll();
	}
}
