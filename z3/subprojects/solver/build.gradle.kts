/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import tools.refinery.z3.gradle.ClassFilePatcher

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

val classifier = "z3-${version}-x64-glibc-2.31"
val extractedClassesDir = layout.buildDirectory.dir("z3-extracted")
val extractedSourcesDir = layout.buildDirectory.dir("z3-sources")

java {
	withJavadocJar()
	withSourcesJar()
}

val z3Source: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val extractZ3Jar by tasks.registering(Sync::class) {
	dependsOn(configurations.z3)
	from({
		val zipFile = configurations.z3.map { it.singleFile }
		val jarFile = zipTree(zipFile).matching {
			include("${classifier}/bin/com.microsoft.z3.jar")
		}.singleFile
		zipTree(jarFile).matching {
			exclude("META-INF/MANIFEST.MF")
			includeEmptyDirs = false
		}
	})
	into(extractedClassesDir)
	doLast {
		// The class initializer off {@see com.microsoft.z3.Native} will try to load the Z3 native libraries
		// from the system default library path unless the {@code z3.skipLibraryLoad} system property is set.
		// Since we don't control the library path or system properties, we remove the class initializer entirely.
		val nativeClassFile = extractedClassesDir.get().file("com/microsoft/z3/Native.class").asFile
		ClassFilePatcher.removeClassInitializer(nativeClassFile)
	}
}

val extractZ3Source by tasks.registering(Sync::class) {
	dependsOn(z3Source)
	from({
		val zipFile = z3Source.singleFile
		zipTree(zipFile).matching {
			include("z3-z3-${version}/src/api/java/**/*")
			includeEmptyDirs = false
		}
	})
	eachFile {
		val pathInBin = relativePath.segments.drop(4).toTypedArray()
		relativePath = RelativePath(true, "com", "microsoft", "z3", *pathInBin)
	}
	into(extractedSourcesDir)
}

tasks.jar {
	// Add class files to our jar manually.
	from(extractZ3Jar)
}

tasks.test {
	useJUnitPlatform()
}

tasks.named<Jar>("sourcesJar") {
	from(extractZ3Source)
}

tasks.named<Javadoc>("javadoc") {
	source(sourceSets.main.map { it.allJava })
	source(fileTree(extractedSourcesDir) {
		builtBy(extractZ3Source)
		include("**/*.java")
	})
	options {
		this as StandardJavadocDocletOptions
		addBooleanOption("Xdoclint:none", true)
		// {@code -Xmaxwarns 0} will print all warnings, so we must keep at least one.
		addStringOption("Xmaxwarns", "1")
		quiet()
	}
}

dependencies {
	z3("Z3Prover:z3:${version}:${classifier}@zip")
	z3Source("Z3Prover:z3:${version}@zip")
	// This dependency doesn't get added to Maven metadata, so we have to add the class files to our jar manually.
	api(files(extractZ3Jar))
	implementation(libs.jna)
	implementation(project(":refinery-z3-solver-darwin-aarch64"))
	implementation(project(":refinery-z3-solver-darwin-x86-64"))
	implementation(project(":refinery-z3-solver-linux-aarch64"))
	implementation(project(":refinery-z3-solver-linux-x86-64"))
	implementation(project(":refinery-z3-solver-win32-x86-64"))
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
