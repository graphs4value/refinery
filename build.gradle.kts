/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType
import tools.refinery.gradle.MavenPublishPlugin

plugins {
	alias(pluginLibs.plugins.versions)
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.frontend-worktree")
	id("tools.refinery.gradle.sonarqube")
}

val frontendFiles: FileCollection = files(
	"yarn.lock",
	"package.json",
	"tsconfig.json",
	"tsconfig.base.json",
	"eslintrc.cjs",
	"prettier.config.cjs",
	"vite.config.ts",
) + fileTree("scripts") {
	include("**/*.cjs")
}

val mavenRepositoryDir = layout.buildDirectory.map { it.dir("repo") }

tasks {
	val typeCheckFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		inputs.files(frontendFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.files(frontendFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		args.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarnTaskType>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.files(frontendFiles)
		args.set("run lint:fix")
		group = "verification"
		description = "Fix TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}
}

val cleanMavenRepository by tasks.registering(Delete::class) {
	delete(mavenRepositoryDir)
}

val mavenRepository by tasks.registering(Task::class) {
	dependsOn(cleanMavenRepository)
}

gradle.projectsEvaluated {
	mavenRepository.configure {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(MavenPublishPlugin::class)) {
				dependsOn(subproject.tasks.named("publishMavenJavaPublicationToFileRepository"))
			}
		}

		dependsOn(project("refinery-gradle-plugins").tasks.named("publishAllPublicationsToFileRepository"))
	}
}

sonarqube.properties {
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
