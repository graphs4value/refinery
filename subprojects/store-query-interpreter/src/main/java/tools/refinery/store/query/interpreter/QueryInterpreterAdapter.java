/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.internal.QueryInterpreterBuilderImpl;

public interface QueryInterpreterAdapter extends ModelQueryAdapter {
	@Override
	QueryInterpreterStoreAdapter getStoreAdapter();

	static QueryInterpreterBuilder builder() {
		return new QueryInterpreterBuilderImpl();
	}
}
