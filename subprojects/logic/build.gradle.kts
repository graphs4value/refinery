/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.java-test-fixtures")
}

mavenArtifact {
	description = "Logic expressions"
}

dependencies {
	implementation(libs.bigmath)
	testFixturesApi(libs.hamcrest)
}
