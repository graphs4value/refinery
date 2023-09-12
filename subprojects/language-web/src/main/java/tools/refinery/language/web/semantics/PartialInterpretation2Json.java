/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.semantics.model.SemanticsUtils;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.TreeMap;

@Singleton
public class PartialInterpretation2Json {
	@Inject
	private SemanticsUtils semanticsUtils;

	public JsonObject getPartialInterpretation(ModelInitializer initializer, Model model, Concreteness concreteness,
											   CancellationToken cancellationToken) {
		var adapter = model.getAdapter(ReasoningAdapter.class);
		var json = new JsonObject();
		for (var entry : initializer.getRelationTrace().entrySet()) {
			var relation = entry.getKey();
			var partialSymbol = entry.getValue();
			var tuples = getTuplesJson(adapter, concreteness, partialSymbol);
			var name = semanticsUtils.getName(relation).orElse(partialSymbol.name());
			json.add(name, tuples);
			cancellationToken.checkCancelled();
		}
		json.add("builtin::count", getCountJson(model));
		return json;
	}

	private static JsonArray getTuplesJson(ReasoningAdapter adapter, Concreteness concreteness,
										   PartialRelation partialSymbol) {
		var interpretation = adapter.getPartialInterpretation(concreteness, partialSymbol);
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);
	}

	private static JsonArray getTuplesJson(Cursor<Tuple, ?> cursor) {
		var map = new TreeMap<Tuple, Object>();
		while (cursor.move()) {
			map.put(cursor.getKey(), cursor.getValue());
		}
		var tuples = new JsonArray();
		for (var entry : map.entrySet()) {
			tuples.add(toArray(entry.getKey(), entry.getValue()));
		}
		return tuples;
	}

	private static JsonArray toArray(Tuple tuple, Object value) {
		int arity = tuple.getSize();
		var json = new JsonArray(arity + 1);
		for (int i = 0; i < arity; i++) {
			json.add(tuple.get(i));
		}
		json.add(value.toString());
		return json;
	}

	private static JsonArray getCountJson(Model model) {
		var interpretation = model.getInterpretation(MultiObjectTranslator.COUNT_STORAGE);
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);

	}
}
