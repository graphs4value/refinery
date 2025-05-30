/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

frontend {
	assembleScript.set("run build")
}

val esBuildOutputDir = layout.buildDirectory.dir("esbuild")

val productionResources: Provider<Directory> = esBuildOutputDir.map { it.dir("production") }

val sources: FileCollection = fileTree("src")

val installationState: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
)

val assembleConfigFiles: FileCollection = installationState + files(
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"esbuild.mjs",
) + fileTree("config")

val assembleFiles: FileCollection = sources + assembleConfigFiles

val lintingFiles: FileCollection = sources + assembleConfigFiles + files(
	rootProject.file(".eslintrc.cjs"),
	rootProject.file("prettier.config.cjs"),
)

tasks {
	assembleFrontend {
		dependsOn(rootProject.project("refinery-client-js").tasks.named("assembleFrontend"))
		inputs.files(assembleFiles)
		outputs.dir(productionResources)
	}

	val typeCheckFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(rootProject.project("refinery-client-js").tasks.named("typeCheckFrontend"))
		inputs.files(lintingFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.files(lintingFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		args.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarnTaskType>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.files(lintingFiles)
		args.set("run lint:fix")
		group = "verification"
		description = "Fix TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", "src")
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
