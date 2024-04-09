/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.siouan.frontendgradleplugin.domain.ExecutableType

plugins {
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.internal.frontend-conventions")
}

frontend {
	nodeDistributionProvided = true
}

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
		// Workaround to enable this task even if we have disabled `installPackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L514-L516
		setOnlyIf { frontend.assembleScript.isPresent }
		// Workaround for disabled `resolvePackageManager`. See
		// https://github.com/siouan/frontend-gradle-plugin/blob/2add49d3a74c927abc813d98787be116d0074afe/plugin/src/main/java/org/siouan/frontendgradleplugin/FrontendGradlePlugin.java#L513
		executableType.set(ExecutableType.YARN)
	}
}
