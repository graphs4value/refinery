/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	`kotlin-dsl`
	alias(libs.plugins.versions)
}

repositories {
	gradlePluginPortal()
	mavenCentral()
}

dependencies {
	implementation(libs.gradlePlugin.frontend)
	implementation(libs.gradlePlugin.shadow)
	implementation(libs.gradlePlugin.sonarqube)
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
