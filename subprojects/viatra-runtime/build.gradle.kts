/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	// Vendor code from Eclipse VIATRA is maintained by the VIATRA project,
	// so we don't need to keep track of coverage ourselves.
	// Our own modifications are covered by tests in the `store-query-viatra` subproject.
	id("tools.refinery.gradle.skip-coverage")
}

dependencies {
	implementation(libs.slf4j.log4j)
	implementation(libs.eclipseCollections)
	implementation(libs.eclipseCollections.api)
}
