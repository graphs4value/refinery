/*
 * SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

val productionAssets = configurations.create("productionAssets") {
	isCanBeConsumed = true
	isCanBeResolved = false
}

frontend {
	checkScript.set(if (project.hasProperty("ci")) "run test:run:ci" else "run test:run")
}

val distDir = layout.projectDirectory.dir("dist")

val assembleConfigFiles: FileCollection = files("vite.config.ts")

val configFiles: FileCollection = assembleConfigFiles + files(
	"vitest.config.ts",
	"vitest.workspace.ts",
)

tasks {
	assembleFrontend {
		inputs.files(assembleConfigFiles)
		outputs.dir(distDir)
		outputs.dir(layout.buildDirectory.dir("vite"))
	}

	checkFrontend {
		dependsOn(rootProject.tasks.named("installBrowsers"))
		inputs.files(configFiles)
		inputs.dir(rootProject.layout.projectDirectory.dir(".playwright"))
		outputs.dir(layout.buildDirectory.dir("coverage"))
	}

	typeCheckFrontend {
		inputs.files(configFiles)
	}

	lintFrontend {
		inputs.files(configFiles)
	}

	fixFrontend {
		inputs.files(configFiles)
	}

	clean {
		delete(distDir)
	}
}

artifacts {
	add("productionAssets", distDir) {
		builtBy(tasks.assembleFrontend)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.tests", "src")
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "**/__fixtures__/**", "**/__tests__/**",
		"**/*.test.ts")
	SonarPropertiesUtils.addToList(properties, "sonar.test.inclusions", "**/__fixtures__/**", "**/__tests__/**",
		"**/*.test.ts")
	property("sonar.javascript.lcov.reportPaths", "${layout.buildDirectory.get()}/coverage/lcov.info")
}
