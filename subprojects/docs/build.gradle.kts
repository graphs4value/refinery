/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarn
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

frontend {
	assembleScript.set("run build")
}

val srcDir = "src"

val docusaurusOutputDir = layout.buildDirectory.dir("docusaurus")

val configFiles: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"babel.config.config.ts",
	"docusaurus.config.ts",
)

val lintConfigFiles: FileCollection = configFiles + files(
	rootProject.file(".eslintrc.cjs"),
	rootProject.file("prettier.config.cjs")
)

tasks {
	assembleFrontend {
		inputs.dir("src")
		inputs.files(configFiles)
		outputs.dir(docusaurusOutputDir)
	}

	val typeCheckFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		script.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		script.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarn>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		script.set("run lint:fix")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	clean {
		delete(".docusaurus")
		delete(".yarn")
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", srcDir)
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
