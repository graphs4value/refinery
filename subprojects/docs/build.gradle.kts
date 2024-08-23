/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarn
import tools.refinery.gradle.JavaLibraryPlugin
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

frontend {
	assembleScript.set("run build")
}

val javadocs: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val releasedJavadocs: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val interpreterGroup = property("tools.refinery.interpreter.group").toString()
val releasedVersion = property("tools.refinery.release").toString()
val releasedInterpreterVersion = property("tools.refinery.interpreter.release").toString()

repositories {
	mavenCentral()
}

dependencies {
	gradle.projectsEvaluated {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(JavaLibraryPlugin::class)) {
				javadocs(project(subproject.path, "javadocElements"))
				val releasedProjectVersion = if (subproject.group.toString() == interpreterGroup)
					releasedInterpreterVersion else releasedVersion
				releasedJavadocs("${subproject.group}:${subproject.name}:$releasedProjectVersion:javadoc@jar")
			}
		}
	}

	javadocs(project(":refinery-gradle-plugins", "javadocElements"))
	releasedJavadocs("tools.refinery:refinery-gradle-plugins:$releasedVersion:javadoc@jar")
}

val srcDir = "src"
val docusaurusOutputDir = layout.buildDirectory.dir("docusaurus")
val javadocsDir = layout.buildDirectory.dir("javadocs")

val configFiles: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"babel.config.config.ts",
	"docusaurus.config.ts",
)

val lintConfigFiles: FileCollection = configFiles + files(
	rootProject.file(".eslintrc.cjs"), rootProject.file("prettier.config.cjs")
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
		configuration.resolvedConfiguration.resolvedArtifacts.associate { artifact ->
			artifact.moduleVersion.id.name to artifact.file
		}
	}
}

tasks {
	val extractJavadocs by registering(ExtractJavadocTask::class) {
		dependsOn(javadocs)
		targetDir = javadocsDir.map { it.dir("snapshot/develop/javadoc" ) }
		resolvedJavadocArtifacts = resolveJavadocs(javadocs)
	}

	val extractReleasedJavadocs by registering(ExtractJavadocTask::class) {
		dependsOn(releasedJavadocs)
		targetDir = javadocsDir.map { it.dir("develop/javadoc" ) }
		resolvedJavadocArtifacts = resolveJavadocs(releasedJavadocs)
	}

	assembleFrontend {
		dependsOn(extractJavadocs, extractReleasedJavadocs)
		inputs.dir(srcDir)
		inputs.dir("static")
		inputs.dir(javadocsDir)
		inputs.files(configFiles)
		outputs.dir(docusaurusOutputDir)
	}

	val typeCheckFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		script.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		script.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarn>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		script.set("run lint:fix")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	clean {
		delete(".docusaurus")
		delete(".yarn")
	}

	val siteZip by registering(Zip::class) {
		dependsOn(assembleFrontend)
		from(docusaurusOutputDir)
		archiveFileName = "refinery-docs.zip"
		destinationDirectory = layout.buildDirectory
	}

	assemble {
		dependsOn(siteZip)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", srcDir)
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
