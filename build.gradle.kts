plugins {
	// Workaround for https://github.com/gradle/gradle/issues/22797
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.versions)
	id("tools.refinery.gradle.eclipse")
	id("tools.refinery.gradle.frontend-worktree")
	id("tools.refinery.gradle.sonarqube")
}
