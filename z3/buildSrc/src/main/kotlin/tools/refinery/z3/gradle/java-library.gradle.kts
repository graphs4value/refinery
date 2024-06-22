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
	withJavadocJar()
	withSourcesJar()

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

	javadoc {
		options {
			this as StandardJavadocDocletOptions
			addBooleanOption("Xdoclint:none", true)
			// {@code -Xmaxwarns 0} will print all warnings, so we must keep at least one.
			addStringOption("Xmaxwarns", "1")
			quiet()
		}
	}
}

open class MavenArtifactExtension {
	var nameSuffix: String? = null
}

val artifactExtension = project.extensions.create<MavenArtifactExtension>("mavenArtifact")

publishing.publications {
	register<MavenPublication>("mavenJava") {
		from(components["java"])
		pom {
			val nameString = provider {
				val prefix = "Z3 Java Bindings"
				val nameSuffix = artifactExtension.nameSuffix
				if (nameSuffix == null) prefix else "$prefix ($nameSuffix)"
			}
			name = nameString.map { "Refinery $it" }
			description = nameString.map {
				"$it for Refinery, an efficient graph solver for generating well-formed models"
			}
			url = "https://refinery.tools/"
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
			developers {
				developer {
					name = "The Refinery Authors"
					url = "https://refinery.tools/"
				}
				developer {
					name = "Microsoft Corporation"
					url = "https://github.com/Z3Prover/z3"
				}
			}
			scm {
				connection = "scm:git:https://github.com/graphs4value/refinery.git"
				developerConnection = "scm:git:ssh://github.com:graphs4value/refinery.git"
				url = "https://github.com/graphs4value/refinery"
			}
			issueManagement {
				url = "https://github.com/graphs4value/refinery/issues"
			}
		}
	}
}
