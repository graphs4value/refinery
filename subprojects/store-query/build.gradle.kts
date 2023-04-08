plugins {
	id("refinery-java-library")
	id("refinery-java-test-fixtures")
}

dependencies {
	api(project(":refinery-store"))
	testFixturesApi(libs.hamcrest)
}
