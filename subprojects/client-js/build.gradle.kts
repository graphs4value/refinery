/*
 * SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
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
	checkScript.set(if (project.hasProperty("ci")) "run test:run:ci" else "run test:run")
}

val srcDir = "src"

val distDir = "dist"

val configFiles: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"vite.config.ts",
	"vitest.config.ts",
	"vitest.workspace.ts",
)

val lintConfigFiles: FileCollection = configFiles + files(
	rootProject.file(".eslintrc.cjs"), rootProject.file("prettier.config.cjs")
)

tasks {
	assembleFrontend {
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(distDir)
		outputs.dir(layout.buildDirectory.dir("vite"))
	}

	checkFrontend {
		dependsOn(rootProject.tasks.named("installBrowsers"))
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("coverage"))
	}

	val typeCheckFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		args.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarnTaskType>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		args.set("run lint:fix")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	clean {
		delete(distDir)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", srcDir)
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
	property("sonar.javascript.lcov.reportPaths", "${layout.buildDirectory.get()}/coverage/lcov.info")
}
