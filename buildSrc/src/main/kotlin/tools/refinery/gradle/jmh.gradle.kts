/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs
import tools.refinery.gradle.utils.EclipseUtils
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.internal.java-conventions")
	id("tools.refinery.gradle.sonarqube")
}

val sourceSets = the<SourceSetContainer>()

val jmh: SourceSet by sourceSets.creating {
	compileClasspath += sourceSets.main.get().output
	runtimeClasspath += sourceSets.main.get().output
	// Allow using test classes in benchmarks for now.
	compileClasspath += sourceSets.test.get().output
	runtimeClasspath += sourceSets.test.get().output
}

val jmhImplementation: Configuration by configurations.getting {
	extendsFrom(configurations.implementation.get(), configurations.testImplementation.get())
}

val jmhAnnotationProcessor: Configuration by configurations.getting

configurations["jmhRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get(), configurations.testRuntimeOnly.get())

val libs = the<LibrariesForLibs>()

dependencies {
	jmhImplementation(libs.jmh.core)
	jmhAnnotationProcessor(libs.jmh.annprocess)
}

tasks.register<JavaExec>("jmh") {
	dependsOn(tasks.named("jmhClasses"))
	mainClass.set("org.openjdk.jmh.Main")
	classpath = jmh.runtimeClasspath
}

EclipseUtils.patchClasspathEntries(eclipse) { entry ->
	// Workaround from https://github.com/gradle/gradle/issues/4802#issuecomment-407902081
	if (entry.entryAttributes["gradle_scope"] == "jmh") {
		// Allow test helper classes to be used in benchmarks from Eclipse
		// and do not expose JMH dependencies to the main source code.
		entry.entryAttributes["test"] = true
	} else {
		EclipseUtils.patchGradleUsedByScope(entry) { usedBy ->
			if (listOf("main", "test", "testFixtures").any { e -> usedBy.contains(e) }) {
				// main and test sources are also used by jmh sources.
				usedBy += "jmh"
			}
		}
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.tests", "src/jmh/java")
}
