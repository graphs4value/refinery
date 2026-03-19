/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs
import tools.refinery.gradle.utils.JvmArgsUtils

plugins {
	id("tools.refinery.gradle.internal.java-basic-conventions")
}

val libs = the<LibrariesForLibs>()

val mockitoAgent = configurations.create("mockitoAgent") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	testImplementation(libs.hamcrest)
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testImplementation(libs.junit.params)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.junit)
	mockitoAgent(libs.mockito.core) {
		isTransitive = false
	}
}

configurations.forEach {
	// See https://github.com/google/guice/issues/1926#issuecomment-3991531593
	it.resolutionStrategy.dependencySubstitution {
		substitute(module("com.google.inject:guice"))
			.using(module("com.google.inject:guice:${libs.versions.guice.get()}"))
			.withClassifier("classes")
			.because("Use Java 25 compatible ASM with Guice instead of the bundled version")
	}
}

tasks.withType(JavaExec::class) {
	jvmArgs(JvmArgsUtils.JVM_ARGS)
}

tasks.test {
	jvmArgs(JvmArgsUtils.JVM_ARGS)
	// See
	// https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3
	jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
