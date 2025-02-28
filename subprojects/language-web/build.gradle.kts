/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
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
	implementation(project(":refinery-generator"))
	implementation(project(":refinery-language"))
	implementation(project(":refinery-language-ide"))
	implementation(project(":refinery-store-reasoning-scope"))
	implementation(libs.gson)
	implementation(libs.hk2.guiceBridge)
	implementation(libs.jetty.server)
	implementation(libs.jetty.servlet)
	implementation(libs.jetty.websocket.api)
	implementation(libs.jetty.websocket.server)
	implementation(libs.jakarta.ws.rs)
	implementation(libs.jersey.hk2)
	implementation(libs.jersey.server)
	implementation(libs.jersey.servlet)
	implementation(libs.jersey.validation)
	implementation(libs.slf4j)
	// Jersey uses java.util.logging.
	implementation(libs.slf4j.jul)
	implementation(libs.xtext.web)
	// Needed to avoid a Jersey warning, see
	// https://mkyong.com/webservices/jax-rs/jakarta-activation-datasource-was-not-found/
	runtimeOnly(libs.jakarta.activation)
	// Required for SSE support in Jersey.
	runtimeOnly(libs.jersey.sse)
	xtextGenerated(project(":refinery-language", "generatedWebSources"))
	webapp(project(":refinery-frontend", "productionAssets"))
	testImplementation(testFixtures(project(":refinery-language")))
	testImplementation(libs.jetty.websocket.client)
}

application {
	mainClass.set("tools.refinery.language.web.ServerLauncher")
}

tasks {
	jar {
		dependsOn(webapp)
		from(webapp) {
			into("webapp")
		}
	}

	register<JavaExec>("serve") {
		dependsOn(webapp)
		val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
		dependsOn(mainRuntimeClasspath)
		classpath(mainRuntimeClasspath)
		mainClass.set(application.mainClass)
		standardInput = System.`in`
		environment(System.getenv())
		environment("REFINERY_BASE_RESOURCE", webapp.singleFile)
		group = "run"
		description = "Start a Jetty web server serving the Xtext API and assets."
	}

	register<JavaExec>("serveBackend") {
		val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
		dependsOn(mainRuntimeClasspath)
		classpath(mainRuntimeClasspath)
		mainClass.set(application.mainClass)
		standardInput = System.`in`
		group = "run"
		description = "Start a Jetty web server serving the Xtext API without assets."
	}
}
