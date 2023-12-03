/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.internal

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.plugins.ide.eclipse.model.ProjectDependency
import tools.refinery.gradle.utils.EclipseUtils

plugins {
    jacoco
    java
	`maven-publish`
	id("tools.refinery.gradle.eclipse")
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
	compileOnly(libs.jetbrainsAnnotations)
	testCompileOnly(libs.jetbrainsAnnotations)
	testImplementation(libs.hamcrest)
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
					"Bundle-SymbolicName" to "${project.group}.${project.name}",
					"Bundle-Version" to project.version
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

publishing.publications {
	create<MavenPublication>("mavenJava") {
		from(components["java"])
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
		if (entry is ProjectDependency) {
			entry.entryAttributes.remove("test")
		}
	}

	jdt.file.withProperties {
		// Allow @SuppressWarnings to suppress SonarLint warnings
		this["org.eclipse.jdt.core.compiler.problem.unhandledWarningToken"] = "ignore"
	}
}
