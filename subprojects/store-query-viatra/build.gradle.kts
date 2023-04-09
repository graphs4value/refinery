plugins {
	id("tools.refinery.gradle.java-library")
}

dependencies {
	implementation(libs.ecore)
	api(libs.viatra)
	api(project(":refinery-store-query"))
}
