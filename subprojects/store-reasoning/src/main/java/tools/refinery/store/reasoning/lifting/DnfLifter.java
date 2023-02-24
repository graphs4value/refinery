package tools.refinery.store.reasoning.lifting;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.reasoning.literal.ModalDnfCallLiteral;
import tools.refinery.store.reasoning.Reasoning;
import tools.refinery.store.reasoning.literal.ModalRelationLiteral;
import tools.refinery.store.reasoning.literal.PartialRelationLiteral;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.DnfBuilder;
import tools.refinery.store.query.DnfClause;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.DnfCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.literal.Modality;
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
		var quantifiedVariables = new HashSet<>(clause.quantifiedVariables());
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
			// Quantify over variables that are not already quantified with the expected modality.
			liftedLiterals.add(Reasoning.EXISTS.call(CallPolarity.POSITIVE, modality,
					List.of(quantifiedVariable)));
		}
		builder.clause(liftedLiterals);
		return changed || !quantifiedVariables.isEmpty();
	}

	@Nullable
	private Variable isExistsLiteralForVariable(Modality modality, Literal literal) {
		if (literal instanceof ModalRelationLiteral modalRelationLiteral &&
				modalRelationLiteral.getPolarity() == CallPolarity.POSITIVE &&
				modalRelationLiteral.getModality() == modality &&
				modalRelationLiteral.getTarget().equals(Reasoning.EXISTS)) {
			return modalRelationLiteral.getArguments().get(0);
		}
		return null;
	}

	@Nullable
	private Literal liftLiteral(Modality modality, Literal literal) {
		if (literal instanceof PartialRelationLiteral partialRelationLiteral) {
			return new ModalRelationLiteral(modality, partialRelationLiteral);
		} else if (literal instanceof DnfCallLiteral dnfCallLiteral) {
			var polarity = dnfCallLiteral.getPolarity();
			var target = dnfCallLiteral.getTarget();
			var liftedTarget = lift(modality.commute(polarity), target);
			if (target.equals(liftedTarget)) {
				return null;
			}
			return new DnfCallLiteral(polarity, liftedTarget, dnfCallLiteral.getArguments());
		} else if (literal instanceof ModalDnfCallLiteral modalDnfCallLiteral) {
			var liftedTarget = lift(modalDnfCallLiteral.getModality(), modalDnfCallLiteral.getTarget());
			return new DnfCallLiteral(modalDnfCallLiteral.getPolarity(), liftedTarget,
					modalDnfCallLiteral.getArguments());
		} else {
			return null;
		}
	}
}
