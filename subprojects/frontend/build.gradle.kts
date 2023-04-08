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

val sourcesWithoutTypeGen = fileTree("src") {
	exclude("**/*.typegen.ts")
}

val generateXStateTypes by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	inputs.files(sourcesWithoutTypeGen)
	inputs.file("package.json")
	inputs.file(rootProject.file("yarn.lock"))
	outputs.dir("src")
	script.set("run typegen")
	description = "Generate TypeScript typings for XState state machines."
}

tasks.assembleFrontend {
	dependsOn(generateXStateTypes)
	inputs.dir("public")
	inputs.files(sourcesWithoutTypeGen)
	inputs.file("index.html")
	inputs.files("package.json", "tsconfig.json", "tsconfig.base.json", "vite.config.ts")
	inputs.file(rootProject.file("yarn.lock"))
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
	inputs.dir("src")
	inputs.dir("types")
	inputs.files("package.json", "tsconfig.json", "tsconfig.base.json", "tsconfig.node.json")
	inputs.file(rootProject.file("yarn.lock"))
	outputs.dir("$buildDir/typescript")
	script.set("run typecheck")
	group = "verification"
	description = "Check for TypeScript type errors."
}

val lintFrontend by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	dependsOn(typeCheckFrontend)
	inputs.dir("src")
	inputs.dir("types")
	inputs.files(".eslintrc.cjs", "prettier.config.cjs")
	inputs.files("package.json", "tsconfig.json", "tsconfig.base.json", "tsconfig.node.json")
	inputs.file(rootProject.file("yarn.lock"))
	if (project.hasProperty("ci")) {
		outputs.file("$buildDir/eslint.json")
		script.set("run lint:ci")
	} else {
		script.set("run lint")
	}
	group = "verification"
	description = "Check for TypeScript lint errors and warnings."
}

val fixFrontend by tasks.registering(RunYarn::class) {
	dependsOn(tasks.installFrontend)
	dependsOn(generateXStateTypes)
	dependsOn(typeCheckFrontend)
	inputs.dir("src")
	inputs.dir("types")
	inputs.files(".eslintrc.cjs", "prettier.config.cjs")
	inputs.files("package.json", "tsconfig.json", "tsconfig.base.json", "tsconfig.node.json")
	inputs.file(rootProject.file("yarn.lock"))
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
	inputs.dir("public")
	inputs.files(sourcesWithoutTypeGen)
	inputs.file("index.html")
	inputs.files("package.json", "tsconfig.json", "tsconfig.base.json", "vite.config.ts")
	inputs.file(rootProject.file("yarn.lock"))
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
