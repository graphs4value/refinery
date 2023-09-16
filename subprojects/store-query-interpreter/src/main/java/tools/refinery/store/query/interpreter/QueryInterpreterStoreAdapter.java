/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.interpreter.api.InterpreterEngineOptions;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryStoreAdapter;

public interface QueryInterpreterStoreAdapter extends ModelQueryStoreAdapter {
	InterpreterEngineOptions getEngineOptions();

	@Override
	QueryInterpreterAdapter createModelAdapter(Model model);
}
