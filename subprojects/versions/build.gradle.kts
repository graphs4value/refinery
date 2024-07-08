/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.JavaLibraryPlugin

plugins {
	`version-catalog`
	id("tools.refinery.gradle.maven-publish")
}

mavenArtifact {
	name = "Refinery Version Catalog"
	description = "Version catalog of Refinery and its dependencies for Gradle"
}

val refineryVersion = "refinery"
val interpreterVersion = "refineryInterpreter"
val interpreterGroup = property("tools.refinery.interpreter.group").toString()
val shadowVersion = "shadow"

catalog.versionCatalog {
	from(files("../../gradle/libs.versions.toml"))
	version(refineryVersion, project.version.toString())
	version(interpreterVersion, property("tools.refinery.interpreter.version").toString())
	library("bom", group.toString(), "refinery-bom").versionRef(refineryVersion)
	library("bom-dependencies", group.toString(), "refinery-bom-dependencies").versionRef(refineryVersion)

	// Let downstream users add the shadow plugin to bundle their dependencies.
	version(shadowVersion, pluginLibs.versions.shadow.get())
	plugin("shadow", "io.github.goooler.shadow").versionRef(shadowVersion)
}

publishing.publications.named<MavenPublication>("mavenJava") {
	from(components["versionCatalog"])
}

gradle.projectsEvaluated {
	catalog.versionCatalog {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(JavaLibraryPlugin::class)) {
				val alias = subproject.name.removePrefix("refinery-")
				val group = subproject.group.toString()
				val versionRef = if (interpreterGroup == group) interpreterVersion else refineryVersion
				library(alias, group, subproject.name).versionRef(versionRef)
			}
		}
	}
}
