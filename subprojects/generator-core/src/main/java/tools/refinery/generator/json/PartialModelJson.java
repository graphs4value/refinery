/*
 * SPDX-FileCopyrightText: 2025-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.json;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import tools.refinery.language.semantics.metadata.*;
import tools.refinery.language.utils.Visibility;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.List;

public record PartialModelJson(Concreteness concreteness, List<NodeMetadata> nodes, List<RelationMetadata> relations,
                               JsonObject partialInterpretation) {

	/**
	 * Register the classes needed to serialize a {@link PartialModelJson} to a {@link GsonBuilder}.
	 *
	 * @param builder The builder to configure.
	 */
	public static void register(GsonBuilder builder) {
		builder
				.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(RelationDetail.class, "type")
						.registerSubtype(RelationDetail.Class.class, "class")
						.registerSubtype(RelationDetail.Computed.class, "computed")
						.registerSubtype(RelationDetail.Reference.class, "reference")
						.registerSubtype(RelationDetail.Attribute.class, "attribute")
						.registerSubtype(RelationDetail.Opposite.class, "opposite")
						.registerSubtype(RelationDetail.Predicate.class, "pred")
						.registerSubtype(RelationDetail.Function.class, "function")
						.registerSubtype(RelationDetail.Domain.class, "domain"))
				.registerTypeAdapter(Concreteness.class, new LowercaseTypeAdapter<>(Concreteness.class))
				.registerTypeAdapter(NodeKind.class, new LowercaseTypeAdapter<>(NodeKind.class))
				.registerTypeAdapter(PredicateDetailKind.class, new LowercaseTypeAdapter<>(PredicateDetailKind.class))
				.registerTypeAdapter(FunctionDetailKind.class, new LowercaseTypeAdapter<>(FunctionDetailKind.class))
				.registerTypeAdapter(Visibility.class, new LowercaseTypeAdapter<>(Visibility.class));
	}
}
