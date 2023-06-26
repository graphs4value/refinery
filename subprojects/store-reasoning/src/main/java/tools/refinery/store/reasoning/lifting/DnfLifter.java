/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.equality.DnfEqualityChecker;
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

public class DnfLifter {
	private final Map<ModalDnf, Dnf> cache = new HashMap<>();

	public Dnf lift(Modality modality, Concreteness concreteness, Dnf dnf) {
		return cache.computeIfAbsent(new ModalDnf(modality, concreteness, dnf), this::doLift);
	}

	private Dnf doLift(ModalDnf modalDnf) {
		var modality = modalDnf.modality();
		var concreteness = modalDnf.concreteness();
		var dnf = modalDnf.dnf();
		var builder = Dnf.builder("%s#%s#%s".formatted(dnf.name(), modality, concreteness));
		builder.symbolicParameters(dnf.getSymbolicParameters());
		builder.functionalDependencies(dnf.getFunctionalDependencies());
		for (var clause : dnf.getClauses()) {
			builder.clause(liftClause(modality, concreteness, dnf, clause));
		}
		var liftedDnf = builder.build();
		if (dnf.equalsWithSubstitution(DnfEqualityChecker.DEFAULT, liftedDnf)) {
			return dnf;
		}
		return liftedDnf;
	}

	private List<Literal> liftClause(Modality modality, Concreteness concreteness, Dnf dnf, DnfClause clause) {
		var liftedLiterals = new ArrayList<Literal>();
		for (var literal : clause.literals()) {
			var liftedLiteral = liftLiteral(modality, concreteness, clause, literal);
			liftedLiterals.add(liftedLiteral);
		}
		var quantifiedVariables = getQuantifiedNodeVariables(dnf, clause);
		var existsConstraint = ModalConstraint.of(modality, concreteness, ReasoningAdapter.EXISTS);
		for (var quantifiedVariable : quantifiedVariables) {
			liftedLiterals.add(existsConstraint.call(quantifiedVariable));
		}
		return liftedLiterals;
	}

	private Literal liftLiteral(Modality modality, Concreteness concreteness, DnfClause clause, Literal literal) {
		if (literal instanceof CallLiteral callLiteral) {
			return liftCallLiteral(modality, concreteness, clause, callLiteral);
		} else if (literal instanceof EquivalenceLiteral equivalenceLiteral) {
			return liftEquivalenceLiteral(modality, concreteness, equivalenceLiteral);
		} else if (literal instanceof ConstantLiteral ||
				literal instanceof AssignLiteral<?> ||
				literal instanceof CheckLiteral) {
			return literal;
		} else if (literal instanceof CountLiteral) {
			throw new IllegalArgumentException("Count literal %s cannot be lifted".formatted(literal));
		} else if (literal instanceof AggregationLiteral<?,?>) {
			throw new IllegalArgumentException("Aggregation literal %s cannot be lifted".formatted(literal));
		} else {
			throw new IllegalArgumentException("Unknown literal to lift: " + literal);
		}
	}

	private Literal liftCallLiteral(Modality modality, Concreteness concreteness, DnfClause clause,
									CallLiteral callLiteral) {
		Constraint target = callLiteral.getTarget();
		var polarity = callLiteral.getPolarity();
		var arguments = callLiteral.getArguments();
		return switch (polarity) {
			case POSITIVE -> ModalConstraint.of(modality, concreteness, target).call(
					CallPolarity.POSITIVE, arguments);
			case NEGATIVE -> {
				var callModality = modality.negate();
				boolean needsHelper = !polarity.isPositive() &&
						!callLiteral.getPrivateVariables(clause.positiveVariables()).isEmpty();
				if (needsHelper) {
					yield callNegationHelper(callModality, concreteness, clause, callLiteral, target);
				}
				yield ModalConstraint.of(callModality, concreteness, target).call(
						CallPolarity.NEGATIVE, arguments);
			}
			case TRANSITIVE -> createTransitiveHelper(modality, concreteness, target).call(
					CallPolarity.POSITIVE, arguments);
		};
	}

