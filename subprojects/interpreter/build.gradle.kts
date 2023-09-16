/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.interpreter-library")
}

dependencies {
	implementation(libs.slf4j.log4j)
	// Code in this subproject inherits from Eclipse Collection implementation classes, so this can't be `runtimeOnly`.
	implementation(libs.eclipseCollections)
	implementation(libs.eclipseCollections.api)
}
