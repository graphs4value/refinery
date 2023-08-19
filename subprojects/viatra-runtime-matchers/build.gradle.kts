/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	implementation(libs.slf4j.log4j)
	implementation(libs.eclipseCollections.api)
	implementation(libs.eclipseCollections)
}
