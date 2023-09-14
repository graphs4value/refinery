/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.dnf.*;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnfLifter {
	private final Map<ModalDnf, Dnf> cache = new HashMap<>();

	public <T> Query<T> lift(Modality modality, Concreteness concreteness, Query<T> query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public RelationalQuery lift(Modality modality, Concreteness concreteness, RelationalQuery query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public <T> FunctionalQuery<T> lift(Modality modality, Concreteness concreteness, FunctionalQuery<T> query) {
		var liftedDnf = lift(modality, concreteness, query.getDnf());
		return query.withDnf(liftedDnf);
	}

	public Dnf lift(Modality modality, Concreteness concreteness, Dnf dnf) {
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

	private List<Literal> liftClause(Modality modality, Concreteness concreteness, Dnf dnf, DnfClause clause) {
		var lifter = new ClauseLifter(modality, concreteness, dnf, clause);
		return lifter.liftClause();
	}

	private record ModalDnf(Modality modality, Concreteness concreteness, Dnf dnf) {
		@Override
		public String toString() {
			return "%s %s %s".formatted(modality, concreteness, dnf.name());
		}
	}

	public static String decorateName(String name, Modality modality, Concreteness concreteness) {
		return "%s#%s#%s".formatted(name, modality, concreteness);
	}
}
