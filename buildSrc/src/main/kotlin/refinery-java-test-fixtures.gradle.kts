import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import tools.refinery.buildsrc.EclipseUtils

plugins {
	`java-test-fixtures`
}

apply(plugin = "refinery-java-conventions")

the<EclipseModel>().classpath {
	containsTestFixtures.set(true)

	EclipseUtils.whenClasspathFileMerged(file) { eclipseClasspath ->
		val hasTest = eclipseClasspath.entries.any { entry ->
			entry is AbstractClasspathEntry && entry.entryAttributes["gradle_scope"] == "test"
		}
		EclipseUtils.patchClasspathEntries(eclipseClasspath) { entry ->
			// Workaround https://github.com/gradle/gradle/issues/11845 based on
			// https://discuss.gradle.org/t/gradle-used-by-scope-not-correctly-generated-when-the-java-test-fixtures-plugin-is-used/39935/2
			EclipseUtils.patchGradleUsedByScope(entry) { usedBy ->
				if (usedBy.contains("main")) {
					usedBy += "testFixtures"
				}
				if (hasTest && usedBy.contains("testFixtures")) {
					usedBy += "test"
				}
			}
		}
	}
}
