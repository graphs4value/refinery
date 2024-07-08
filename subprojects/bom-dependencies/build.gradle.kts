/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.maven-bom")
}

mavenArtifact {
	name = "Refinery Dependencies BOM"
	description = "Java platform that controls the versions of transitive dependencies"
}

val libsCatalog = versionCatalogs.named("libs")

dependencies {
	api(platform(libs.junit.bom))

	constraints {
		// See https://github.com/gradle/gradle/issues/16784#issuecomment-1520556706
		for (alias in libsCatalog.libraryAliases) {
			val dependency = libsCatalog.findLibrary(alias).get()
			if (dependency.get().version != null) {
				api(dependency)
			}
		}
	}
}
