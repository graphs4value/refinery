/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.Modality;

public class ReasoningBuilderImpl extends AbstractModelAdapterBuilder<ReasoningStoreAdapterImpl>
		implements ReasoningBuilder {
	@Override
	public ReasoningBuilder liftedQuery(Dnf liftedQuery) {
		return null;
	}

	@Override
	public Dnf lift(Modality modality, Dnf query) {
		checkNotConfigured();
		return null;
	}

	@Override
	public ReasoningStoreAdapterImpl doBuild(ModelStore store) {
		return null;
	}
}
