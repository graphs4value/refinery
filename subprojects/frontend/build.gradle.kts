import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarn
import tools.refinery.buildsrc.SonarPropertiesUtils

plugins {
	id("refinery-frontend-workspace")
	id("refinery-sonarqube")
}

val viteOutputDir = "$buildDir/vite"
val productionResources = file("$viteOutputDir/production")

frontend {
	assembleScript.set("run build")
}

val productionAssets: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

val sourcesWithoutTypes = fileTree("src") {
	exclude("**/*.typegen.ts")
}

val sourcesWithTypes = fileTree("src") + fileTree("types")

val buildScripts = fileTree("config") + files(
		".eslintrc.cjs",
		"prettier.config.cjs",
		"vite.config.ts",
)

val installationState = files(
		rootProject.file("yarn.lock"),
		rootProject.file("package.json"),
		"package.json",
)

val sharedConfigFiles = installationState + files(
		"tsconfig.json",
		"tsconfig.base.json",
		"tsconfig.node.json",
		"tsconfig.shared.json",
)

val assembleConfigFiles = sharedConfigFiles + file("vite.config.ts") + fileTree("config") {
	include("**/*.ts")
}

val assembleSources = sourcesWithTypes + fileTree("public") + file("index.html")

val assembleFiles = assembleSources + assembleConfigFiles

val lintingFiles = sourcesWithTypes + buildScripts + sharedConfigFiles

val generateXStateTypes by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	inputs.files(sourcesWithoutTypes)
	inputs.files(installationState)
	outputs.dir("src")
	script.set("run typegen")
	description = "Generate TypeScript typings for XState state machines."
}

tasks.assembleFrontend {
	dependsOn(generateXStateTypes)
	inputs.files(assembleFiles)
	outputs.dir(productionResources)
}

artifacts {
	add("productionAssets", productionResources) {
		builtBy(tasks.assembleFrontend)
	}
}

val typeCheckFrontend by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	inputs.files(lintingFiles)
	outputs.dir("$buildDir/typescript")
	script.set("run typecheck")
	group = "verification"
	description = "Check for TypeScript type errors."
}

val lintFrontend by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	dependsOn(typeCheckFrontend)
	inputs.files(lintingFiles)
	outputs.file("$buildDir/eslint.json")
	script.set("run lint")
	group = "verification"
	description = "Check for TypeScript lint errors and warnings."
}

val fixFrontend by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	dependsOn(typeCheckFrontend)
	inputs.files(lintingFiles)
	script.set("run lint:fix")
	group = "verification"
	description = "Fix TypeScript lint errors and warnings."
}

tasks.check {
	dependsOn(typeCheckFrontend)
	dependsOn(lintFrontend)
}

tasks.register("serveFrontend", RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	inputs.files(assembleFiles)
	outputs.dir("$viteOutputDir/development")
	script.set("run serve")
	group = "run"
	description = "Start a Vite dev server with hot module replacement."
}

tasks.clean {
	delete("dev-dist")
	delete(fileTree("src") {
		include("**/*.typegen.ts")
	})
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", "src")
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "$buildDir/eslint.json")
}
