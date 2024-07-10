/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins.internal;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.provider.Provider;

import java.util.List;

public final class RefineryPluginUtils {
	public static final String VERSION_PROPERTY = "tools.refinery.version";

	private static final List<String> SHADOW_PLUGIN_IDS = List.of(
			"com.github.johnrengelman.shadow",
			"io.github.goooler.shadow"
	);

	private RefineryPluginUtils() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly.");
	}

	public static void withShadowPlugin(Project project, Action<? super Project> action) {
		// Method parameter in Gradle API uses raw type.
		@SuppressWarnings("rawtypes")
		Action<? super Plugin> pluginAction = ignored -> action.execute(project);
		var plugins = project.getPlugins();
		for (var pluginId : SHADOW_PLUGIN_IDS) {
			plugins.withId(pluginId, pluginAction);
		}
	}

	public static boolean hasShadowPlugin(Project project) {
		var plugins = project.getPlugins();
		for (var pluginId : SHADOW_PLUGIN_IDS) {
			if (plugins.hasPlugin(pluginId)) {
				return true;
			}
		}
		return false;
	}

	public static void addConditionalDependency(DependencyHandler dependencies, String configuration,
                                                Object dependency, Provider<Boolean> condition) {
		var provider = condition.map(value -> Boolean.TRUE.equals(value) ? dependency : null);
		dependencies.add(configuration, provider);
	}
}
