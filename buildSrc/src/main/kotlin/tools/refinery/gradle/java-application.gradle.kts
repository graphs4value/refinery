/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs
import tools.refinery.gradle.utils.JvmArgsUtils

plugins {
	application
	id("tools.refinery.gradle.java-conventions")
}

val libs = the<LibrariesForLibs>()

val distTarConfiguration = configurations.create("distTar") {
	isCanBeConsumed = true
	isCanBeResolved = false
}

dependencies {
	runtimeOnly(project(":refinery-logging"))
	implementation(libs.slf4j.log4j)
	implementation(enforcedPlatform(project(":refinery-bom-dependencies")))
}

application {
	applicationDefaultJvmArgs += JvmArgsUtils.JVM_ARGS
}

tasks.distZip {
	enabled = false
}

artifacts {
	add("distTar", layout.buildDirectory.file("distributions/${name}-${version}.tar")) {
		builtBy(tasks.distTar)
	}
}
