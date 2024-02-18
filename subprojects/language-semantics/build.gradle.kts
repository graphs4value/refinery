/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	api(libs.eclipseCollections.api)
	api(project(":refinery-language"))
	api(project(":refinery-store"))
	api(project(":refinery-store-query"))
	api(project(":refinery-store-reasoning"))
	implementation(project(":refinery-store-reasoning-scope"))
	runtimeOnly(libs.eclipseCollections)
	testImplementation(project(":refinery-store-dse-visualization"))
	testImplementation(project(":refinery-store-query-interpreter"))
	testImplementation(testFixtures(project(":refinery-language")))
}
