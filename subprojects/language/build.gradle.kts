/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.java-test-fixtures")
	id("tools.refinery.gradle.mwe2")
	id("tools.refinery.gradle.sonarqube")
	id("tools.refinery.gradle.xtext-generated")
}

val generatedIdeSources: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

val generatedWebSources: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

dependencies {
	api(platform(libs.xtext.bom))
	api(libs.ecore)
	api(libs.xtext.core)
	api(libs.xtext.xbase)
	api(project(":refinery-language-model"))
	testFixturesApi(libs.xtext.testing)
	mwe2(libs.xtext.generator)
	mwe2(libs.xtext.generator.antlr)
}

sourceSets {
	testFixtures {
		java.srcDir("src/testFixtures/xtext-gen")
		resources.srcDir("src/testFixtures/xtext-gen")
	}
}

val generateXtextLanguage by tasks.registering(JavaExec::class) {
	mainClass.set("org.eclipse.emf.mwe2.launch.runtime.Mwe2Launcher")
	classpath(configurations.mwe2)
	inputs.file("src/main/java/tools/refinery/language/GenerateProblem.mwe2")
	inputs.file("src/main/java/tools/refinery/language/Problem.xtext")
	inputs.file("../language-model/src/main/resources/model/problem.ecore")
	inputs.file("../language-model/src/main/resources/model/problem.genmodel")
	outputs.dir("src/main/xtext-gen")
	outputs.dir("src/testFixtures/xtext-gen")
	outputs.dir(layout.buildDirectory.dir("generated/sources/xtext/ide"))
	outputs.dir(layout.buildDirectory.dir("generated/sources/xtext/web"))
	args("src/main/java/tools/refinery/language/GenerateProblem.mwe2", "-p", "rootPath=/$projectDir/..")
}

tasks {
	jar {
		from(sourceSets.main.map { it.allSource }) {
			include("**/*.xtext")
		}
	}

	syncXtextGeneratedSources {
		// We generate Xtext runtime sources directly to {@code src/main/xtext-gen}, so there is no need to copy them
		// from an artifact. We expose the {@code generatedIdeSources} and {@code generatedWebSources} artifacts to
		// sibling IDE and web projects which can use this task to consume them and copy the appropriate sources to
		// their own {@code src/main/xtext-gen} directory.
		enabled = false
	}

	for (taskName in listOf("compileJava", "processResources", "compileTestFixturesJava",
			"processTestFixturesResources", "generateEclipseSourceFolders", "sourcesJar")) {
		named(taskName) {
			dependsOn(generateXtextLanguage)
		}
	}

	clean {
		delete("src/main/xtext-gen")
		delete("src/testFixtures/xtext-gen")
	}
}

artifacts {
	add(generatedIdeSources.name, layout.buildDirectory.dir("generated/sources/xtext/ide")) {
		builtBy(generateXtextLanguage)
	}

	add(generatedWebSources.name, layout.buildDirectory.dir("generated/sources/xtext/web")) {
		builtBy(generateXtextLanguage)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "src/textFixtures/xtext-gen/**")
}

eclipse.project.natures.plusAssign("org.eclipse.xtext.ui.shared.xtextNature")
