import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.ProjectDependency
import tools.refinery.buildsrc.EclipseUtils

plugins {
    jacoco
    java
}

apply(plugin = "refinery-eclipse")

repositories {
	mavenCentral()
	maven {
		url = uri("https://repo.eclipse.org/content/groups/releases/")
	}
}

// Use log4j-over-slf4j instead of log4j 1.x in the tests.
configurations.testRuntimeClasspath {
	exclude(group = "log4j", module = "log4j")
}

val libs = the<LibrariesForLibs>()

dependencies {
	compileOnly(libs.jetbrainsAnnotations)
	testCompileOnly(libs.jetbrainsAnnotations)
	testImplementation(libs.hamcrest)
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testImplementation(libs.junit.params)
	testImplementation(libs.mockito.core)
	testImplementation(libs.mockito.junit)
	testImplementation(libs.slf4j.simple)
	testImplementation(libs.slf4j.log4j)
}

java.toolchain {
	languageVersion.set(JavaLanguageVersion.of(19))
}

tasks.withType(JavaCompile::class) {
    options.release.set(17)
}

val test = tasks.named<Test>("test")

val jacocoTestReport = tasks.named<JacocoReport>("jacocoTestReport")

test.configure {
    useJUnitPlatform {
        excludeTags("slow")
    }
    finalizedBy(jacocoTestReport)
}

jacocoTestReport.configure {
	dependsOn(test)
	reports {
		xml.required.set(true)
	}
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
	manifest {
		attributes(
				"Bundle-SymbolicName" to "${project.group}.${project.name}",
				"Bundle-Version" to project.version
		)
	}
}

val generateEclipseSourceFolders by tasks.registering

tasks.register("prepareEclipse") {
	dependsOn(generateEclipseSourceFolders)
	dependsOn(tasks.named("eclipseJdt"))
}

tasks.named("eclipseClasspath") {
	dependsOn(generateEclipseSourceFolders)
}

configure<EclipseModel> {
	EclipseUtils.patchClasspathEntries(this) { entry ->
		if (entry.path.endsWith("-gen")) {
			entry.entryAttributes["ignore_optional_problems"] = true
		}
		// If a project has a main dependency on a project and a test dependency on the testFixtures of a project,
		// it will be erroneously added as a test-only dependency to Eclipse. As a workaround, we add all project
		// dependencies as main dependencies (we do not deliberately use test-only project dependencies).
		if (entry is ProjectDependency) {
			entry.entryAttributes.remove("test")
		}
	}

	jdt.file.withProperties {
		// Allow @SuppressWarnings to suppress SonarLint warnings
		this["org.eclipse.jdt.core.compiler.problem.unhandledWarningToken"] = "ignore"
	}
}
