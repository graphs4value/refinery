import tools.refinery.buildsrc.SonarPropertiesUtils

plugins {
	id("refinery-java-library")
	id("refinery-java-test-fixtures")
	id("refinery-sonarqube")
	id("refinery-mwe2")
	id("refinery-xtext-conventions")
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

tasks.jar {
	from(sourceSets.main.map { it.allSource }) {
		include("**/*.xtext")
	}
}

val generateXtextLanguage by tasks.registering(JavaExec::class) {
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
	tasks.named(taskName) {
		dependsOn(generateXtextLanguage)
	}
}

tasks.clean {
	delete("src/main/xtext-gen")
	delete("src/testFixtures/xtext-gen")
	delete("../language-ide/src/main/xtext-gen")
	delete("../language-web/src/main/xtext-gen")
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "src/textFixtures/xtext-gen/**")
}

eclipse.project.natures.plusAssign("org.eclipse.xtext.ui.shared.xtextNature")
