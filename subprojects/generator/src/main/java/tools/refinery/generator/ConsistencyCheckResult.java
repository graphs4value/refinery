/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
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
		var nodeNames = getNodeNames();
		for (var error : inconsistencies) {
			appendError(error, nodeNames, errorsBuilder);
		}
		return errorsBuilder.toString();
	}

	private IntObjectMap<String> getNodeNames() {
		var trace = facade.getProblemTrace();
		var nodeNames = IntObjectMaps.mutable.<String>empty();
		trace.getNodeTrace().forEachKeyValue((node, i) -> {
			var name = node.getName();
			if (name != null) {
				nodeNames.put(i, name);
			}
		});
		return nodeNames;
	}

	private static void appendError(AnyError error, IntObjectMap<String> nodeNames, StringBuilder errorsBuilder) {
		var symbol = error.partialSymbol();
		var key = error.tuple();
		errorsBuilder.append('\t').append(symbol.name()).append("(");
		int arity = key.getSize();
		for (int i = 0; i < arity; i++) {
			if (i > 0) {
				errorsBuilder.append(", ");
			}
			int nodeId = key.get(i);
			var name = nodeNames.get(nodeId);
			if (name == null) {
				errorsBuilder.append("::").append(i);
			} else {
				errorsBuilder.append(name);
			}
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
