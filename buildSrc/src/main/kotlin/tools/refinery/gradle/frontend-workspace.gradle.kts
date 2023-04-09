/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
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

	installYarnGlobally {
		dependsOn(rootProject.tasks.named("installYarnGlobally"))
		enabled = false
	}

	installYarn {
		dependsOn(rootProject.tasks.named("installYarn"))
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
