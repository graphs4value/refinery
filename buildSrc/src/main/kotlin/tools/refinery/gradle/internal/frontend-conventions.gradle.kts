/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.internal

plugins {
	id("org.siouan.frontend-jdk17")
}

frontend {
	nodeVersion.set(providers.gradleProperty("frontend.nodeVersion"))
	nodeInstallDirectory.set(file("$rootDir/.node"))
}
