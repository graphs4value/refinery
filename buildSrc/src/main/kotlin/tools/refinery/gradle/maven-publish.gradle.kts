/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

plugins {
	`maven-publish`
	id("tools.refinery.gradle.signing")
}

val mavenRepositoryDir = rootProject.layout.buildDirectory.map { it.dir("repo") }

open class MavenArtifactExtension(project: Project) {
	var name: String = project.name.split("-").drop(1).joinToString(" ") { segment ->
		segment.replaceFirstChar { it.uppercase() }
	}
	var description: String? = null
}

val artifactExtension = project.extensions.create<MavenArtifactExtension>("mavenArtifact", project)

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			pom {
				name = provider { "Refinery ${artifactExtension.name}" }
				description = provider {
					val prefix = artifactExtension.description ?: artifactExtension.name.lowercase()
						.replaceFirstChar { it.uppercase() }
					"$prefix in Refinery, an efficient graph solver for generating well-formed models"
				}
				url = "https://refinery.tools/"
				licenses {
					license {
						name = "Eclipse Public License - v 2.0"
						url = "https://www.eclipse.org/legal/epl-2.0/"
					}
				}
				developers {
					developer {
						name = "The Refinery Authors"
						url = "https://refinery.tools/"
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

	repositories {
		maven {
			name = "file"
			setUrl(mavenRepositoryDir.map { uri(it) })
		}
	}
}

tasks.named<PublishToMavenRepository>("publishMavenJavaPublicationToFileRepository") {
	mustRunAfter(rootProject.tasks.named("cleanMavenRepository"))
	outputs.dir(mavenRepositoryDir)
}

signing {
	sign(publishing.publications["mavenJava"])
}
