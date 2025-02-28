/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	application
	id("tools.refinery.gradle.java-conventions")
}

val libs = the<LibrariesForLibs>()

dependencies {
	runtimeOnly(libs.logback.core)
	runtimeOnly(libs.logback.classic)
	implementation(libs.slf4j.log4j)
	implementation(enforcedPlatform(project(":refinery-bom-dependencies")))
}

tasks.distZip {
	enabled = false
}
