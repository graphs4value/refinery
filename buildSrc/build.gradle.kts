plugins {
	`kotlin-dsl`
	// Workaround for https://github.com/gradle/gradle/issues/22797
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.versions)
}

repositories {
	gradlePluginPortal()
	mavenCentral()
}

dependencies {
	implementation(libs.gradlePlugin.frontend)
	implementation(libs.gradlePlugin.shadow)
	implementation(libs.gradlePlugin.sonarqube)
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
