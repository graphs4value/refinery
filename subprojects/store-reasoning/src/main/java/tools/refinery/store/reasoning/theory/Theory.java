/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

import tools.refinery.store.model.ModelStoreBuilder;

import java.util.Collection;

public interface Theory {
	TheorySupport checkSupport(TheoryRule theoryRule);

	void createPropagator(ModelStoreBuilder storeBuilder, Collection<TheoryRule> collectedRules);
}
