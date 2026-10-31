/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.internal.execution.caching.CachingState.enabled
import org.siouan.frontendgradleplugin.domain.ExecutableType
import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.internal.frontend-conventions")
	id("tools.refinery.gradle.sonarqube")
}

val frontendImplementation = configurations.create("frontendImplementation") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val typeCheckTypes = configurations.create("typeCheckTypes") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val typings = configurations.create("typings") {
	isCanBeConsumed = true
	isCanBeResolved = false
}

frontend {
	nodeDistributionProvided = true
	assembleScript.set("run build")
}

val configFiles: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
)

val lintingFiles: FileCollection = configFiles + files(
		rootProject.file(".eslintrc.cjs"), rootProject.file("prettier.config.cjs"))

tasks {
	installNode {
		dependsOn(rootProject.tasks.named("installNode"))
		enabled = false
	}

	resolvePackageManager {
		dependsOn(rootProject.tasks.named("resolvePackageManager"))
		// We can't enable this task, because the package manager is declared in the worktree `package.json` instead of
		// the `package.json` in this workspace (subproject).
		enabled = false
	}

	installPackageManager {
		dependsOn(rootProject.tasks.named("installPackageManager"))
		enabled = false
	}

	val rootInstallFrontend = rootProject.tasks.named("installFrontend")

	rootInstallFrontend.configure {
		inputs.file("$projectDir/package.json")
	}

	installFrontend {
		dependsOn(rootInstallFrontend)
		enabled = false
	}

	assembleFrontend {
		val onlyIfProvider = provider { frontend.assembleScript.isPresent }
		// Workaround to enable this task even if we have disabled `installPackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L514-L516
		onlyIf { onlyIfProvider.get() }
		// Workaround for disabled `resolvePackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L513
		executableType.set(ExecutableType.YARN)
		inputs.dir("src")
		inputs.files(configFiles)
		inputs.files(frontendImplementation)
	}

	checkFrontend {
		val onlyIfProvider = provider { frontend.checkScript.isPresent }
		// Workaround to enable this task even if we have disabled `installPackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L514-L516
		onlyIf { onlyIfProvider.get() }
		// Workaround for disabled `resolvePackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L513
		executableType.set(ExecutableType.YARN)
		inputs.dir("src")
		inputs.files(configFiles)
		inputs.files(frontendImplementation)
	}

	val typeCheckFrontend = register<RunYarnTaskType>("typeCheckFrontend") {
		dependsOn(installFrontend)
		inputs.dir("src")
		inputs.files(typeCheckTypes)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend = register<RunYarnTaskType>("lintFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir("src")
		inputs.files(lintingFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		args.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	register<RunYarnTaskType>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir("src")
		inputs.files(lintingFiles)
		args.set("run lint:fix")
		group = "verification"
		description = "Fix TypeScript lint errors and warnings."
	}
}

artifacts {
	add("typings", layout.buildDirectory.dir("typescript")) {
		builtBy(tasks.named("typeCheckFrontend"))
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", "src")
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
