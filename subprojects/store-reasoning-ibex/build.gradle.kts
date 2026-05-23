/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Store Reasoning IBEX"
	description = "IBEX interval constraint propagator for the model store"
}

dependencies {
	api(project(":refinery-store-reasoning"))
	implementation(libs.eclipseCollections)
	// Local IBEX JAR — the native library (libibex-java.so) must be on java.library.path at runtime.
	// Set -Djava.library.path=/home/kmono/ibex-lib/build/src/java when launching the JVM.
	implementation(files("/home/kmono/ibex-lib/build/src/java/ibex.jar"))
	runtimeOnly(libs.eclipseCollections.impl)
	testImplementation(project(":refinery-store-query-interpreter"))
	testImplementation(project(":refinery-store-reasoning-scope"))
}

tasks.test {
	jvmArgs("-Djava.library.path=/home/kmono/ibex-lib/build/src/java")
}