/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	`java-gradle-plugin`
	`maven-publish`
	id("com.gradle.plugin-publish")
	id("tools.refinery.gradle.java-conventions")
	id("tools.refinery.gradle.signing")
}

val mavenRepositoryDir = rootProject.layout.buildDirectory.map { it.dir("repo") }
val generatedSourcesDir = project.layout.buildDirectory.dir("generated/sources/refineryVersion")
val generatedSourceFile = generatedSourcesDir.map {
	it.file("tools/refinery/gradle/plugins/internal/Versions.java")
}

java {
	setSourceCompatibility(11)
	setTargetCompatibility(11)
}

sourceSets.main {
	java.srcDir(generatedSourcesDir)
}

gradlePlugin {
	website = "https://refinery.tools/"
	vcsUrl = "https://github.com/graphs4value/refinery"

	plugins {
		create("settings") {
			id = "tools.refinery.settings"
			displayName = "Refinery settings plugin"
			description = "Configures common build settings for projects using Refinery"
			@Suppress("UnstableApiUsage")
			tags = listOf("refinery", "settings", "conventions")
			implementationClass = "tools.refinery.gradle.plugins.RefinerySettingsPlugin"
		}

		create("javaConventions") {
			id = "tools.refinery.java"
			displayName = "Refinery Java conventions plugin"
			description = "Configures common Java settings for projects using Refinery"
			@Suppress("UnstableApiUsage")
			tags = listOf("refinery", "java", "conventions")
			implementationClass = "tools.refinery.gradle.plugins.RefineryJavaPlugin"
		}
	}
}

abstract class GenerateVersionsFileTask : DefaultTask() {
	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@get:Input
	abstract val refineryVersion: Property<String>

	@get:Input
	abstract val useMavenLocal: Property<Boolean>

	@get:Input
	abstract val javaLanguageVersion: Property<Int>

	@TaskAction
	fun execute() {
		val file = outputFile.asFile.get()
		file.parentFile.mkdirs()
		file.writeText(
			"""
			package tools.refinery.gradle.plugins.internal;

			public final class Versions {
				public static final String REFINERY_VERSION = "${refineryVersion.get()}";

				public static final boolean USE_MAVEN_LOCAL = ${useMavenLocal.get()};

				public static final int JAVA_LANGUAGE_VERSION = ${javaLanguageVersion.get()};

				private Versions() {
					throw new IllegalStateException("This is a static utility class and should not be " +
							"instantiated directly.");
				}
			}
			""".trimIndent()
		)
	}
}

val generateVersionsFile by tasks.registering(GenerateVersionsFileTask::class) {
	outputs.dir(generatedSourcesDir)
	outputFile = generatedSourceFile
	refineryVersion = version.toString()
	useMavenLocal = false
	javaLanguageVersion = java.toolchain.languageVersion.map { it.asInt() }
}

gradle.taskGraph.whenReady {
	generateVersionsFile.configure {
		useMavenLocal = gradle.taskGraph.hasTask(":${project.name}:publishToMavenLocal")
	}
}

tasks.compileJava {
	dependsOn(generateVersionsFile)
}

tasks.sourcesJar {
	dependsOn(generateVersionsFile)
}

publishing {
	publications {
		create<MavenPublication>("pluginMaven") {
			pom {
				name = "Refinery Gradle Plugins"
				description = "Gradle convention plugins in Refinery, an efficient graph solver for generating " +
						"well-formed models"
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

afterEvaluate {
	for (publication in listOf("pluginMaven", "settingsPluginMarkerMaven", "javaConventionsPluginMarkerMaven")) {
		val capitalizedName = publication.replaceFirstChar { it.uppercase() }
		tasks.named("publish${capitalizedName}PublicationToFileRepository") {
			mustRunAfter(rootProject.tasks.named("cleanMavenRepository"))
			outputs.dir(mavenRepositoryDir)
		}
	}
}
