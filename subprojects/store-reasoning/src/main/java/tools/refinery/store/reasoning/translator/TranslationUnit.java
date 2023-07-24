/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningBuilder;

import java.util.Collection;

public abstract class TranslationUnit {
	private ReasoningBuilder reasoningBuilder;

	protected ReasoningBuilder getReasoningBuilder() {
		return reasoningBuilder;
	}

	public void setPartialInterpretationBuilder(ReasoningBuilder reasoningBuilder) {
		this.reasoningBuilder = reasoningBuilder;
		configureReasoningBuilder();
	}

	protected void configureReasoningBuilder() {
		// Nothing to configure by default.
	}

	public abstract Collection<TranslatedRelation> getTranslatedRelations();

	public abstract void initializeModel(Model model, int nodeCount);
}
