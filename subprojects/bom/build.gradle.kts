/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.JavaLibraryPlugin

plugins {
	id("tools.refinery.gradle.maven-bom")
}

mavenArtifact {
	name = "Refinery BOM"
	description = "Java platform that controls the versions of Refinery and its transitive dependencies"
}

dependencies {
	api(platform(project(":refinery-bom-dependencies")))

	constraints {
		api(platform(project(":refinery-bom-dependencies")))
		api(platform(project(":refinery-versions")))
	}
}

gradle.projectsEvaluated {
	dependencies.constraints {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(JavaLibraryPlugin::class)) {
				api(project(subproject.path))
			}
		}
	}
}
