/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.interpreter-library")
	id("tools.refinery.gradle.mwe2")
	id("tools.refinery.gradle.sonarqube")
}

dependencies {
	api(project(":refinery-interpreter"))
	api(libs.ecore)
	mwe2(libs.ecore.codegen)
	mwe2(libs.mwe.utils)
	mwe2(libs.mwe2.lib)
	mwe2(libs.slf4j.simple)
	mwe2(libs.xtext.core)
	mwe2(libs.xtext.xbase)
}

sourceSets {
	main {
		java.srcDir("src/main/emf-gen")
	}
}

tasks {
	val generateEPackage by registering(JavaExec::class) {
		mainClass.set("org.eclipse.emf.mwe2.launch.runtime.Mwe2Launcher")
		classpath(configurations.mwe2)
		inputs.file("src/main/java/tools/refinery/interpreter/rete/recipes/GenerateReteRecipes.mwe2")
		inputs.file("src/main/resources/model/recipes.ecore")
		inputs.file("src/main/resources/model/rete-recipes.genmodel")
		outputs.file("build.properties")
		outputs.file("META-INF/MANIFEST.MF")
		outputs.file("plugin.xml")
		outputs.file("plugin.properties")
		outputs.dir("src/main/emf-gen")
		args("src/main/java/tools/refinery/interpreter/rete/recipes/GenerateReteRecipes.mwe2",
				"-p", "rootPath=/$projectDir")
	}

	for (taskName in listOf("compileJava", "processResources", "generateEclipseSourceFolders", "sourcesJar")) {
		named(taskName) {
			dependsOn(generateEPackage)
		}
	}

	clean {
		delete("src/main/emf-gen")
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "src/main/emf-gen/**")
}

