/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	application
	id("com.github.johnrengelman.shadow")
	id("tools.refinery.gradle.internal.java-conventions")
}

// Use log4j-over-slf4j instead of log4j 1.x when running the application.
configurations.runtimeClasspath {
	exclude(group = "log4j", module = "log4j")
}

val libs = the<LibrariesForLibs>()

dependencies {
	implementation(libs.slf4j.simple)
	implementation(libs.slf4j.log4j)
}

for (taskName in listOf("distZip", "shadowDistTar", "shadowDistZip")) {
	tasks.named(taskName) {
		enabled = false
	}
}
