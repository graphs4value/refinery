/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	api(project(":refinery-viatra-runtime-base"))
	api(libs.ecore)
	implementation(libs.eclipse)
	implementation(libs.emf)
	implementation(libs.slf4j.log4j)
}
