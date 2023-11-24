/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

val classifier = "z3-${version}-x64-win"
val library = "z3java-win32-x86-64"

dependencies {
	z3("Z3Prover:z3:${version}:${classifier}@zip")
}

val extractZ3Libs by tasks.registering(Sync::class) {
	dependsOn(configurations.z3)
	from({
		val zipFile = configurations.z3.map { it.singleFile }
		zipTree(zipFile).matching {
			include("${classifier}/bin/*.dll")
			// Do not include .NET assembly.
			exclude("${classifier}/bin/Microsoft.Z3.dll")
			includeEmptyDirs = false
		}
	})
	eachFile {
		val pathInBin = relativePath.segments.drop(2).toTypedArray()
		relativePath = RelativePath(true, library, *pathInBin)
	}
	into(layout.buildDirectory.dir("z3-extracted"))
}

sourceSets.main {
	resources.srcDir(extractZ3Libs)
}
