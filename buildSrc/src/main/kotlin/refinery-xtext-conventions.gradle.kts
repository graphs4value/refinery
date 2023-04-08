import org.gradle.api.tasks.SourceSetContainer
import org.sonarqube.gradle.SonarExtension
import tools.refinery.buildsrc.SonarPropertiesUtils

apply(plugin = "refinery-java-conventions")
apply(plugin = "refinery-sonarqube")

val xtextGenPath = "src/main/xtext-gen"

the<SourceSetContainer>().named("main") {
	java.srcDir(xtextGenPath)
	resources.srcDir(xtextGenPath)
}

tasks.named<Delete>("clean") {
	delete(xtextGenPath)
}

the<SonarExtension>().properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "$xtextGenPath/**")
}
