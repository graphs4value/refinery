plugins {
	id("refinery-java-library")
	id("refinery-xtext-conventions")
}

dependencies {
	api(project(":refinery-language"))
	api(libs.xtext.ide)
	api(libs.xtext.xbase.ide)
}

val generateXtextLanguage by project(":refinery-language").tasks.existing

for (taskName in listOf("compileJava", "processResources")) {
	tasks.named(taskName) {
		dependsOn(generateXtextLanguage)
	}
}
