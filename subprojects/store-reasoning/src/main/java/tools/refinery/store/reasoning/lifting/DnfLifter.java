/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		var lifter = new ClauseLifter(modality, concreteness, dnf, clause);
		return lifter.liftClause();
	}

	private record ModalDnf(Modality modality, Concreteness concreteness, Dnf dnf) {
		@Override
		public String toString() {
			return "%s %s %s".formatted(modality, concreteness, dnf.name());
		}
	}
}
