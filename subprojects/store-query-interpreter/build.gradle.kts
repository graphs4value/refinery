/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Interpreted query backend for the model store"
}

dependencies {
	api(project(":refinery-interpreter"))
	api(project(":refinery-interpreter-localsearch"))
	api(project(":refinery-interpreter-rete"))
	api(project(":refinery-interpreter-rete-recipes"))
	api(project(":refinery-store-query"))
	implementation(libs.ecore)
	implementation(libs.slf4j.log4j)
	testImplementation(testFixtures(project(":refinery-logic")))
}

tasks {
	withType(Jar::class) {
		// Make sure we include external project notices.
		from(layout.projectDirectory.file("NOTICE.md"))
	}
}
