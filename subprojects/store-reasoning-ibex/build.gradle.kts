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
	description = "IBEX reasoner for the model store"
}

dependencies {
	api(project(":refinery-store-reasoning"))
	implementation(libs.eclipseCollections)
	implementation(libs.refinery.ibex)
	runtimeOnly(libs.eclipseCollections.impl)
	testImplementation(project(":refinery-store-query-interpreter"))
	testImplementation(project(":refinery-store-reasoning-scope"))
}
