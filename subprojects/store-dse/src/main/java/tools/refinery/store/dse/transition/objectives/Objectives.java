/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.RelationalQuery;

import java.util.Collection;
import java.util.List;

public final class Objectives {
	private Objectives() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static CountObjective count(RelationalQuery query, double weight) {
		return new CountObjective(query, weight);
	}

	public static CountObjective count(RelationalQuery query) {
		return new CountObjective(query);
	}

	public static QueryObjective value(FunctionalQuery<? extends Number> query) {
		return new QueryObjective(query);
	}

	public static Objective sum(Objective... objectives) {
		return sum(List.of(objectives));
	}

	public static Objective sum(Collection<? extends Objective> objectives) {
		if (objectives.size() == 1) {
			return objectives.iterator().next();
		}
		return new CompositeObjective(objectives);
	}
}
