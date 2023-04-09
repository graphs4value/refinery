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

tasks {
	jar {
		from(sourceSets.main.map { it.allSource }) {
			include("**/*.xtext")
		}
	}

	val generateXtextLanguage by registering(JavaExec::class) {
		mainClass.set("org.eclipse.emf.mwe2.launch.runtime.Mwe2Launcher")
		classpath(configurations.mwe2)
		inputs.file("src/main/java/tools/refinery/language/GenerateProblem.mwe2")
		inputs.file("src/main/java/tools/refinery/language/Problem.xtext")
		outputs.dir("src/main/xtext-gen")
		outputs.dir("src/testFixtures/xtext-gen")
		outputs.dir("../language-ide/src/main/xtext-gen")
		outputs.dir("../language-web/src/main/xtext-gen")
		args("src/main/java/tools/refinery/language/GenerateProblem.mwe2", "-p", "rootPath=/$projectDir/..")
	}

	for (taskName in listOf("compileJava", "processResources", "processTestFixturesResources",
			"generateEclipseSourceFolders")) {
		named(taskName) {
			dependsOn(generateXtextLanguage)
		}
	}

	clean {
		delete("src/main/xtext-gen")
		delete("src/testFixtures/xtext-gen")
		delete("../language-ide/src/main/xtext-gen")
		delete("../language-web/src/main/xtext-gen")
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "src/textFixtures/xtext-gen/**")
}

eclipse.project.natures.plusAssign("org.eclipse.xtext.ui.shared.xtextNature")
