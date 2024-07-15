/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighborhood;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class NeighborhoodCalculator extends AbstractNeighborhoodCalculator<Interpretation<?>> {
	public static final int DEFAULT_DEPTH = 7;

	private final List<Interpretation<?>> interpretations;

	protected NeighborhoodCalculator(Model model, List<? extends Interpretation<?>> interpretations,
									 IndividualsSet individuals, int depth) {
		super(model, individuals, depth);
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

	public static StateCodeCalculatorFactory factory(int depth) {
		return (model, interpretations, individuals) -> new NeighborhoodCalculator(model, interpretations,
				individuals, depth);
	}

	public static StateCodeCalculatorFactory factory() {
		return factory(DEFAULT_DEPTH);
	}
}
