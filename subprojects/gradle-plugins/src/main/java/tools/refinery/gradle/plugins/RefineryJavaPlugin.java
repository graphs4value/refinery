/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.internal.tasks.JvmConstants;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;
import tools.refinery.gradle.plugins.internal.RefineryPluginUtils;
import tools.refinery.gradle.plugins.internal.Versions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class RefineryJavaPlugin implements Plugin<Project> {
	private static final String JUNIT_API = "org.junit.jupiter:junit-jupiter-api";
	private static final String JUNIT_BOM = "org.junit:junit-bom";
	private static final String JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine";
	private static final String JUNIT_LAUNCHER = "org.junit.platform:junit-platform-launcher";
	private static final String JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params";
	private static final String HAMCREST = "org.hamcrest:hamcrest";
	private static final String REFINERY_BOM = "tools.refinery:refinery-bom";
	private static final String SLF4J_LOG4J = "org.slf4j:log4j-over-slf4j";
	private static final String SLF4J_SIMPLE = "org.slf4j:slf4j-simple";

	@Override
	public void apply(@NotNull Project target) {
		target.getPluginManager().apply(JavaPlugin.class);
		configureJavaLanguageVersion(target);
		var extension = configureExtension(target);
		configureDependencies(target, extension);
		configureTasks(target, extension);
		RefineryPluginUtils.withShadowPlugin(target, RefineryJavaPlugin::configureShadowPlugin);
		target.afterEvaluate(RefineryJavaPlugin::configureAfterEvaluate);
	}

	private static void configureJavaLanguageVersion(Project target) {
		var javaExtension = target.getExtensions().getByType(JavaPluginExtension.class);
		javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(Versions.JAVA_LANGUAGE_VERSION));
	}

	private static RefineryJavaExtension configureExtension(Project target) {
		var extension = target.getExtensions().create("refineryJava", RefineryJavaExtension.class);
		extension.getAddBundleSymbolicName().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.add-bundle-symbolic-name")
				.map(Boolean::valueOf)
				.orElse(true));
		extension.getDistTar().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.dist-tar")
				.map(Boolean::valueOf)
				.orElse(target.provider(() -> target.getPlugins().hasPlugin(ApplicationPlugin.class) &&
						!RefineryPluginUtils.hasShadowPlugin(target))));
		extension.getDistZip().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.dist-zip")
				.map(Boolean::valueOf)
				.orElse(false));
		extension.getEnforcePlatform().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.enforce-platform")
				.map(Boolean::valueOf)
				.orElse(target.provider(() -> target.getPlugins().hasPlugin(ApplicationPlugin.class))));
		extension.getRefineryVersion().convention(target.getProviders()
				.gradleProperty(RefineryPluginUtils.VERSION_PROPERTY)
				.orElse(Versions.REFINERY_VERSION));
		extension.getTestDependencies().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.test-dependencies")
				.map(TestDependencies::valueOfIgnoreCase)
				.orElse(TestDependencies.FULL));
		extension.getUseSlf4JLog4J().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.use-slf4j-logj4")
				.map(Boolean::valueOf)
				.orElse(true));
		extension.getUseSlf4JSimple().convention(target.getProviders()
				.gradleProperty("tools.refinery.java.use-slf4j-simple")
				.map(Boolean::valueOf)
				.orElse(extension.getUseSlf4JLog4J()));
		return extension;
	}

	private static void configureDependencies(Project target, RefineryJavaExtension extension) {
		var dependencies = target.getDependencies();
		dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, target.provider(() -> {
			var artifact = REFINERY_BOM + ":" + extension.getRefineryVersion().get();
			return Boolean.TRUE.equals(extension.getEnforcePlatform().get()) ?
					dependencies.enforcedPlatform(artifact) : dependencies.platform(artifact);
		}));

		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
				SLF4J_LOG4J, extension.getUseSlf4JLog4J());
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
				SLF4J_LOG4J, extension.getUseSlf4JLog4J());
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
				SLF4J_LOG4J, extension.getUseSlf4JLog4J().map(value ->
						Boolean.TRUE.equals(value) && target.getPlugins().hasPlugin(ApplicationPlugin.class)));

		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME,
				SLF4J_SIMPLE, extension.getUseSlf4JSimple());
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME,
				SLF4J_SIMPLE, extension.getUseSlf4JSimple().map(value ->
						Boolean.TRUE.equals(value) && target.getPlugins().hasPlugin(ApplicationPlugin.class)));

		var addJUnit5 = extension.getTestDependencies().map(TestDependencies::isAddJUnit5);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
				JUNIT_API, addJUnit5);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
				dependencies.enforcedPlatform(JUNIT_BOM), addJUnit5);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME,
				JUNIT_ENGINE, addJUnit5);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME,
				JUNIT_LAUNCHER, addJUnit5);

		var addOtherTestDependencies = extension.getTestDependencies().map(TestDependencies::isAddOtherDependencies);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
				HAMCREST, addOtherTestDependencies);
		RefineryPluginUtils.addConditionalDependency(dependencies, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
				JUNIT_PARAMS, addOtherTestDependencies);
	}

	private static void configureTasks(Project target, RefineryJavaExtension extension) {
		var tasks = target.getTasks();
		tasks.withType(Tar.class, task -> {
			var name = task.getName();
			if (ApplicationPlugin.TASK_DIST_TAR_NAME.equals(name)) {
				task.setOnlyIf("Configured by refineryJava.distTar", ignored -> extension.getDistTar().get());
			} else if ("shadowDistTar".equals(name)) {
				task.setEnabled(false);
			}
		});
		tasks.withType(Zip.class, task -> {
			var name = task.getName();
			if (ApplicationPlugin.TASK_DIST_ZIP_NAME.equals(name)) {
				task.setOnlyIf("Configured by refineryJava.distZip", ignored -> extension.getDistZip().get());
			} else if ("shadowDistZip".equals(name)) {
				task.setEnabled(false);
			}
		});
	}

	private static void configureShadowPlugin(Project target) {
		target.getTasks().named("shadowJar", Jar.class, task -> {
			var shadowJarClass = task.getClass();
			Method appendMethod;
			try {
				appendMethod = shadowJarClass.getMethod("append", String.class);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("Failed to access ShadowJar task method", e);
			}
			try {
				// Silence Xtext warning.
				appendMethod.invoke(task, "plugin.properties");
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new IllegalStateException("Failed to add plugin.properties to the ShadowJar task", e);
			}
		});

		// See https://github.com/johnrengelman/shadow/issues/586
		// https://github.com/johnrengelman/shadow/issues/651
		target.getComponents()
				.named(JvmConstants.JAVA_MAIN_COMPONENT_NAME, AdhocComponentWithVariants.class, component -> {
					var configuration = target.getConfigurations().getByName("shadowRuntimeElements");
					component.withVariantsFromConfiguration(configuration, ConfigurationVariantDetails::skip);
				});
	}

	private static void configureAfterEvaluate(Project project) {
		var extension = project.getExtensions().getByType(RefineryJavaExtension.class);
		if (Boolean.TRUE.equals(extension.getAddBundleSymbolicName().get())) {
			addBundleSymbolicName(project);
		}
		if (Boolean.TRUE.equals(extension.getUseSlf4JLog4J().get())) {
			excludeLog4J(project);
		}
		if (!extension.getTestDependencies().get().isAddJUnit5()) {
			configureJunitPlatform(project);
		}
	}

	private static void addBundleSymbolicName(Project project) {
		var group = project.getGroup().toString();
		var name = project.getName();
		var symbolicName = "".equals(group) ? name : group + "." + name;
		project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, task -> {
			var attributes = task.getManifest().getAttributes();
			attributes.put("Bundle-SymbolicName", symbolicName);
			attributes.put("Bundle-Version", project.getVersion());
		});
	}

	private static void excludeLog4J(Project project) {
		project.getConfigurations().withType(Configuration.class, configuration -> {
			if (configuration.getName().endsWith("Classpath")) {
				configuration.exclude(Map.of("group", "log4j", "module", "log4j"));
				configuration.exclude(Map.of("group", "ch.qos.reload4j", "module", "reload4j"));
			}
		});
	}

	private static void configureJunitPlatform(Project project) {
		project.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class, Test::useJUnitPlatform);
	}
}
