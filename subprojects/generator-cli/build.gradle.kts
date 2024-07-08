/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-application")
}

dependencies {
	implementation(project(":refinery-generator"))
	implementation(libs.jcommander)
	implementation(libs.slf4j.api)
}

application {
	mainClass.set("tools.refinery.generator.cli.RefineryCli")
}

tasks.shadowJar {
	// Silence Xtext warning.
	append("plugin.properties")
}
