/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Partial model reasoner for the model store"
}

dependencies {
	api(project(":refinery-store-dse"))
	testImplementation(testFixtures(project(":refinery-logic")))
	testImplementation(project(":refinery-store-query-interpreter"))
}
