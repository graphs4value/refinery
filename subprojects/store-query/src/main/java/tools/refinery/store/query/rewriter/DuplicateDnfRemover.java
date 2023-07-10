/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateDnfRemover extends AbstractRecursiveRewriter {
	private final Map<CanonicalDnf, Dnf> dnfCache = new HashMap<>();
	private final Map<Dnf, Query<?>> queryCache = new HashMap<>();

	@Override
	protected Dnf map(Dnf dnf) {
		var result = super.map(dnf);
		return dnfCache.computeIfAbsent(new CanonicalDnf(result), CanonicalDnf::getDnf);
	}

	@Override
	protected Dnf doRewrite(Dnf dnf) {
		var builder = Dnf.builderFrom(dnf);
		for (var clause : dnf.getClauses()) {
			builder.clause(rewriteClause(clause));
		}
		return builder.build();
	}

	private List<Literal> rewriteClause(DnfClause clause) {
		var originalLiterals = clause.literals();
		var literals = new ArrayList<Literal>(originalLiterals.size());
		for (var literal : originalLiterals) {
			var rewrittenLiteral = literal;
			if (literal instanceof AbstractCallLiteral abstractCallLiteral &&
					abstractCallLiteral.getTarget() instanceof Dnf targetDnf) {
				var rewrittenTarget = rewrite(targetDnf);
				rewrittenLiteral = abstractCallLiteral.withTarget(rewrittenTarget);
			}
			literals.add(rewrittenLiteral);
		}
		return literals;
	}

	@Override
	public <T> Query<T> rewrite(Query<T> query) {
		var rewrittenDnf = rewrite(query.getDnf());
		// {@code withDnf} will always return the appropriate type.
		@SuppressWarnings("unchecked")
		var rewrittenQuery = (Query<T>) queryCache.computeIfAbsent(rewrittenDnf, query::withDnf);
		return rewrittenQuery;
	}

	private static class CanonicalDnf {
		private final Dnf dnf;
		private final int hash;

		public CanonicalDnf(Dnf dnf) {
			this.dnf = dnf;
			hash = dnf.hashCodeWithSubstitution();
		}

		public Dnf getDnf() {
			return dnf;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			var otherCanonicalDnf = (CanonicalDnf) obj;
			return dnf.equalsWithSubstitution(DnfEqualityChecker.DEFAULT, otherCanonicalDnf.dnf);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return dnf.name();
		}
	}
}
