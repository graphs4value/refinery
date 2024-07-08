/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

plugins {
	`java-platform`
	id("tools.refinery.gradle.maven-publish")
}

javaPlatform {
	allowDependencies()
}

publishing.publications.named<MavenPublication>("mavenJava") {
	from(components["javaPlatform"])
}
