/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.dnf.*;
import tools.refinery.logic.equality.DnfEqualityChecker;
import tools.refinery.logic.literal.Literal;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.ModalitySpecification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnfLifter {
	private final Map<ModalDnf, Dnf> cache = new HashMap<>();

	public <T> Query<T> lift(Modality modality, Concreteness concreteness, Query<T> query) {
		return lift(modality.toSpecification(), concreteness.toSpecification(), query);
	}

	public <T> Query<T> lift(ModalitySpecification modality, ConcretenessSpecification concreteness, Query<T> query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public RelationalQuery lift(Modality modality, Concreteness concreteness, RelationalQuery query) {
		return lift(modality.toSpecification(), concreteness.toSpecification(), query);
	}

	public RelationalQuery lift(ModalitySpecification modality, ConcretenessSpecification concreteness,
								RelationalQuery query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public <T> FunctionalQuery<T> lift(Modality modality, Concreteness concreteness, FunctionalQuery<T> query) {
		return lift(modality.toSpecification(), concreteness.toSpecification(), query);
	}

	public <T> FunctionalQuery<T> lift(ModalitySpecification modality, ConcretenessSpecification concreteness,
									   FunctionalQuery<T> query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public Dnf lift(Modality modality, Concreteness concreteness, Dnf dnf) {
		return lift(modality.toSpecification(), concreteness.toSpecification(), dnf);
	}

	public Dnf lift(ModalitySpecification modality, ConcretenessSpecification concreteness, Dnf dnf) {
		if (modality == ModalitySpecification.UNSPECIFIED && concreteness == ConcretenessSpecification.UNSPECIFIED) {
			return dnf;
		}
		return cache.computeIfAbsent(new ModalDnf(modality, concreteness, dnf), this::doLift);
	}

	private Dnf doLift(ModalDnf modalDnf) {
		var modality = modalDnf.modality();
		var concreteness = modalDnf.concreteness();
		var dnf = modalDnf.dnf();
		var builder = Dnf.builder(decorateName(dnf.name(), modality, concreteness));
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

	private List<Literal> liftClause(ModalitySpecification modality, ConcretenessSpecification concreteness, Dnf dnf,
									 DnfClause clause) {
		var lifter = new ClauseLifter(modality, concreteness, dnf, clause);
		return lifter.liftClause();
	}

	private record ModalDnf(ModalitySpecification modality, ConcretenessSpecification concreteness, Dnf dnf) {
		@Override
		public @NotNull String toString() {
			return "%s %s %s".formatted(modality, concreteness, dnf.name());
		}
	}

	public static String decorateName(String name, Modality modality, Concreteness concreteness) {
		return decorateName(name, modality.toSpecification(), concreteness.toSpecification());
	}

	public static String decorateName(String name, ModalitySpecification modality,
									  ConcretenessSpecification concreteness) {
		var builder = new StringBuilder(name);
		if (modality != ModalitySpecification.UNSPECIFIED) {
			builder.append('#').append(modality);
		}
		if (concreteness != ConcretenessSpecification.UNSPECIFIED) {
			builder.append('#').append(concreteness);
		}
		return builder.toString();
	}
}
