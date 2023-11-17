/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.internal.java-conventions")
	id("tools.refinery.gradle.sonarqube")
}

val xtextGenPath = "src/main/xtext-gen"

val xtextGenerated: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

sourceSets.main {
	java.srcDir(xtextGenPath)
	resources.srcDir(xtextGenPath)
}

tasks {
	// Based on the idea from https://stackoverflow.com/a/57788355 to safely consume generated sources in sibling
	// projects.
	val syncXtextGeneratedSources by tasks.creating(Sync::class) {
		from(xtextGenerated)
		into(xtextGenPath)
	}

	for (taskName in listOf("compileJava", "processResources", "generateEclipseSourceFolders", "sourcesJar")) {
		tasks.named(taskName) {
			dependsOn(syncXtextGeneratedSources)
		}
	}

	clean {
		delete(xtextGenPath)
	}
}


sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "$xtextGenPath/**")
}
