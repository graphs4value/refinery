plugins {
	id("refinery-java-library")
	id("refinery-xtext-conventions")
}

dependencies {
	api(project(":refinery-language"))
	api(libs.xtext.ide)
	api(libs.xtext.xbase.ide)
}

val generateXtextLanguage = project(":refinery-language").tasks.named("generateXtextLanguage")

for (taskName in listOf("compileJava", "processResources")) {
	tasks.named(taskName) {
		dependsOn(generateXtextLanguage)
	}
}
