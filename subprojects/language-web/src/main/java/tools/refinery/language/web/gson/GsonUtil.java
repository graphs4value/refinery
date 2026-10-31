/*
 * SPDX-FileCopyrightText: 2024-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.gson;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.json.LowercaseTypeAdapterFactory;
import tools.refinery.generator.json.PartialModelJson;
import tools.refinery.generator.json.RuntimeTypeAdapterFactory;
import tools.refinery.language.web.api.dto.RefineryResponse;

/**
 * Utility class containing global configuration for Gson.
 * <p>
 * Since this configuration only relates to our class hierarchy, it doesn't make sense to mock it.
 * Therefore, it has been implemented as a simple static final field holding a {@link Gson} instance.
 * </p>
 */
public class GsonUtil {
	private static final Gson GSON;

	static {
		var builder = new GsonBuilder()
				.disableJdkUnsafe()
				.setFormattingStyle(FormattingStyle.COMPACT)
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
				.registerTypeAdapterFactory(new LowercaseTypeAdapterFactory<>(GeneratorResult.class));
		PartialModelJson.register(builder);
		GSON = builder.create();
	}

	private GsonUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Gson getGson() {
		return GSON;
	}
}
