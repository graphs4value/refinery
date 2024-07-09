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

// Use log4j-over-slf4j instead of log4j 1.x when running the application.
configurations.runtimeClasspath {
	exclude(group = "log4j", module = "log4j")
}

val libs = the<LibrariesForLibs>()

dependencies {
	runtimeOnly(libs.slf4j.simple)
	implementation(libs.slf4j.log4j)
	implementation(enforcedPlatform(project(":refinery-bom-dependencies")))
}

tasks.distZip {
	enabled = false
}
