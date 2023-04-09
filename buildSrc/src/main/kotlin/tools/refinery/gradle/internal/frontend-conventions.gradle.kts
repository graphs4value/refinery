package tools.refinery.gradle.internal

plugins {
	id("org.siouan.frontend-jdk11")
}

frontend {
	nodeVersion.set(providers.gradleProperty("frontend.nodeVersion"))
	nodeInstallDirectory.set(file("$rootDir/.node"))
	yarnEnabled.set(true)
	yarnVersion.set(providers.gradleProperty("frontend.yarnVersion"))
}

tasks.enableYarnBerry {
	// There is no need to enable berry manually, because berry files are already committed to the repo.
	enabled = false
}
