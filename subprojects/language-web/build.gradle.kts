/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-application")
	id("tools.refinery.gradle.xtext-generated")
}

val webapp: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	implementation(project(":refinery-language"))
	implementation(project(":refinery-language-ide"))
	implementation(libs.jetty.server)
	implementation(libs.jetty.servlet)
	implementation(libs.jetty.websocket.server)
	implementation(libs.slf4j.api)
	implementation(libs.xtext.web)
	webapp(project(path = ":refinery-frontend", configuration = "productionAssets"))
	testImplementation(testFixtures(project(":refinery-language")))
	testImplementation(libs.jetty.websocket.client)
}

application {
	mainClass.set("tools.refinery.language.web.ServerLauncher")
}

tasks {
	val generateXtextLanguage by project(":refinery-language").tasks.existing

	for (taskName in listOf("compileJava", "processResources")) {
		named(taskName) {
			dependsOn(generateXtextLanguage)
		}
	}

	jar {
		dependsOn(webapp)
		from(webapp) {
			into("webapp")
		}
	}

	shadowJar {
		dependsOn(webapp)
		from(project.sourceSets.main.map { it.output })
		exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "schema/*",
				".options", ".api_description", "*.profile", "about.*", "about_*.html", "about_files/*",
				"plugin.xml", "systembundle.properties", "profile.list", "META-INF/resources/xtext/**")
		append("plugin.properties")
		from(webapp) {
			into("webapp")
		}
	}

	register<JavaExec>("serveBackend") {
		dependsOn(webapp)
		val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
		dependsOn(mainRuntimeClasspath)
		classpath(mainRuntimeClasspath)
		mainClass.set(application.mainClass)
		standardInput = System.`in`
		val baseResource = webapp.incoming.artifacts.artifactFiles.first()
		environment("BASE_RESOURCE", baseResource)
		group = "run"
		description = "Start a Jetty web server serving the Xtex API and assets."
	}
}
