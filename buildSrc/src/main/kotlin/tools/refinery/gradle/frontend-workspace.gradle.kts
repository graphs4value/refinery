/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

plugins {
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.internal.frontend-conventions")
}

tasks {
	installNode {
		dependsOn(rootProject.tasks.named("installNode"))
		enabled = false
	}

	resolvePackageManager {
		dependsOn(rootProject.tasks.named("resolvePackageManager"))
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
}
