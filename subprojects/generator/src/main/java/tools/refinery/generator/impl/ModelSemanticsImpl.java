/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Provider;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ModelSemanticsImpl extends ModelFacadeImpl implements ModelSemantics {
	public ModelSemanticsImpl(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
							  Provider<MetadataCreator> metadataCreatorProvider) {
		super(problemTrace, store, modelSeed, metadataCreatorProvider);
	}

	@Override
	public Concreteness getConcreteness() {
		return Concreteness.PARTIAL;
	}

	@Override
	protected MetadataCreator getMetadataCreator() {
		var metadataCreator = super.getMetadataCreator();
		metadataCreator.setPreserveNewNodes(true);
		return metadataCreator;
	}

	@Override
	public Problem serialize() {
		return getProblemTrace().getProblem();
	}
}
