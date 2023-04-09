/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	implementation(libs.eclipseCollections)
	implementation(libs.eclipseCollections.api)
	api(project(":refinery-language"))
	api(project(":refinery-store"))
	testImplementation(testFixtures(project(":refinery-language")))
}
