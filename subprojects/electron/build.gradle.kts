/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import javax.inject.Inject
import org.gradle.api.file.FileSystemOperations
import org.panteleyev.jlink.JLinkTask
import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
}

interface InjectedFileSystemOperations {
	@get:Inject
	val fs: FileSystemOperations
}

frontend {
	assembleScript.set(if (project.hasProperty("ci")) "run build:ci" else "run build")
	checkScript.set(if (project.hasProperty("ci")) "run test:run:ci" else "run test:run")
}

val frontendAssets: Configuration = configurations.create("frontendAssets") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val backendDistribution: Configuration = configurations.create("backendDistribution") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	frontendAssets(project(":refinery-frontend", "productionAssets"))
	backendDistribution(project(":refinery-generator-cli", "distTar"))
	backendDistribution(project(":refinery-language-web", "distTar"))
	typeCheckTypes(project(":refinery-frontend", "typings"))
}

val esBuildOutputDir = layout.buildDirectory.dir("esbuild")

val productionResources: Provider<Directory> = esBuildOutputDir.map { it.dir("production") }

val distDir = layout.buildDirectory.dir("dist")

val assembleFiles: FileCollection = files(
	rootProject.project("refinery-frontend").file("tsconfig.shared.json"),
	"electron-builder.config.mjs",
	"esbuild.mjs",
) + fileTree("app") + fileTree("scripts")

// The Windows CLI launcher is cross-compiled with a downloaded Zig toolchain.
// Only build it when we are actually producing Windows artifacts: on a Windows
// host (electron-builder targets the host OS by default), or when a cross-build
// forces it with `-Ptools.refinery.electron.winLauncher=true`. This keeps Linux/macOS
// contributor builds free of the extra toolchain download.
val buildWindowsLauncher: Boolean =
	System.getProperty("os.name").startsWith("Windows") ||
		(project.findProperty("tools.refinery.electron.winLauncher") as String?)?.toBoolean() == true

val lintingFiles: FileCollection = assembleFiles + files(
	rootProject.file(".eslintrc.cjs"),
	rootProject.file("prettier.config.cjs"),
	"vitest.config.ts",
	"vitest.e2e.config.ts",
) + fileTree("e2e")

tasks {
	val jlink = register<JLinkTask>("jlink") {
		noHeaderFiles = true
		noManPages = true
		stripDebug = true
		addModules = listOf("java.base", "java.logging", "java.management", "java.naming", "java.xml", "jdk.zipfs")
		output.set(layout.buildDirectory.dir("jre"))
		description = "Create a stripped down JRE"
	}

	var extractBackend = register<RunYarnTaskType>("extractBackend") {
		dependsOn(installFrontend)
		inputs.files(
			rootProject.file("yarn.lock"),
			rootProject.file("package.json"),
			"package.json",
			fileTree("scripts"),
		)
		inputs.files(backendDistribution)
		outputs.dir(layout.buildDirectory.dir("backend"))
		outputs.dir(layout.buildDirectory.dir("esbuild/production"))
		args.set("run backend:extract")
		description = "Extract backend binaries"
	}

	val installZig = register<RunYarnTaskType>("installZig") {
		dependsOn(installFrontend)
		inputs.files(
			rootProject.file("yarn.lock"),
			rootProject.file("package.json"),
			"package.json",
			"scripts/zig.mjs",
			"scripts/installZig.mjs",
		)
		outputs.dir(layout.buildDirectory.dir("zig"))
		args.set("run zig:install")
		description = "Download the Zig toolchain used to build the Windows launcher"
	}

	val buildLauncher = register<RunYarnTaskType>("buildLauncher") {
		dependsOn(installZig)
		inputs.dir(layout.buildDirectory.dir("zig"))
		inputs.files(
			"package.json",
			"scripts/zig.mjs",
			"scripts/buildLauncher.mjs",
			"src/refinery-launcher.c",
		)
		outputs.dir(layout.buildDirectory.dir("launcher"))
		args.set("run launcher:build")
		description = "Cross-compile the Windows CLI launcher with Zig"
	}

	assembleFrontend {
		val distDirectoryPath = distDir.get().asFile.absolutePath
		val injectedFileSystemOperations = project.objects.newInstance<InjectedFileSystemOperations>()
		doFirst {
			injectedFileSystemOperations.fs.delete {
				delete(distDirectoryPath)
			}
		}
		dependsOn(jlink)
		dependsOn(extractBackend)
		if (buildWindowsLauncher) {
			dependsOn(buildLauncher)
			inputs.dir(layout.buildDirectory.dir("launcher"))
		}
		inputs.files(frontendAssets)
		inputs.dir(layout.buildDirectory.dir("jre"))
		inputs.dir(layout.buildDirectory.dir("backend"))
		inputs.files(assembleFiles)
		inputs.dir("build-resources")
		outputs.dir(productionResources)
		outputs.dir(distDir)
	}

	checkFrontend {
		inputs.files("vitest.config.ts")
		outputs.dir(layout.buildDirectory.dir("coverage"))
	}

	val e2eTest = register<RunYarnTaskType>("e2eTest") {
		dependsOn(assembleFrontend)
		inputs.dir(distDir)
		inputs.files("vitest.e2e.config.ts")
		inputs.files(fileTree("e2e") {
			exclude("**/__diffs__/**")
		})
		outputs.file(layout.buildDirectory.file("e2e/results.json"))
		args.set(if (project.hasProperty("ci")) "run test:e2e:ci" else "run test:e2e")
		description = "Run end-to-end tests against the packaged Electron CLI"
	}

	named("check") {
		dependsOn(e2eTest)
	}

	typeCheckFrontend {
		dependsOn(rootProject.project("refinery-frontend").tasks.named("typeCheckFrontend"))
		inputs.files(lintingFiles)
	}

	lintFrontend {
		inputs.files(lintingFiles)
	}

	fixFrontend {
		inputs.files(lintingFiles)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.tests", "src")
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "**/*.test.ts")
	SonarPropertiesUtils.addToList(properties, "sonar.test.inclusions", "**/*.test.ts")
	property("sonar.javascript.lcov.reportPaths", "${layout.buildDirectory.get()}/coverage/lcov.info")
}
