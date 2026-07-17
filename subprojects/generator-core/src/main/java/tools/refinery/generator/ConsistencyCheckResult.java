/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.language.semantics.metadata.NodesMetadata;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public record ConsistencyCheckResult(ModelFacade facade, List<AnyError> inconsistencies) {
	public boolean isConsistent() {
		return inconsistencies.isEmpty();
	}

	public void throwIfInconsistent() {
		if (!isConsistent()) {
			throw new IllegalStateException(formatMessage());
		}
	}

	public String formatMessage() {
		if (isConsistent()) {
			return "Model is consistent";
		}
		var errorsBuilder = new StringBuilder("Inconsistencies found in model:\n\n");
		var nodesMetadata = facade.getNodesMetadata();
		for (var error : inconsistencies) {
			appendError(error, nodesMetadata, errorsBuilder);
		}
		return errorsBuilder.toString();
	}

	private static void appendError(AnyError error, NodesMetadata nodesMetadata, StringBuilder errorsBuilder) {
		var symbol = error.partialSymbol();
		var key = error.tuple();
		errorsBuilder.append('\t').append(symbol.name()).append("(");
		int arity = key.getSize();
		for (int i = 0; i < arity; i++) {
			if (i > 0) {
				errorsBuilder.append(", ");
			}
			int nodeId = key.get(i);
			errorsBuilder.append(nodesMetadata.getSimpleName(nodeId));
		}
		errorsBuilder.append("): error.\n");
	}

	public sealed interface AnyError {
		AnyPartialSymbol partialSymbol();

		Tuple tuple();
	}

	public record Error<A extends AbstractValue<A, C>, C>(PartialSymbol<A, C> partialSymbol, Tuple tuple,
														  A value) implements AnyError {
	}
}
