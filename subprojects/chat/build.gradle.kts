/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.frontend-workspace")
}

val esBuildOutputDir = layout.buildDirectory.dir("esbuild")

val productionResources: Provider<Directory> = esBuildOutputDir.map { it.dir("production") }

val assembleConfigFiles: FileCollection = files("esbuild.mjs")

dependencies {
	frontendImplementation(project(":refinery-client-js", "productionAssets"))
	typeCheckTypes(project(":refinery-client-js", "typings"))
}

tasks {
	assembleFrontend {
		inputs.files(assembleConfigFiles)
		outputs.dir(productionResources)
	}

	typeCheckFrontend {
		inputs.files(assembleConfigFiles)
	}

	lintFrontend {
		inputs.files(assembleConfigFiles)
	}

	fixFrontend {
		inputs.files(assembleConfigFiles)
	}
}
