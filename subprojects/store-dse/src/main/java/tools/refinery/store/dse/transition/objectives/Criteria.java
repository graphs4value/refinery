/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.query.dnf.AnyQuery;

import java.util.Collection;
import java.util.List;

public final class Criteria {
	private Criteria() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static QueryCriterion whenHasMatch(AnyQuery query) {
		return new QueryCriterion(query, true);
	}

	public static QueryCriterion whenNoMatch(AnyQuery query) {
		return new QueryCriterion(query, false);
	}

	public static Criterion and(Criterion... criteria) {
		return and(List.of(criteria));
	}

	public static Criterion and(Collection<? extends Criterion> criteria) {
		if (criteria.size() == 1) {
			return criteria.iterator().next();
		}
		return new AndCriterion(criteria);
	}

	public static Criterion or(Criterion... criteria) {
		return or(List.of(criteria));
	}

	public static Criterion or(Collection<? extends Criterion> criteria) {
		if (criteria.size() == 1) {
			return criteria.iterator().next();
		}
		return new OrCriterion(criteria);
	}
}
