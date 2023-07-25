/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	implementation(libs.ecore)
	implementation ("guru.nidi:graphviz-java:0.18.1")
	api(libs.viatra)
	api(project(":refinery-store-query"))
	api(project(":refinery-store-reasoning"))
	api(project(":refinery-visualization"))
}
