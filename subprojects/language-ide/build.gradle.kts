/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.xtext-generated")
}

dependencies {
	api(project(":refinery-language"))
	api(libs.xtext.ide)
	api(libs.xtext.xbase.ide)
}

val generateXtextLanguage by project(":refinery-language").tasks.existing

for (taskName in listOf("compileJava", "processResources")) {
	tasks.named(taskName) {
		dependsOn(generateXtextLanguage)
	}
}
