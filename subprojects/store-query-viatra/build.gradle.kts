/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	api(project(":refinery-viatra-runtime"))
	api(project(":refinery-viatra-runtime-localsearch"))
	api(project(":refinery-viatra-runtime-rete"))
	api(project(":refinery-viatra-runtime-rete-recipes"))
	api(project(":refinery-store-query"))
	implementation(libs.ecore)
	implementation(libs.slf4j.log4j)
}
