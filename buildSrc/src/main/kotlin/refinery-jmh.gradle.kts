import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.sonarqube.gradle.SonarExtension
import tools.refinery.buildsrc.EclipseUtils
import tools.refinery.buildsrc.SonarPropertiesUtils

apply(plugin = "refinery-java-conventions")
apply(plugin = "refinery-sonarqube")

val sourceSets = the<SourceSetContainer>()

val main: SourceSet by sourceSets.getting

val test: SourceSet by sourceSets.getting

val jmh: SourceSet by sourceSets.creating {
	compileClasspath += main.output
	runtimeClasspath += main.output
	// Allow using test classes in benchmarks for now.
	compileClasspath += test.output
	runtimeClasspath += test.output
}

val jmhImplementation: Configuration by configurations.getting {
	extendsFrom(configurations["implementation"], configurations["testImplementation"])
}

val jmhAnnotationProcessor: Configuration by configurations.getting

configurations["jmhRuntimeOnly"].extendsFrom(configurations["runtimeOnly"], configurations["testRuntimeOnly"])

val libs = the<LibrariesForLibs>()

dependencies {
	jmhImplementation(libs.jmh.core)
	jmhAnnotationProcessor(libs.jmh.annprocess)
}

tasks.register("jmh", JavaExec::class) {
	dependsOn(tasks.named("jmhClasses"))
	mainClass.set("org.openjdk.jmh.Main")
	classpath = jmh.runtimeClasspath
}

EclipseUtils.patchClasspathEntries(the<EclipseModel>()) { entry ->
	// Workaround from https://github.com/gradle/gradle/issues/4802#issuecomment-407902081
	if (entry.entryAttributes["gradle_scope"] == "jmh") {
		// Allow test helper classes to be used in benchmarks from Eclipse
		// and do not expose JMH dependencies to the main source code.
		entry.entryAttributes["test"] = true
	} else {
		EclipseUtils.patchGradleUsedByScope(entry) { usedBy ->
			if (listOf("main", "test", "testFixtures").any { e -> usedBy.contains(e) }) {
				// main and test sources are also used by jmh sources.
				usedBy += "jmh"
			}
		}
	}
}

the<SonarExtension>().properties {
	SonarPropertiesUtils.addToList(properties, "sonar.tests", "src/jmh/java")
}
