/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Store Reasoning SMT"
	description = "SMT reasoner for the model store"
}

dependencies {
	api(project(":refinery-store-reasoning"))
	implementation(libs.refinery.z3)
	testImplementation(project(":refinery-store-query-interpreter"))
}
