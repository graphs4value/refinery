/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Library for model generation with included native solvers"
}

dependencies {
	api(project(":refinery-generator-core"))
	runtimeOnly(project(":refinery-language-semantics-ibex"))
	runtimeOnly(project(":refinery-language-semantics-z3"))
}

tasks.javadoc {
	// Explicitly disable the Javadoc task, because `tools.refinery.gradle.internal.java-basic-conventions` would try
	// to read the Javadoc output and fail. Since we have no source files, no Javadoc output would be generated.
	enabled = false
}
