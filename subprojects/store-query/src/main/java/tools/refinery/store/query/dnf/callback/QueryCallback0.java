/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf.callback;

import tools.refinery.store.query.dnf.QueryBuilder;

@FunctionalInterface
public interface QueryCallback0 {
	void accept(QueryBuilder builder);
}
