/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

rootProject.name = "refinery-z3"

include(
		"solver",
		"solver-darwin-aarch64",
		"solver-darwin-x86-64",
		"solver-linux-aarch64",
		"solver-linux-x86-64",
		"solver-win32-x86-64",
)

for (project in rootProject.children) {
	val projectName = project.name
	project.name = "${rootProject.name}-$projectName"
	project.projectDir = file("subprojects/$projectName")
}
