/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import tools.refinery.gradle.utils.EclipseUtils

plugins {
	`java-test-fixtures`
	id("tools.refinery.gradle.internal.java-conventions")
}

publishing.publications {
	withType(MavenPublication::class) {
		suppressPomMetadataWarningsFor(configurations.testFixturesApiElements.name)
		suppressPomMetadataWarningsFor(configurations.testFixturesRuntimeElements.name)
	}
}

eclipse.classpath {
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
