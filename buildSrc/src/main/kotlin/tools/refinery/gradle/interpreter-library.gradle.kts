/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("maven-publish")
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.sonarqube")
}

property("tools.refinery.interpreter.group")?.let { group = it }
property("tools.refinery.interpreter.version")?.let { version = it }

tasks {
	withType(Jar::class) {
		// Make sure we include external project notices.
		from(layout.projectDirectory.file("about.html"))
		from(layout.projectDirectory.file("NOTICE.md"))
	}
}

sonarqube.properties {
	// Code copied from the VIATRA project is maintained by the VIATRA contributors.
	// Our own modifications are verified by tests in our own subprojects.
	// Therefore, we disable coverage checking for vendor subprojects.
	SonarPropertiesUtils.addToList(properties, "sonar.coverage.exclusions", "src/main/**")
}
