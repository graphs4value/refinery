/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-application")
}

dependencies {
	implementation(project(":refinery-generator"))
	implementation(libs.jcommander)
	implementation(libs.slf4j)
}

application {
	mainClass.set("tools.refinery.generator.cli.RefineryCli")
}
