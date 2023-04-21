/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfBuilder;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.literal.CallLiteral;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.util.CycleDetectingMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DnfLifter {
	private final CycleDetectingMapper<ModalDnf, Dnf> mapper = new CycleDetectingMapper<>(ModalDnf::toString,
			this::doLift);

	public Dnf lift(Modality modality, Dnf query) {
		return mapper.map(new ModalDnf(modality, query));
	}

	private Dnf doLift(ModalDnf modalDnf) {
		var modality = modalDnf.modality();
		var dnf = modalDnf.dnf();
		var builder = Dnf.builder();
		builder.parameters(dnf.getParameters());
		boolean changed = false;
		for (var clause : dnf.getClauses()) {
			if (liftClause(modality, clause, builder)) {
				changed = true;
			}
		}
		if (changed) {
			return builder.build();
		}
		return dnf;
	}

	private boolean liftClause(Modality modality, DnfClause clause, DnfBuilder builder) {
		boolean changed = false;
		var quantifiedVariables = new HashSet<>(clause.boundVariables()
				.stream()
				.filter(DataVariable.class::isInstance)
				.toList());
		var literals = clause.literals();
		var liftedLiterals = new ArrayList<Literal>(literals.size());
		for (var literal : literals) {
			Literal liftedLiteral = liftLiteral(modality, literal);
			if (liftedLiteral == null) {
				liftedLiteral = literal;
			} else {
				changed = true;
			}
			liftedLiterals.add(liftedLiteral);
			var variable = isExistsLiteralForVariable(modality, liftedLiteral);
			if (variable != null) {
				// If we already quantify over the existence of the variable with the expected modality,
				// we don't need to insert quantification manually.
				quantifiedVariables.remove(variable);
			}
		}
		for (var quantifiedVariable : quantifiedVariables) {
			// Quantify over data variables that are not already quantified with the expected modality.
			liftedLiterals.add(new CallLiteral(CallPolarity.POSITIVE,
					new ModalConstraint(modality, ReasoningAdapter.EXISTS), List.of(quantifiedVariable)));
		}
		builder.clause(liftedLiterals);
		return changed || !quantifiedVariables.isEmpty();
	}

	@Nullable
	private Variable isExistsLiteralForVariable(Modality modality, Literal literal) {
		if (literal instanceof CallLiteral callLiteral &&
				callLiteral.getPolarity() == CallPolarity.POSITIVE &&
				callLiteral.getTarget() instanceof ModalConstraint modalConstraint &&
				modalConstraint.modality() == modality &&
				modalConstraint.constraint().equals(ReasoningAdapter.EXISTS)) {
			return callLiteral.getArguments().get(0);
		}
		return null;
	}

	@Nullable
	private Literal liftLiteral(Modality modality, Literal literal) {
		if (!(literal instanceof CallLiteral callLiteral)) {
			return null;
		}
		var target = callLiteral.getTarget();
		if (target instanceof ModalConstraint modalTarget) {
			var actualTarget = modalTarget.constraint();
			if (actualTarget instanceof Dnf dnf) {
				var targetModality = modalTarget.modality();
				var liftedTarget = lift(targetModality, dnf);
				return new CallLiteral(callLiteral.getPolarity(), liftedTarget, callLiteral.getArguments());
			}
			// No more lifting to be done, pass any modal call to a partial symbol through.
			return null;
		} else if (target instanceof Dnf dnf) {
			var polarity = callLiteral.getPolarity();
			var liftedTarget = lift(modality.commute(polarity), dnf);
			// Use == instead of equals(), because lift will return the same object by reference is there are no
			// changes made during lifting.
			return liftedTarget == target ? null : new CallLiteral(polarity, liftedTarget, callLiteral.getArguments());
		} else {
			return PartialLiterals.addModality(callLiteral, modality);
		}
	}
}
