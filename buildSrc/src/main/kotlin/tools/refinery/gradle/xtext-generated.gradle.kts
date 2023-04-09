/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.internal.java-conventions")
	id("tools.refinery.gradle.sonarqube")
}

val xtextGenPath = "src/main/xtext-gen"

sourceSets.main {
	java.srcDir(xtextGenPath)
	resources.srcDir(xtextGenPath)
}

tasks.clean {
	delete(xtextGenPath)
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "$xtextGenPath/**")
}
