plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.java-test-fixtures")
}

dependencies {
	api(project(":refinery-store"))
	testFixturesApi(libs.hamcrest)
}
