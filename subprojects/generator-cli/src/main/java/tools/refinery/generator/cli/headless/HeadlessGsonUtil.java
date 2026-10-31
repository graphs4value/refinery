/*
 * SPDX-FileCopyrightText: 2024-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.headless;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tools.refinery.generator.cli.output.OutputTheme;
import tools.refinery.generator.json.LowercaseTypeAdapter;

/**
 * Utility class containing global configuration for Gson when used for serializing IPC headers.
 * <p>
 * Since this configuration only relates to our class hierarchy, it doesn't make sense to mock it.
 * Therefore, it has been implemented as a simple static final field holding a {@link Gson} instance.
 * </p>
 */
public class HeadlessGsonUtil {
	private static final Gson GSON;

	static {
		var builder = new GsonBuilder()
				.disableJdkUnsafe()
				.setFormattingStyle(FormattingStyle.COMPACT)
				.registerTypeAdapter(GraphicalOutputFormat.class,
						new LowercaseTypeAdapter<>(GraphicalOutputFormat.class))
				.registerTypeAdapter(OutputTheme.class,
						new LowercaseTypeAdapter<>(OutputTheme.class));
		GSON = builder.create();
	}

	private HeadlessGsonUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Gson getGson() {
		return GSON;
	}
}
