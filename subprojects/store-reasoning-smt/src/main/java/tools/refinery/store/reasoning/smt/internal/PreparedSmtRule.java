/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal;

import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.dnf.SymbolicParameter;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.bool.BoolTerms;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.smt.SmtRule;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.literal.PartialLiterals.candidateMust;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public record PreparedSmtRule(RelationalQuery partialPrecondition, RelationalQuery candidatePrecondition,
							  Term<TruthValue> assertedTerm, ObjectIntMap<NodeVariable> parameterMap,
							  List<Influence> influences) {
	public static PreparedSmtRule of(SmtRule rule) {
		var precondition = rule.precondition().getDnf();
		var symbolicParameters = precondition.getSymbolicParameters();
		var parameterVariables = symbolicParameters.stream().map(SymbolicParameter::getVariable).toList();
		var helper = Dnf.builder(precondition.name() + "#helper")
				.symbolicParameters(symbolicParameters)
				.clause(
						precondition.call(CallPolarity.POSITIVE, parameterVariables),
						check(BoolTerms.not(TruthValueTerms.must(rule.assertedTerm())))
				)
				.build();
		int literalCount = symbolicParameters.size() + 1;
		var partialLiterals = new ArrayList<Literal>(literalCount);
		var candidateLiterals = new ArrayList<Literal>(literalCount);
		partialLiterals.add(must(helper.call(CallPolarity.POSITIVE, parameterVariables)));
		candidateLiterals.add(candidateMust(helper.call(CallPolarity.POSITIVE, parameterVariables)));
		for (var symbolicParameter : symbolicParameters) {
			var variable = symbolicParameter.getVariable();
			partialLiterals.add(not(MULTI_VIEW.call(variable)));
			candidateLiterals.add(must(EXISTS_SYMBOL.call(variable)));
		}

		var concreteness = rule.concreteness();
		var partialPreconditionBuilder = Query.builder(precondition.name() + "#partial")
				.symbolicParameters(symbolicParameters);
		if (concreteness != ConcretenessSpecification.CANDIDATE) {
			partialPreconditionBuilder.clause(partialLiterals);
		}
		var partialPrecondition = partialPreconditionBuilder.build();
		var candidatePreconditionBuilder = Query.builder(precondition.name() + "#candidate")
				.symbolicParameters(symbolicParameters);
		if (concreteness != ConcretenessSpecification.PARTIAL) {
			candidatePreconditionBuilder.clause(candidateLiterals);
		}
		var candidatePrecondition = candidatePreconditionBuilder.build();

		var assertedTerm = rule.assertedTerm();
		int arity = parameterVariables.size();
		var mutableParameterMap = ObjectIntMaps.mutable.<NodeVariable>withInitialCapacity(arity);
		for (int i = 0; i < arity; i++) {
			mutableParameterMap.put(parameterVariables.get(i).asNodeVariable(), i);
		}
		var parameterMap = mutableParameterMap.toImmutable();
		var collector = new InfluenceCollector(parameterMap);
		collector.collectFromTerm(assertedTerm);
		return new PreparedSmtRule(partialPrecondition, candidatePrecondition, assertedTerm, parameterMap,
				collector.getInfluences());
	}

	public RelationalQuery getQuery(Concreteness concreteness) {
		return switch (concreteness) {
			case PARTIAL -> partialPrecondition;
            case CANDIDATE -> candidatePrecondition;
		};
	}

	public record Influence(AnyPartialFunction partialFunction, Tuple parameterIndices) {
	}

	private static class InfluenceCollector implements TermRewriter {
		private final ObjectIntMap<NodeVariable> parameterMap;
		private final Set<Influence> influences = new LinkedHashSet<>();

		public InfluenceCollector(ObjectIntMap<NodeVariable> parameterMap) {
			this.parameterMap = parameterMap;
		}

		public List<Influence> getInfluences() {
			return List.copyOf(influences);
		}

		public void collectFromTerm(Term<TruthValue> term) {
			rewriteTerm(term);
		}

		@Override
		public <T> Term<T> rewriteTerm(Term<T> term) {
			if (term instanceof PartialFunctionCallTerm<?,?> callTerm) {
				var partialFunction = callTerm.getPartialFunction();
				var parameterIndexArray = callTerm.getArguments().stream()
						.mapToInt(parameterMap::get)
						.toArray();
				var parameterIndices = Tuple.of(parameterIndexArray);
				influences.add(new Influence(partialFunction, parameterIndices));
			}
			return term.rewriteSubTerms(this);
		}
	}
}
