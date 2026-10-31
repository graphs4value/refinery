/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.json;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tools.refinery.generator.ModelFacade;
import tools.refinery.store.util.CancellationToken;

@Singleton
public class PartialModel2Json {
	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	public PartialModelJson savePartialInterpretation(ModelFacade facade) {
		return savePartialInterpretation(facade, facade.getModel().getCancellationToken());
	}

	public PartialModelJson savePartialInterpretation(ModelFacade facade, CancellationToken cancellationToken) {
		cancellationToken.checkCancelled();
		var nodes = facade.getNodesMetadata().list();
		cancellationToken.checkCancelled();
		var relations = facade.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(facade, cancellationToken);
		return new PartialModelJson(facade.getConcreteness(), nodes, relations, partialInterpretation);
	}
}
