/*
 * SPDX-FileCopyrightText: 2024-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.internal.JavaBasicLibraryPlugin

plugins {
	id("tools.refinery.gradle.frontend-workspace")
}

val javadocs: Configuration = configurations.create("javadocs") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val releasedJavadocs: Configuration = configurations.create("releasedJavadocs") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val releasedVersion = property("tools.refinery.release").toString()

repositories {
	mavenCentral()
}

dependencies {
	gradle.projectsEvaluated {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(JavaBasicLibraryPlugin::class)) {
				javadocs(project(subproject.path, "javadocElements"))
				releasedJavadocs("${subproject.group}:${subproject.name}:$releasedVersion:javadoc@jar")
			}
		}
	}

	javadocs(project(":refinery-gradle-plugins", "javadocElements"))
	releasedJavadocs("tools.refinery:refinery-gradle-plugins:$releasedVersion:javadoc@jar")
}

val docusaurusOutputDir = layout.buildDirectory.dir("docusaurus")
val javadocsDir = layout.buildDirectory.dir("javadocs")

val configFiles: FileCollection = files(
	"babel.config.config.ts",
	"docusaurus.config.ts",
)

abstract class ExtractJavadocTask : DefaultTask() {
	@get:OutputDirectory
	abstract val targetDir: DirectoryProperty

	@get:Input
	abstract val resolvedJavadocArtifacts: MapProperty<String, File>

	@get:Inject
	abstract val fs: FileSystemOperations

	@get:Inject
	abstract val archive: ArchiveOperations

	@TaskAction
	fun action() {
		fs.delete {
			delete(targetDir)
		}
		val javadocsDocsDir = targetDir.get()
		resolvedJavadocArtifacts.get().forEach { artifact ->
			fs.copy {
				from(archive.zipTree(artifact.value))
				into(javadocsDocsDir.dir(artifact.key))
			}
		}
	}
}

fun resolveJavadocs(configuration: Configuration): Provider<Map<String, File>> {
	return provider {
		configuration.incoming.artifactView {
			// Use lenient resolution to avoid throwing an error on subprojects that don't have any released Javadoc
			// artifacts yet.
			lenient(true)
		}.artifacts.resolvedArtifacts.get().associate { artifact ->
			when (val componentIdentifier = artifact.id.componentIdentifier) {
				is ModuleComponentIdentifier -> componentIdentifier.module
				is ProjectComponentIdentifier -> componentIdentifier.projectName
				else -> throw IllegalArgumentException("Unsupported component identifier: $componentIdentifier")
			} to artifact.file
		}
	}
}

tasks {
	val extractJavadocs = register<ExtractJavadocTask>("extractJavadocs") {
		dependsOn(javadocs)
		targetDir = javadocsDir.map { it.dir("snapshot/develop/javadoc") }
		resolvedJavadocArtifacts = resolveJavadocs(javadocs)
		description = "Extract Javadoc HTML files"
	}

	val extractReleasedJavadocs = register<ExtractJavadocTask>("extractReleasedJavadocs") {
		dependsOn(releasedJavadocs)
		targetDir = javadocsDir.map { it.dir("develop/javadoc") }
		resolvedJavadocArtifacts = resolveJavadocs(releasedJavadocs)
		description = "Extract released Javadoc HTML files"
	}

	assembleFrontend {
		dependsOn(extractJavadocs, extractReleasedJavadocs)
		inputs.dir("static")
		inputs.dir(javadocsDir)
		inputs.files(configFiles)
		outputs.dir(docusaurusOutputDir)
	}

	typeCheckFrontend {
		inputs.files(configFiles)
	}

	lintFrontend {
		inputs.files(configFiles)
	}

	fixFrontend {
		inputs.files(configFiles)
	}

	clean {
		delete(".docusaurus")
		delete(".yarn")
	}
}
