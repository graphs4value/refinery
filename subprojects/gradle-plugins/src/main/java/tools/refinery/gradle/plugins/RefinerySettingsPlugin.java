/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import tools.refinery.gradle.plugins.internal.RefineryPluginUtils;
import tools.refinery.gradle.plugins.internal.Versions;

import java.net.URI;
import java.net.URISyntaxException;

public class RefinerySettingsPlugin implements Plugin<Settings> {
	public static final String MAVEN_SNAPSHOTS = "https://refinery.tools/maven/snapshots/";

	@Override
	public void apply(@NotNull Settings target) {
		configureRepositories(target);
		createVersionCatalog(target);
		configureAllProjects(target);
	}

	private static void configureRepositories(Settings target) {
		var dependencyResolutionManagement = target.getDependencyResolutionManagement();
		// We depend on unstable API to configure dependency management for the version catalog.
		@SuppressWarnings("UnstableApiUsage")
		var repositories = dependencyResolutionManagement.getRepositories();
		var repository = target.getProviders()
				.gradleProperty("tools.refinery.repository")
				.map(Repository::valueOfIgnoreCase)
				.getOrElse(Repository.getDefault());
		switch (repository) {
		case LOCAL:
			repositories.mavenLocal();
			break;
		case SNAPSHOT:
			repositories.maven(mavenArtifactRepository -> {
				try {
					mavenArtifactRepository.setUrl(new URI(MAVEN_SNAPSHOTS));
				} catch (URISyntaxException e) {
					throw new IllegalStateException(e);
				}
				mavenArtifactRepository.setName("refinery-snapshots");
			});
			break;
		case CENTRAL: {
			// Since the central repository is common for all configurations, we handle it below.
		}
		break;
		default:
			throw new IllegalArgumentException("Unknown repository: " + repository);
		}
		repositories.mavenCentral();
	}

	private static void createVersionCatalog(Settings target) {
		var version = target.getProviders()
				.gradleProperty(RefineryPluginUtils.VERSION_PROPERTY)
				.getOrElse(Versions.REFINERY_VERSION);
		var dependencyResolutionManagement = target.getDependencyResolutionManagement();
		var versionCatalogs = dependencyResolutionManagement.getVersionCatalogs();
		var versionCatalog = versionCatalogs.create("refinery");
		versionCatalog.from("tools.refinery:refinery-versions:" + version);
	}

	private static void configureAllProjects(Settings target) {
		target.getGradle().allprojects(project -> project.getPlugins().withType(JavaPlugin.class, ignored -> {
			var autoApply = project.findProperty("tools.refinery.auto-apply");
			if (autoApply == null || !Boolean.FALSE.equals(Boolean.valueOf(autoApply.toString()))) {
				project.getPlugins().apply("tools.refinery.java");
			}
		}));
	}
}
