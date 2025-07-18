/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import tools.refinery.generator.ModelFacade;
import tools.refinery.language.semantics.SemanticsUtils;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.TreeMap;
import java.util.function.Function;

@Singleton
public class PartialInterpretation2Json {
	private static final String UNKNOWN_STRING = "unknown";
	private static final String ERROR_STRING = "error";

	@Inject
	private SemanticsUtils semanticsUtils;

	public JsonObject getPartialInterpretation(ModelFacade facade, CancellationToken cancellationToken) {
		var model = facade.getModel();
		var json = new JsonObject();
		for (var entry : facade.getProblemTrace().getRelationTrace().entrySet()) {
			var relation = entry.getKey();
			var partialSymbol = entry.getValue();
			var tuples = getTuplesJson(facade, partialSymbol);
			var name = semanticsUtils.getNameWithoutRootPrefix(relation).orElse(partialSymbol.name());
			json.add(name, tuples);
			cancellationToken.checkCancelled();
		}
		return json;
	}

	private static JsonArray getTuplesJson(ModelFacade facade, AnyPartialSymbol partialSymbol) {
		var interpretation = facade.getPartialInterpretation(partialSymbol);
		return getTuplesJson((PartialInterpretation<?, ?>) interpretation);
	}

	private static <A extends AbstractValue<A, C>, C> JsonArray getTuplesJson(
			PartialInterpretation<A, C> interpretation) {
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);
	}

	private static <A extends AbstractValue<A, C>, C> JsonArray getTuplesJson(Cursor<Tuple, A> cursor) {
		return getTuplesJson(cursor, Function.identity());
	}

	private static <A extends AbstractValue<A, C>, C> JsonArray getTuplesJson(
			Cursor<Tuple, A> cursor, Function<A, A> transform) {
		var map = new TreeMap<Tuple, A>();
		while (cursor.move()) {
			map.put(cursor.getKey(), transform.apply(cursor.getValue()));
		}
		var tuples = new JsonArray();
		for (var entry : map.entrySet()) {
			tuples.add(toArray(entry.getKey(), entry.getValue()));
		}
		return tuples;
	}

	// We deliberately use {@code ==} to check for the equality of interned strings.
	@SuppressWarnings("StringEquality")
	private static <A extends AbstractValue<A, C>, C> JsonArray toArray(Tuple tuple, A value) {
		int arity = tuple.getSize();
		var json = new JsonArray(arity + 1);
		for (int i = 0; i < arity; i++) {
			json.add(tuple.get(i));
		}
		var stringValue = value.toString();
		if (stringValue == UNKNOWN_STRING || stringValue == ERROR_STRING || value.isConcrete()) {
			json.add(stringValue);
		} else if (value.isError()) {
			var jsonObject = new JsonObject();
			jsonObject.addProperty(ERROR_STRING, stringValue);
			json.add(jsonObject);
		} else {
			var jsonObject = new JsonObject();
			jsonObject.addProperty(UNKNOWN_STRING, stringValue);
			json.add(jsonObject);
		}
		return json;
	}
}
