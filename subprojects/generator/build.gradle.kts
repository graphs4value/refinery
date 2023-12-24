/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	api(project(":refinery-language-semantics"))
	api(libs.eclipseCollections.api)
	implementation(project(":refinery-store-query-interpreter"))
	testImplementation(testFixtures(project(":refinery-language")))
}
