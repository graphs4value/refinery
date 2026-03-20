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

public final class RefineryPluginUtils {
	public static final String VERSION_PROPERTY = "tools.refinery.version";
	public static final String SHADOW_JAR_TASK = "com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar";

	private static final String SHADOW_PLUGIN_ID = "com.gradleup.shadow";

	private RefineryPluginUtils() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly.");
	}

	public static void withShadowPlugin(Project project, Action<? super Project> action) {
		// Method parameter in Gradle API uses raw type.
		@SuppressWarnings("rawtypes")
		Action<? super Plugin> pluginAction = ignored -> action.execute(project);
		var plugins = project.getPlugins();
		plugins.withId(SHADOW_PLUGIN_ID, pluginAction);
	}

	public static boolean hasShadowPlugin(Project project) {
		var plugins = project.getPlugins();
		return plugins.hasPlugin(SHADOW_PLUGIN_ID);
	}

	public static void addConditionalDependency(DependencyHandler dependencies, String configuration,
                                                Object dependency, Provider<Boolean> condition) {
		var provider = condition.map(value -> value ? dependency : null);
		dependencies.add(configuration, provider);
	}
}
