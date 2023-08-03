/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;

public interface StateCoderStoreAdapter extends ModelStoreAdapter {
	StateEquivalenceChecker.EquivalenceResult checkEquivalence(Version v1, Version v2);

	@Override
	StateCoderAdapter createModelAdapter(Model model);
}
