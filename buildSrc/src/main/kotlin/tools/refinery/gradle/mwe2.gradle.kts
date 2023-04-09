/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	id("tools.refinery.gradle.internal.java-conventions")
}

val mwe2: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	extendsFrom(configurations.implementation.get())
}

val libs = the<LibrariesForLibs>()

dependencies {
	mwe2(libs.mwe2.launch)
}

eclipse.classpath.plusConfigurations += mwe2
