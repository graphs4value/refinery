/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Language Semantics Z3"
	description = "Partial modeling language bindings for the Z3 SMT solver"
}

dependencies {
	api(project(":refinery-language-semantics"))
	implementation(project(":refinery-store-reasoning-smt"))
}
