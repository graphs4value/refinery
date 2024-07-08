/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

rootProject.name = "buildSrc"

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}

		create("pluginLibs") {
			from(files("../gradle/pluginLibs.versions.toml"))
		}
	}
}
