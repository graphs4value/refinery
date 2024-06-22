/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.xtext-generated")
}

mavenArtifact {
	name = "Language IDE"
	description = "IDE support for the partial modeling language"
}

dependencies {
	api(project(":refinery-language"))
	api(libs.xtext.ide)
	api(libs.xtext.xbase.ide)
	xtextGenerated(project(":refinery-language", "generatedIdeSources"))
}

