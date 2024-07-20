/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.java-test-fixtures")
}

mavenArtifact {
	description = "Library for model generation"
}

dependencies {
	api(project(":refinery-language-semantics"))
	implementation(project(":refinery-store-query-interpreter"))
	testFixturesImplementation(libs.junit.api)
	testImplementation(testFixtures(project(":refinery-language")))
}
