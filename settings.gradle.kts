/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

rootProject.name = "refinery"

include(
		"frontend",
		"language",
		"language-ide",
		"language-model",
		"language-semantics",
		"language-web",
		"store",
		"store-dse",
		"store-query",
		"store-query-viatra",
		"store-reasoning",
		"store-reasoning-scope",
		"viatra-runtime",
		"viatra-runtime-localsearch",
		"viatra-runtime-rete",
		"viatra-runtime-rete-recipes",
		"visualization",
)

for (project in rootProject.children) {
	val projectName = project.name
	project.name = "${rootProject.name}-$projectName"
	project.projectDir = file("subprojects/$projectName")
}
