/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.*;
import java.util.stream.Collectors;

class ClauseLifter {
	private final Modality modality;
	private final Concreteness concreteness;
	private final DnfClause clause;
	private final Set<NodeVariable> quantifiedVariables;
	private final Set<NodeVariable> existentialQuantifiersToAdd;

	public ClauseLifter(Modality modality, Concreteness concreteness, Dnf dnf, DnfClause clause) {
		this.modality = modality;
		this.concreteness = concreteness;
		this.clause = clause;
		quantifiedVariables = getQuantifiedNodeVariables(dnf, clause);
		existentialQuantifiersToAdd = new LinkedHashSet<>(quantifiedVariables);
	}

	private static Set<NodeVariable> getQuantifiedNodeVariables(Dnf dnf, DnfClause clause) {
		var quantifiedVariables = clause.positiveVariables().stream()
				.filter(Variable::isNodeVariable)
				.map(Variable::asNodeVariable)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		for (var symbolicParameter : dnf.getSymbolicParameters()) {
			if (symbolicParameter.getVariable() instanceof NodeVariable nodeParameter) {
				quantifiedVariables.remove(nodeParameter);
			}
		}
		return Collections.unmodifiableSet(quantifiedVariables);
	}

	public List<Literal> liftClause() {
		var liftedLiterals = new ArrayList<Literal>();
		for (var literal : clause.literals()) {
			var liftedLiteral = liftLiteral(literal);
			liftedLiterals.add(liftedLiteral);
		}
		var existsConstraint = ModalConstraint.of(modality, concreteness, ReasoningAdapter.EXISTS_SYMBOL);
		for (var quantifiedVariable : existentialQuantifiersToAdd) {
			liftedLiterals.add(existsConstraint.call(quantifiedVariable));
		}
		return liftedLiterals;
	}

	private Literal liftLiteral(Literal literal) {
		if (literal instanceof CallLiteral callLiteral) {
			return liftCallLiteral(callLiteral);
		} else if (literal instanceof EquivalenceLiteral equivalenceLiteral) {
			return liftEquivalenceLiteral(equivalenceLiteral);
		} else if (literal instanceof ConstantLiteral ||
				literal instanceof AssignLiteral<?> ||
				literal instanceof CheckLiteral) {
			return literal;
		} else if (literal instanceof AbstractCountLiteral<?>) {
			throw new IllegalArgumentException("Count literal %s cannot be lifted".formatted(literal));
		} else if (literal instanceof AggregationLiteral<?, ?>) {
			throw new IllegalArgumentException("Aggregation literal %s cannot be lifted".formatted(literal));
		} else if (literal instanceof RepresentativeElectionLiteral) {
			throw new IllegalArgumentException("SCC literal %s cannot be lifted".formatted(literal));
		} else {
			throw new IllegalArgumentException("Unknown literal to lift: " + literal);
		}
	}

	private Literal liftCallLiteral(CallLiteral callLiteral) {
		var polarity = callLiteral.getPolarity();
		return switch (polarity) {
			case POSITIVE -> {
				Constraint target = callLiteral.getTarget();
				var arguments = callLiteral.getArguments();
				yield ModalConstraint.of(modality, concreteness, target).call(CallPolarity.POSITIVE, arguments);
			}
			case NEGATIVE -> callNegationHelper(callLiteral);
			case TRANSITIVE -> callTransitiveHelper(callLiteral);
		};
	}

	private Literal callNegationHelper(CallLiteral callLiteral) {
		var target = callLiteral.getTarget();
		var originalArguments = callLiteral.getArguments();
		var negatedModality = modality.negate();
		var privateVariables = callLiteral.getPrivateVariables(clause.positiveVariables());
		if (privateVariables.isEmpty()) {
			// If there is no universal quantification, we may directly call the original Dnf.
			return ModalConstraint.of(negatedModality, concreteness, target)
					.call(CallPolarity.NEGATIVE, originalArguments);
		}

		var builder = Dnf.builder("%s#negation#%s#%s#%s"
				.formatted(target.name(), modality, concreteness, privateVariables));
		var uniqueOriginalArguments = List.copyOf(new LinkedHashSet<>(originalArguments));

		var alwaysInputVariables = callLiteral.getInputVariables(Set.of());
		for (var variable : uniqueOriginalArguments) {
			var direction = alwaysInputVariables.contains(variable) ? ParameterDirection.IN : ParameterDirection.OUT;
			builder.parameter(variable, direction);
		}

		var literals = new ArrayList<Literal>();
		var liftedConstraint = ModalConstraint.of(negatedModality, concreteness, target);
		literals.add(liftedConstraint.call(CallPolarity.POSITIVE, originalArguments));

		var existsConstraint = ModalConstraint.of(negatedModality, concreteness, ReasoningAdapter.EXISTS_SYMBOL);
		for (var variable : uniqueOriginalArguments) {
			if (privateVariables.contains(variable)) {
				literals.add(existsConstraint.call(variable));
			}
		}

		builder.clause(literals);
		var liftedTarget = builder.build();
		return liftedTarget.call(CallPolarity.NEGATIVE, uniqueOriginalArguments);
	}

	private Literal callTransitiveHelper(CallLiteral callLiteral) {
		var target = callLiteral.getTarget();
		var originalArguments = callLiteral.getArguments();
		var liftedTarget = ModalConstraint.of(modality, concreteness, target);

		var existsConstraint = ModalConstraint.of(modality, concreteness, ReasoningAdapter.EXISTS_SYMBOL);
		var existingEndHelperName = "%s#exisitingEnd#%s#%s".formatted(target.name(), modality, concreteness);
		var existingEndHelper = Dnf.of(existingEndHelperName, builder -> {
			var start = builder.parameter("start");
			var end = builder.parameter("end");
			builder.clause(
					liftedTarget.call(start, end),
					existsConstraint.call(end)
			);
		});

		// The start and end of a transitive path is always a node.
		var pathEnd = (NodeVariable) originalArguments.get(1);
		if (quantifiedVariables.contains(pathEnd)) {
			// The end of the path needs existential quantification anyway, so we don't need a second helper.
			// We replace the call to EXISTS with the transitive path call.
			existentialQuantifiersToAdd.remove(pathEnd);
			return existingEndHelper.call(CallPolarity.TRANSITIVE, originalArguments);
		}

		var transitiveHelperName = "%s#transitive#%s#%s".formatted(target.name(), modality, concreteness);
		var transitiveHelper = Dnf.of(transitiveHelperName, builder -> {
			var start = builder.parameter("start");
			var end = builder.parameter("end");
			// Make sure the end of the path is not existentially quantified.
			builder.clause(liftedTarget.call(start, end));
			builder.clause(middle -> List.of(
					existingEndHelper.callTransitive(start, middle),
					liftedTarget.call(middle, end)
			));
		});

		return transitiveHelper.call(CallPolarity.POSITIVE, originalArguments);
	}

	private Literal liftEquivalenceLiteral(EquivalenceLiteral equivalenceLiteral) {
		if (equivalenceLiteral.isPositive()) {
			return ModalConstraint.of(modality, concreteness, ReasoningAdapter.EQUALS_SYMBOL)
					.call(CallPolarity.POSITIVE, equivalenceLiteral.getLeft(), equivalenceLiteral.getRight());
		}
		return ModalConstraint.of(modality.negate(), concreteness, ReasoningAdapter.EQUALS_SYMBOL)
				.call(CallPolarity.NEGATIVE, equivalenceLiteral.getLeft(), equivalenceLiteral.getRight());
	}
}
