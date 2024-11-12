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
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Singleton
public class PartialInterpretation2Json {
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
		json.add("builtin::count", getCountJson(model, facade.getConcreteness()));
		return json;
	}

	private static JsonArray getTuplesJson(ModelFacade facade, PartialRelation partialSymbol) {
		var interpretation = facade.getPartialInterpretation(partialSymbol);
		var cursor = interpretation.getAll();
		return getTuplesJson(cursor);
	}

	private static JsonArray getTuplesJson(Cursor<Tuple, ?> cursor) {
		return getTuplesJson(cursor, Function.identity());
	}

	private static <T, R> JsonArray getTuplesJson(Cursor<Tuple, T> cursor, Function<T, R> transform) {
		var map = new TreeMap<Tuple, Object>();
		while (cursor.move()) {
			map.put(cursor.getKey(), transform.apply(cursor.getValue()));
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

	private static JsonArray getCountJson(Model model, Concreteness concreteness) {
		var interpretation = model.getInterpretation(MultiObjectTranslator.COUNT_STORAGE);
		var cursor = interpretation.getAll();
		UnaryOperator<CardinalityInterval> transform = switch (concreteness) {
			case PARTIAL -> UnaryOperator.identity();
			case CANDIDATE -> count -> count.equals(CardinalityIntervals.ONE) ? count :
					count.meet(CardinalityIntervals.NONE);
		};
		return getTuplesJson(cursor, transform);

	}
}
