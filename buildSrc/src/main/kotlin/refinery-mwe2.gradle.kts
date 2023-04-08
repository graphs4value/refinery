import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.plugins.ide.eclipse.model.EclipseModel

apply(plugin = "refinery-java-conventions")

val mwe2: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	extendsFrom(configurations["implementation"])
}

val libs = the<LibrariesForLibs>()

dependencies {
	mwe2(libs.mwe2.launch)
}

the<EclipseModel>().classpath.plusConfigurations += mwe2
