/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.sonarqube")
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.coverage.exclusions", "src/main/**")
}
