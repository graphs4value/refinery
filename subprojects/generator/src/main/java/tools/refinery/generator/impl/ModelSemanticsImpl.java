/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.reasoning.literal.Concreteness;

public class ModelSemanticsImpl extends ModelFacadeImpl implements ModelSemantics {
	public ModelSemanticsImpl(Args args) {
		super(args);
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
