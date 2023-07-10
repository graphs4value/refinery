/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;

@FunctionalInterface
public interface DnfRewriter {
	Dnf rewrite(Dnf dnf);

	default AnyQuery rewrite(AnyQuery query) {
		return rewrite((Query<?>) query);
	}

	default <T> Query<T> rewrite(Query<T> query) {
		var rewrittenDnf = rewrite(query.getDnf());
		return query.withDnf(rewrittenDnf);
	}
}
