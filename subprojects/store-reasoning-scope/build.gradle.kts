/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Multiplicity reasoner for the model store"
}

dependencies {
	api(project(":refinery-store-reasoning"))
	implementation(libs.eclipseCollections)
	implementation(libs.ortools)
	runtimeOnly(libs.eclipseCollections.impl)
	testImplementation(project(":refinery-store-query-interpreter"))
}
