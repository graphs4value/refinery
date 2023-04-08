import org.siouan.frontendgradleplugin.infrastructure.gradle.EnableYarnBerryTask
import org.siouan.frontendgradleplugin.infrastructure.gradle.FrontendExtension

plugins {
	id("org.siouan.frontend-jdk11")
}

configure<FrontendExtension> {
	nodeVersion.set(providers.gradleProperty("frontend.nodeVersion"))
	nodeInstallDirectory.set(file("$rootDir/.node"))
	yarnEnabled.set(true)
	yarnVersion.set(providers.gradleProperty("frontend.yarnVersion"))
}

tasks.named<EnableYarnBerryTask>("enableYarnBerry") {
	// There is no need to enable berry manually, because berry files are already committed to the repo.
	enabled = false
}
