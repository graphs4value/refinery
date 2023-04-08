import org.siouan.frontendgradleplugin.infrastructure.gradle.*

plugins {
	id("refinery-eclipse")
	id("refinery-frontend-conventions")
}

tasks.named<NodeInstallTask>("installNode") {
	dependsOn(rootProject.tasks.named("installNode"))
	enabled = false
}

tasks.named<YarnGlobalInstallTask>("installYarnGlobally") {
	dependsOn(rootProject.tasks.named("installYarnGlobally"))
	enabled = false
}

tasks.named<InstallYarnTask>("installYarn") {
	dependsOn(rootProject.tasks.named("installYarn"))
	enabled = false
}

val rootInstallFrontend = rootProject.tasks.named("installFrontend")

rootInstallFrontend.configure {
	inputs.file("$projectDir/package.json")
}

tasks.named("installFrontend") {
	dependsOn(rootInstallFrontend)
	enabled = false
}
