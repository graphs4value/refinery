rootProject.name = "refinery"

include(
		"frontend",
		"language",
		"language-ide",
		"language-model",
		"language-semantics",
		"language-web",
		"store",
		"store-query",
		"store-query-viatra",
		"store-reasoning",
)

for (project in rootProject.children) {
	val projectName = project.name
	project.name = "${rootProject.name}-$projectName"
	project.projectDir = file("subprojects/$projectName")
}
