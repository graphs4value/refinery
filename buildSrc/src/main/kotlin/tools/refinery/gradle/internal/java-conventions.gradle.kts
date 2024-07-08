/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.internal

import org.gradle.accessors.dm.LibrariesForLibs
import tools.refinery.gradle.utils.EclipseUtils

plugins {
	jacoco
	java
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.maven-publish")
}

repositories {
	mavenCentral()
}

// Use log4j-over-slf4j instead of log4j 1.x in the tests.
configurations.testRuntimeClasspath {
	exclude(group = "log4j", module = "log4j")
}

val libs = the<LibrariesForLibs>()

dependencies {
	implementation(platform(project(":refinery-bom-dependencies")))
	compileOnly(libs.jetbrainsAnnotations)
	testCompileOnly(libs.jetbrainsAnnotations)
	testImplementation(libs.hamcrest)
	testImplementation(libs.junit.api)
	testImplementation(platform(libs.junit.bom))
	testRuntimeOnly(libs.junit.engine)
	testRuntimeOnly(libs.junit.launcher)
	testImplementation(libs.junit.params)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.junit)
	testImplementation(libs.slf4j.simple)
	testImplementation(libs.slf4j.log4j)
}

java {
	withJavadocJar()
	withSourcesJar()

	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

publishing.publications.named<MavenPublication>("mavenJava") {
	from(components["java"])
}

tasks {
	test {
		useJUnitPlatform {
			excludeTags("slow")
		}
		finalizedBy(tasks.jacocoTestReport)
	}

	jacocoTestReport {
		dependsOn(tasks.test)
		reports {
			xml.required.set(true)
		}
	}

	jar {
		manifest {
			attributes(
				"Bundle-SymbolicName" to "${project.group}.${project.name}", "Bundle-Version" to project.version
			)
		}
	}

	tasks.named<Jar>("sourcesJar") {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

	javadoc {
		options {
			this as StandardJavadocDocletOptions
			addBooleanOption("Xdoclint:none", true)
			quiet()
		}
	}

	val generateEclipseSourceFolders by tasks.registering

	register("prepareEclipse") {
		dependsOn(generateEclipseSourceFolders)
		dependsOn(tasks.named("eclipseJdt"))
	}

	eclipseClasspath {
		dependsOn(generateEclipseSourceFolders)
	}
}

fun collectDependentProjects(configuration: Configuration, dependentProjects: MutableCollection<Project>) {
	for (dependency in configuration.dependencies) {
		if (dependency is ProjectDependency) {
			val dependentProject = dependency.dependencyProject
			if (dependentProject.plugins.hasPlugin(JavaPlugin::class) && dependentProjects.add(dependentProject)) {
				collectDependentProjectsTransitively(dependentProject, dependentProjects)
			}
		}
	}
}

fun collectDependentProjectsTransitively(dependentProject: Project, dependentProjects: MutableCollection<Project>) {
	val apiConfiguration = dependentProject.configurations.findByName("api")
	if (apiConfiguration != null) {
		collectDependentProjects(apiConfiguration, dependentProjects)
	}
	collectDependentProjects(configurations.implementation.get(), dependentProjects)
}

gradle.projectsEvaluated {
	tasks.javadoc {
		val dependentProjects = HashSet<Project>()
		collectDependentProjectsTransitively(project, dependentProjects)
		val links = ArrayList<JavadocOfflineLink>()
		for (dependentProject in dependentProjects) {
			dependsOn(dependentProject.tasks.javadoc)
			val javadocDir = dependentProject.layout.buildDirectory.map { it.dir("docs/javadoc") }
			inputs.dir(javadocDir)
			links += JavadocOfflineLink("../${dependentProject.name}", javadocDir.get().asFile.path)
		}
		options {
			this as StandardJavadocDocletOptions
			linksOffline = (linksOffline ?: listOf()) + links
		}
	}
}

eclipse {
	EclipseUtils.patchClasspathEntries(this) { entry ->
		if (entry.path.endsWith("-gen")) {
			entry.entryAttributes["ignore_optional_problems"] = true
		}
		// If a project has a main dependency on a project and a test dependency on the testFixtures of a project,
		// it will be erroneously added as a test-only dependency to Eclipse. As a workaround, we add all project
		// dependencies as main dependencies (we do not deliberately use test-only project dependencies).
		if (entry is org.gradle.plugins.ide.eclipse.model.ProjectDependency) {
			entry.entryAttributes.remove("test")
		}
	}

	jdt.file.withProperties {
		// Allow @SuppressWarnings to suppress SonarLint warnings
		this["org.eclipse.jdt.core.compiler.problem.unhandledWarningToken"] = "ignore"
	}
}
