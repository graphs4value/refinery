plugins {
	id("refinery-java-application")
	id("refinery-xtext-conventions")
}

val webapp: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

configurations.all {
	// Use log4j-over-slf4j instead of log4j 1.x
	exclude(group = "log4j", module = "log4j")
}

dependencies {
	implementation(project(":refinery-language"))
	implementation(project(":refinery-language-ide"))
	implementation(libs.jetty.server)
	implementation(libs.jetty.servlet)
	implementation(libs.jetty.websocket.server)
	implementation(libs.slf4j.api)
	implementation(libs.slf4j.simple)
	implementation(libs.slf4j.log4j)
	implementation(libs.xtext.web)
	webapp(project(path = ":refinery-frontend", configuration = "productionAssets"))
	testImplementation(testFixtures(project(":refinery-language")))
	testImplementation(libs.jetty.websocket.client)
}

val generateXtextLanguage = project(":refinery-language").tasks.named("generateXtextLanguage")

for (taskName in listOf("compileJava", "processResources")) {
	tasks.named(taskName) {
		dependsOn(generateXtextLanguage)
	}
}

application {
	mainClass.set("tools.refinery.language.web.ServerLauncher")
	// Enable JDK 19 preview features for virtual thread support.
	applicationDefaultJvmArgs += "--enable-preview"
}

tasks.withType(JavaCompile::class) {
	options.release.set(19)
	// Enable JDK 19 preview features for virtual thread support.
	options.compilerArgs.plusAssign("--enable-preview")
}

// Enable JDK 19 preview features for virtual thread support.
fun enablePreview(task: JavaForkOptions) {
	task.jvmArgs("--enable-preview")
}

tasks.withType(Test::class) {
	enablePreview(this)
}

tasks.jar {
	dependsOn(webapp)
	from(webapp) {
		into("webapp")
	}
}

tasks.shadowJar {
	dependsOn(webapp)
	from(project.sourceSets.main.map { it.output })
	exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA","schema/*",
		".options", ".api_description", "*.profile", "about.*", "about_*.html", "about_files/*",
		"plugin.xml", "systembundle.properties", "profile.list", "META-INF/resources/xtext/**")
	append("plugin.properties")
	from(webapp) {
		into("webapp")
	}
}

tasks.register("serveBackend", JavaExec::class) {
	dependsOn(webapp)
	val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
	dependsOn(mainRuntimeClasspath)
	classpath(mainRuntimeClasspath)
	mainClass.set(application.mainClass)
	enablePreview(this)
	standardInput = System.`in`
	val baseResource = webapp.incoming.artifacts.artifactFiles.first()
	environment("BASE_RESOURCE", baseResource)
	group = "run"
	description = "Start a Jetty web server serving the Xtex API and assets."
}
