/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Language Semantics IBEX"
	description = "Partial modeling language bindings for the IBEX constraint solver"
}

dependencies {
	api(project(":refinery-language-semantics"))
	implementation(libs.refinery.ibex)
	implementation(project(":refinery-store-reasoning-ibex"))
}
