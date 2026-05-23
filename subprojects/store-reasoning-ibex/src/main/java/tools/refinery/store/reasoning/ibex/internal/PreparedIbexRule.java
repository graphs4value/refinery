/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.internal;

import ibex.Ibex;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.dnf.SymbolicParameter;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.bool.BoolTerms;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.ibex.IbexRule;
import tools.refinery.store.reasoning.ibex.expr.TermToIbexConstraint;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

/**
 * Pre-processed form of an {@link IbexRule} ready for use at runtime.
 * <p>
 * Created once at store-build time and shared across all model instances.
 * The embedded {@link Ibex} solver is stateless w.r.t. domain values (domains are passed per call
 * to {@code contract()}), so sharing it across models is safe as long as calls are not concurrent.
 */
public record PreparedIbexRule(RelationalQuery partialPrecondition,
							   Term<TruthValue> assertedTerm,
							   ObjectIntMap<NodeVariable> parameterMap,
							   List<Influence> influences,
							   Ibex ibex) {

	/** Precision for real-valued variables (e.g. RealInterval attributes). */
	private static final double REAL_PRECISION = 1e-8;

	public static PreparedIbexRule of(IbexRule rule) {
		var precondition = rule.precondition().getDnf();
		var symbolicParameters = precondition.getSymbolicParameters();
		var parameterVariables = symbolicParameters.stream().map(SymbolicParameter::getVariable).toList();

		// helper query: precondition holds AND the asserted term does not yet must-hold
		var helper = Dnf.builder(precondition.name() + "#helper")
				.symbolicParameters(symbolicParameters)
				.clause(
						precondition.call(CallPolarity.POSITIVE, parameterVariables),
						check(BoolTerms.not(TruthValueTerms.must(rule.assertedTerm())))
				)
				.build();

		int literalCount = symbolicParameters.size() + 1;
		var partialLiterals = new ArrayList<Literal>(literalCount);
		partialLiterals.add(must(helper.call(CallPolarity.POSITIVE, parameterVariables)));
		for (var symbolicParameter : symbolicParameters) {
			// exclude multi-objects (nodes that represent a set of unknown size)
			partialLiterals.add(not(MULTI_VIEW.call(symbolicParameter.getVariable())));
		}

		var partialPrecondition = Query.builder(precondition.name() + "#partial")
				.symbolicParameters(symbolicParameters)
				.clause(partialLiterals)
				.build();

		// parameter index map: NodeVariable → position in rule parameter list
		int arity = parameterVariables.size();
		var mutableParamMap = ObjectIntMaps.mutable.<NodeVariable>withInitialCapacity(arity);
		for (int i = 0; i < arity; i++) {
			mutableParamMap.put(parameterVariables.get(i).asNodeVariable(), i);
		}
		var parameterMap = mutableParamMap.toImmutable();

		// collect which (partialFunction, parameterIndices) pairs appear in the assertedTerm
		var collector = new InfluenceCollector(parameterMap);
		collector.collectFromTerm(rule.assertedTerm());
		var influences = collector.getInfluences();

		// translate assertedTerm → IBEX constraint string, e.g. "({0}+18)<={1}"
		var termToIbex = new TermToIbexConstraint(influences, parameterMap);
		var constraintString = termToIbex.toConstraint(rule.assertedTerm());

		// build the IBEX solver: one variable slot per influence
		// -1 marks a variable as integral; >= 0 marks it as real with that precision
		int numVars = influences.size();
		var prec = new double[numVars];
		for (int i = 0; i < numVars; i++) {
			var domain = influences.get(i).partialFunction().abstractDomain();
			prec[i] = IntIntervalDomain.INSTANCE.equals(domain) ? -1 : REAL_PRECISION;
		}
		// preserve_rounding=true: restores Java's FPU rounding mode after each call
		var ibex = new Ibex(prec, true);
		ibex.add_ctr(constraintString);
		ibex.build();

		return new PreparedIbexRule(partialPrecondition, rule.assertedTerm(), parameterMap, influences, ibex);
	}

	/**
	 * Maps a (partialFunction, parameterIndices) pair to an IBEX variable index.
	 * The index is the position in the {@link #influences()} list.
	 */
	public record Influence(AnyPartialFunction partialFunction, Tuple parameterIndices) {
	}

	private static class InfluenceCollector implements TermRewriter {
		private final ObjectIntMap<NodeVariable> parameterMap;
		private final Set<Influence> influences = new LinkedHashSet<>();

		InfluenceCollector(ObjectIntMap<NodeVariable> parameterMap) {
			this.parameterMap = parameterMap;
		}

		List<Influence> getInfluences() {
			return List.copyOf(influences);
		}

		void collectFromTerm(Term<TruthValue> term) {
			rewriteTerm(term);
		}

		@Override
		public <T> Term<T> rewriteTerm(Term<T> term) {
			if (term instanceof PartialFunctionCallTerm<?, ?> callTerm) {
				var paramIndexArray = callTerm.getArguments().stream()
						.mapToInt(parameterMap::get)
						.toArray();
				influences.add(new Influence(callTerm.getPartialFunction(), Tuple.of(paramIndexArray)));
			}
			return term.rewriteSubTerms(this);
		}
	}
}
