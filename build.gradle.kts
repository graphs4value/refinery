/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType

plugins {
	alias(pluginLibs.plugins.versions)
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.frontend-worktree")
}

val frontendFiles: FileCollection = files(
	"yarn.lock",
	"package.json",
	"tsconfig.json",
	"tsconfig.base.json",
	".eslintrc.cjs",
	"prettier.config.cjs",
) + fileTree("scripts") {
	include("**/*.cjs")
	include("**/*.mjs")
}

val mavenRepositoryDir = layout.buildDirectory.map { it.dir("repo") }

tasks {
	val typeCheckFrontend = register<RunYarnTaskType>("typeCheckFrontend") {
		dependsOn(installFrontend)
		inputs.files(frontendFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend = register<RunYarnTaskType>("lintFrontend") {
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

	register<RunYarnTaskType>("installBrowsers") {
		dependsOn(installFrontend)
		inputs.files(frontendFiles)
		outputs.dir(".playwright")
		args.set(if (project.hasProperty("ci")) "run browsers:install:ci" else "run browsers:install")
		description = "Install browser testing dependencies."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}
}

val cleanMavenRepository = tasks.register<Delete>("cleanMavenRepository") {
	delete(mavenRepositoryDir)
	description = "Clean files published to the Maven repository directory"
}

val mavenRepository = tasks.register<Task>("mavenRepository") {
	dependsOn(cleanMavenRepository)
	description = "Publish artifacts to Maven repository directory"
}

gradle.projectsEvaluated {
	mavenRepository.configure {
		for (subproject in rootProject.subprojects) {
			if (subproject.name != "refinery-gradle-plugins" && subproject.plugins.hasPlugin(
					MavenPublishPlugin::class)) {
				val publishTask = subproject.tasks.named("publishMavenJavaPublicationToFileRepository")
				publishTask.configure {
					mustRunAfter(cleanMavenRepository)
				}
				dependsOn(publishTask)
			}
		}

		val pluginPublishTask = project("refinery-gradle-plugins").tasks.named("publishAllPublicationsToFileRepository")
		pluginPublishTask.configure {
			mustRunAfter(cleanMavenRepository)
		}
		dependsOn(pluginPublishTask)
	}
}
