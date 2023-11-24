/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle

plugins {
	`java-library`
	`maven-publish`
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

repositories {
	mavenCentral()

	// Configuration based on https://stackoverflow.com/a/34327202 to pretend that GitHub is an Ivy repository
	// in order to take advantage of Gradle dependency caching.
	val github = ivy {
		setUrl("https://github.com")
		patternLayout {
			artifact("/[organisation]/[module]/releases/download/[module]-[revision]/[classifier].[ext]")
			artifact("/[organisation]/[module]/archive/refs/tags/[module]-[revision].[ext]")
		}
		metadataSources {
			artifact()
		}
	}

	exclusiveContent {
		forRepositories(github)
		filter {
			includeGroup("Z3Prover")
		}
	}
}

val z3: Provider<Configuration> by configurations.registering {
	isCanBeConsumed = false
	isCanBeResolved = true
}

tasks {
	jar {
		manifest {
			attributes(
					"Bundle-SymbolicName" to "${project.group}.${project.name}",
					"Bundle-Version" to project.version
			)
		}
	}
}

publishing.publications {
	register<MavenPublication>("mavenJava") {
		from(components["java"])
		pom {
			licenses {
				license {
					name = "MIT License"
					url = "https://raw.githubusercontent.com/Z3Prover/z3/master/LICENSE.txt"
				}
				license {
					name = "The Apache License, Version 2.0"
					url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
				}
			}
		}
	}
}
