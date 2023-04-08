plugins {
	// Workaround for https://github.com/gradle/gradle/issues/22797
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.versions)
	id("refinery-eclipse")
	id("refinery-frontend-worktree")
	id("refinery-sonarqube")
}
