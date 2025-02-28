/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.gson;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tools.refinery.language.semantics.metadata.NodeKind;
import tools.refinery.language.semantics.metadata.PredicateDetailKind;
import tools.refinery.language.semantics.metadata.RelationDetail;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.xtext.servlet.LowercaseTypeAdapter;
import tools.refinery.language.web.xtext.servlet.RuntimeTypeAdapterFactory;

/**
 * Utility class containing global configuration for Gson.
 * <p>
 * Since this configuration only relates to our class hierarchy, it doesn't make sense to mock it.
 * Therefore, it has been implemented as a simple static final field holding a {@link Gson} instance.
 * </p>
 */
public class GsonUtil {
	private static final Gson GSON = new GsonBuilder()
			.disableJdkUnsafe()
			.setFormattingStyle(FormattingStyle.COMPACT)
			.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(RelationDetail.class, "type")
					.registerSubtype(RelationDetail.Class.class, "class")
					.registerSubtype(RelationDetail.Computed.class, "computed")
					.registerSubtype(RelationDetail.Reference.class, "reference")
					.registerSubtype(RelationDetail.Opposite.class, "opposite")
					.registerSubtype(RelationDetail.Predicate.class, "pred"))
			.registerTypeAdapter(NodeKind.class, new LowercaseTypeAdapter<>(NodeKind.class))
			.registerTypeAdapter(PredicateDetailKind.class, new LowercaseTypeAdapter<>(PredicateDetailKind.class))
			.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(RefineryResponse.class, "result")
					.recognizeSubtypes()
					.registerSubtype(RefineryResponse.Timeout.class, "timeout")
					.registerSubtype(RefineryResponse.Cancelled.class, "cancelled")
					.registerSubtype(RefineryResponse.RequestError.class, "requestError")
					.registerSubtype(RefineryResponse.ServerError.class, "serverError")
					.registerSubtype(RefineryResponse.InvalidProblem.class, "invalidProblem")
					.registerSubtype(RefineryResponse.Unsatisfiable.class, "unsatisfiable")
					.registerSubtype(RefineryResponse.Success.class, "success")
					.registerSubtype(RefineryResponse.Status.class, "status"))
			.create();

	private GsonUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Gson getGson() {
		return GSON;
	}
}
