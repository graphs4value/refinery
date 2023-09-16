/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	api(project(":refinery-store-query"))
	implementation(project(":refinery-store-dse-visualization"))
	implementation(libs.eclipseCollections.api)
	runtimeOnly(libs.eclipseCollections)
	testImplementation(project(":refinery-store-query-interpreter"))
}
