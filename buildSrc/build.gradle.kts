/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	`kotlin-dsl`
	alias(pluginLibs.plugins.versions)
}

repositories {
	gradlePluginPortal()
	mavenCentral()
}

dependencies {
	implementation(pluginLibs.frontend)
	implementation(pluginLibs.pluginPublish)
	implementation(pluginLibs.shadow)
	implementation(pluginLibs.sonarqube)
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
	constraints {
		implementation(pluginLibs.httpclient) {
			because("CVE-2025-27820 in transitive dependency of org.siouan:frontend-jdk21")
		}
	}
}
