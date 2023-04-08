plugins {
	application
	id("com.github.johnrengelman.shadow")
}

apply(plugin = "refinery-java-conventions")

for (taskName in listOf("distTar", "distZip", "shadowDistTar", "shadowDistZip")) {
	tasks.named(taskName) {
		enabled = false
	}
}
