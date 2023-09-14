/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.AnyQuery;

public sealed interface AnyResultSet permits ResultSet {
	ModelQueryAdapter getAdapter();

	AnyQuery getCanonicalQuery();

	int size();
}
