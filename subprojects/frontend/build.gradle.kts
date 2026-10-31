/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.frontend-workspace")
}

val viteOutputDir = layout.buildDirectory.dir("vite")

val productionResources: Provider<Directory> = viteOutputDir.map { it.dir("production") }

val productionAssets = configurations.create("productionAssets") {
	isCanBeConsumed = true
	isCanBeResolved = false
}

val types: FileCollection = fileTree("types")

val assembleConfigFiles: FileCollection = files(
	"tsconfig.node.json",
	"tsconfig.shared.json",
	"vite.config.ts",
) + fileTree("config")

val extraAssembleSources: FileCollection = types + fileTree("public") + files("index.html")

val assembleFiles: FileCollection = extraAssembleSources + assembleConfigFiles

val lintingFiles: FileCollection = types + assembleConfigFiles

dependencies {
	frontendImplementation(project(":refinery-client-js", "productionAssets"))
	typeCheckTypes(project(":refinery-client-js", "typings"))
}

tasks {
	assembleFrontend {
		inputs.files(assembleFiles)
		outputs.dir(productionResources)
	}

	typeCheckFrontend {
		inputs.files(lintingFiles)
	}

	lintFrontend {
		inputs.files(lintingFiles)
	}

	fixFrontend {
		inputs.files(lintingFiles)
	}

	clean {
		delete("dev-dist")
	}
}

artifacts {
	add("productionAssets", productionResources) {
		builtBy(tasks.assembleFrontend)
	}
}
