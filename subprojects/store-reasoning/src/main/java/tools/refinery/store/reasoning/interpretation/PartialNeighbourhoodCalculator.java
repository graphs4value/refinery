/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.neighbourhood.AbstractNeighbourhoodCalculator;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class PartialNeighbourhoodCalculator extends AbstractNeighbourhoodCalculator<PartialInterpretation<?, ?>> {
	private final ModelQueryAdapter queryAdapter;
	private final Concreteness concreteness;

	protected PartialNeighbourhoodCalculator(Model model, IntSet individuals, Concreteness concreteness, int depth) {
		super(model, individuals, depth);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		this.concreteness = concreteness;
	}

	@Override
	public StateCoderResult calculateCodes() {
		queryAdapter.flushChanges();
		return super.calculateCodes();
	}

	@Override
	protected List<PartialInterpretation<?, ?>> getInterpretations() {
		var adapter = getModel().getAdapter(ReasoningAdapter.class);
		var partialSymbols = adapter.getStoreAdapter().getPartialSymbols();
		return partialSymbols.stream()
				.<PartialInterpretation<?, ?>>map(partialSymbol ->
						adapter.getPartialInterpretation(concreteness, (PartialSymbol<?, ?>) partialSymbol))
				.toList();
	}

	@Override
	protected int getArity(PartialInterpretation<?, ?> interpretation) {
		return interpretation.getPartialSymbol().arity();
	}

	@Override
	protected Object getNullValue(PartialInterpretation<?, ?> interpretation) {
		return interpretation.get(Tuple.of());
	}

	@Override
	protected Cursor<Tuple, ?> getCursor(PartialInterpretation<?, ?> interpretation) {
		return interpretation.getAll();
	}

	public static StateCodeCalculatorFactory factory(Concreteness concreteness, int depth) {
		return (model, interpretations, individuals) -> new PartialNeighbourhoodCalculator(model, individuals,
				concreteness, depth);
	}
}
