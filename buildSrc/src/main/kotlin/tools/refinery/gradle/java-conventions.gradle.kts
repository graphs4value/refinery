/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	id("tools.refinery.gradle.internal.java-basic-conventions")
}

val libs = the<LibrariesForLibs>()

dependencies {
	testImplementation(libs.hamcrest)
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testImplementation(libs.junit.params)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.junit)
}
