/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ibex.IbexPropagator;
import tools.refinery.store.reasoning.ibex.expr.IbexTermChecker;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.store.reasoning.theory.TheorySupport;

import java.util.Collection;

class IbexTheory implements Theory {
	private final IbexTermChecker checker = new IbexTermChecker();
	private final double defaultPrecision;
	private final ObjectDoubleMap<AnyPartialSymbol> precisionMap;
	private final double relativeEpsilon;

	public IbexTheory(double defaultPrecision, ObjectDoubleMap<AnyPartialSymbol> precisionMap,
	                  double relativeEpsilon) {
		this.defaultPrecision = defaultPrecision;
		this.precisionMap = precisionMap;
		this.relativeEpsilon = relativeEpsilon;
	}

	@Override
	public TheorySupport checkSupport(TheoryRule theoryRule) {
		if (checker.isSupported(theoryRule.assertedTerm())) {
			return TheorySupport.ENABLED_BY_DEFAULT;
		}
		return TheorySupport.UNSUPPORTED;
	}

	@Override
	public void createPropagator(ModelStoreBuilder storeBuilder, Collection<TheoryRule> collectedRules) {
		storeBuilder.with(new IbexPropagator()
				.defaultPrecision(defaultPrecision)
				.precisions(precisionMap)
				.relativeEpsilon(relativeEpsilon)
				.rules(collectedRules));
	}

	@Override
	public int getPriority() {
		return Theory.DIVERGING_PRIORITY;
	}
}
