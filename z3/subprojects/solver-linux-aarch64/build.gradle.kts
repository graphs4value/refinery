/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

mavenArtifact.nameSuffix = "Linux aarch64"

tasks.jar {
	// License information is redundant here, since it already gets added to the POM.
	exclude("**/*.license")
}
