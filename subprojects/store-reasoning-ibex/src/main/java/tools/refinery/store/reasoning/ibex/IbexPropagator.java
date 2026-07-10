/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import org.eclipse.collections.api.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import tools.refinery.ibex.Ibex;
import tools.refinery.ibex.IbexSolverLoader;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.realinterval.RealIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.ibex.internal.BoundIbexPropagator;
import tools.refinery.store.reasoning.ibex.internal.PreparedIbexRule;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.theory.TheoryRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

// Methods in this class are used as a fluent builder, so they always return `this`.
@SuppressWarnings("UnusedReturnValue")
public class IbexPropagator implements ModelStoreConfiguration {
	public static final double DEFAULT_PRECISION = 0.01;
	public static final double DEFAULT_RELATIVE_EPSILON = Ibex.RATIO;

	private double defaultPrecision = DEFAULT_PRECISION;
	private double relativeEpsilon = DEFAULT_RELATIVE_EPSILON;
	private final MutableObjectDoubleMap<AnyPartialSymbol> precisionMap = ObjectDoubleMaps.mutable.empty();
	private final List<TheoryRule> rules = new ArrayList<>();

	public IbexPropagator defaultPrecision(double defaultPrecision) {
		if (defaultPrecision <= 0) {
			throw new IllegalArgumentException("defaultPrecision must be positive");
		}
		this.defaultPrecision = defaultPrecision;
		return this;
	}

	public IbexPropagator precision(AnyPartialSymbol partialSymbol, double precision) {
		if (!RealIntervalDomain.INSTANCE.equals(partialSymbol.abstractDomain())) {
			throw new IllegalArgumentException("Only RealInterval can have precision.");
		}
		if (precision <= 0) {
			throw new IllegalArgumentException("precision must be positive");
		}
		precisionMap.put(partialSymbol, precision);
		return this;
	}

	public IbexPropagator precisions(ObjectDoubleMap<? extends AnyPartialSymbol> precisions) {
		precisions.forEachKeyValue(this::precision);
		return this;
	}

	public IbexPropagator relativeEpsilon(double relativeEpsilon) {
		if (relativeEpsilon <= 0 || relativeEpsilon >= 1) {
			throw new IllegalArgumentException("relativeEpsilon must be between 0 and 1 exclusive");
		}
		this.relativeEpsilon = relativeEpsilon;
		return this;
	}

	public IbexPropagator rule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		return rule(new TheoryRule(precondition, assertedTerm));
	}

	public IbexPropagator rule(TheoryRule rule) {
		rules.add(rule);
		return this;
	}

	public IbexPropagator rules(TheoryRule... rules) {
		return rules(List.of(rules));
	}

	public IbexPropagator rules(Collection<TheoryRule> rules) {
		this.rules.addAll(rules);
		return this;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		IbexSolverLoader.loadNativeLibraries();

		var preparedRules = rules.stream()
				.map(rule -> PreparedIbexRule.of(rule, defaultPrecision, precisionMap))
				.toList();

		var queryEngineBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		for (var preparedRule : preparedRules) {
			queryEngineBuilder.queries(preparedRule.partialPrecondition());
		}

		storeBuilder.getAdapter(PropagationBuilder.class)
				.propagator(model -> new BoundIbexPropagator(this, model, preparedRules, relativeEpsilon));

		storeBuilder.getAdapter(ReasoningBuilder.class)
				.requiredInterpretations(Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE));
	}
}