	private Literal callNegationHelper(Modality modality, Concreteness concreteness, DnfClause clause,
									   CallLiteral callLiteral, Constraint target) {
		var builder = Dnf.builder();
		var originalArguments = callLiteral.getArguments();
		var uniqueOriginalArguments = List.copyOf(new LinkedHashSet<>(originalArguments));

		var alwaysInputVariables = callLiteral.getInputVariables(Set.of());
		for (var variable : uniqueOriginalArguments) {
			var direction = alwaysInputVariables.contains(variable) ? ParameterDirection.IN : ParameterDirection.OUT;
			builder.parameter(variable, direction);
		}

		var literals = new ArrayList<Literal>();
		var liftedConstraint = ModalConstraint.of(modality, concreteness, target);
		literals.add(liftedConstraint.call(CallPolarity.POSITIVE, originalArguments));

		var privateVariables = callLiteral.getPrivateVariables(clause.positiveVariables());
		var existsConstraint = ModalConstraint.of(modality, concreteness, ReasoningAdapter.EXISTS);
		for (var variable : uniqueOriginalArguments) {
			if (privateVariables.contains(variable)) {
				literals.add(existsConstraint.call(variable));
			}
		}

		builder.clause(literals);
		var liftedTarget = builder.build();
		return liftedTarget.call(CallPolarity.NEGATIVE, uniqueOriginalArguments);
	}

	private Dnf createTransitiveHelper(Modality modality, Concreteness concreteness, Constraint target) {
		var liftedTarget = ModalConstraint.of(modality, concreteness, target);
		var existsConstraint = ModalConstraint.of(modality, concreteness, ReasoningAdapter.EXISTS);
		var existingEndHelperName = "%s#exisitingEnd#%s#%s".formatted(target.name(), modality, concreteness);
		var existingEndHelper = Dnf.of(existingEndHelperName, builder -> {
			var start = builder.parameter("start");
			var end = builder.parameter("end");
			builder.clause(
					liftedTarget.call(start, end),
					existsConstraint.call(end)
			);
		});
		var transitiveHelperName = "%s#transitive#%s#%s".formatted(target.name(), modality, concreteness);
		return Dnf.of(transitiveHelperName, builder -> {
			var start = builder.parameter("start");
			var end = builder.parameter("end");
			builder.clause(liftedTarget.call(start, end));
			builder.clause(middle -> List.of(
					existingEndHelper.callTransitive(start, middle),
					liftedTarget.call(middle, end)
			));
		});
	}

	private Literal liftEquivalenceLiteral(Modality modality, Concreteness concreteness,
										   EquivalenceLiteral equivalenceLiteral) {
		if (equivalenceLiteral.isPositive()) {
			return ModalConstraint.of(modality, concreteness, ReasoningAdapter.EQUALS).call(
					CallPolarity.POSITIVE, equivalenceLiteral.getLeft(), equivalenceLiteral.getRight());
		}
		return ModalConstraint.of(modality.negate(), concreteness, ReasoningAdapter.EQUALS).call(
				CallPolarity.NEGATIVE, equivalenceLiteral.getLeft(), equivalenceLiteral.getRight());
	}

	private static Set<NodeVariable> getQuantifiedNodeVariables(Dnf dnf, DnfClause clause) {
		var quantifiedVariables = clause.positiveVariables().stream()
				.filter(Variable::isNodeVariable)
				.map(Variable::asNodeVariable)
				.collect(Collectors.toCollection(HashSet::new));
		for (var symbolicParameter : dnf.getSymbolicParameters()) {
			if (symbolicParameter.getVariable() instanceof NodeVariable nodeParameter) {
				quantifiedVariables.remove(nodeParameter);
			}
		}
		return quantifiedVariables;
	}

	private record ModalDnf(Modality modality, Concreteness concreteness, Dnf dnf) {
		@Override
		public String toString() {
			return "%s %s %s".formatted(modality, concreteness, dnf.name());
		}
	}
}
