import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.jar.JarFile

enum class VersionBump { PATCH, MINOR, MAJOR }

val semanticVersionPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")

fun bumpSkyveilVersion(kind: VersionBump): String {
	val propertiesFile = rootProject.file("gradle.properties")
	val text = propertiesFile.readText()
	val propertyPattern = Regex("(?m)^mod_version=(\\d+)\\.(\\d+)\\.(\\d+)\\s*$")
	val match = propertyPattern.find(text) ?: throw GradleException("mod_version must exist in gradle.properties and use MAJOR.MINOR.PATCH")
	val major = match.groupValues[1].toInt()
	val minor = match.groupValues[2].toInt()
	val patch = match.groupValues[3].toInt()
	val next = when (kind) {
		VersionBump.PATCH -> "$major.$minor.${patch + 1}"
		VersionBump.MINOR -> "$major.${minor + 1}.0"
		VersionBump.MAJOR -> "${major + 1}.0.0"
	}
	propertiesFile.writeText(text.replaceRange(match.range, "mod_version=$next"))

	val changelog = rootProject.file("CHANGELOG.md")
	val changelogText = if (changelog.exists()) changelog.readText() else "# Changelog\n"
	if (!Regex("(?m)^## ${Regex.escape(next)}$").containsMatchIn(changelogText)) {
		val titleEnd = changelogText.indexOf('\n').let { if (it < 0) changelogText.length else it + 1 }
		changelog.writeText(changelogText.substring(0, titleEnd).trimEnd() + "\n\n## $next\n\n" + changelogText.substring(titleEnd).trimStart())
	}
	logger.lifecycle("Skyveil version bumped: $major.$minor.$patch -> $next")
	return next
}

plugins {
	id("net.fabricmc.fabric-loom")
	id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

base {
	archivesName.set("Skyveil")
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("skyveil") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
	testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.1")
}

val clientSourceSet = sourceSets.getByName("client")
sourceSets.test {
	compileClasspath += clientSourceSet.output
	runtimeClasspath += clientSourceSet.output
}

tasks.test {
	useJUnitPlatform()
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
	// Keep repository diagnostics strict while ignoring missing optional annotations
	// embedded in third-party class files (currently Gson's Error Prone metadata).
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-classfile"))
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
	from("THIRD_PARTY_NOTICES.md")
}

val releaseVersion = version.toString()
val releaseFileName = "Skyveil-$releaseVersion.jar"

fun registerBumpTask(name: String, kind: VersionBump) = tasks.register(name) {
	group = "versioning"
	description = "Increments Skyveil's ${kind.name.lowercase()} version without building."
	doLast { bumpSkyveilVersion(kind) }
}

registerBumpTask("bumpPatch", VersionBump.PATCH)
registerBumpTask("bumpMinor", VersionBump.MINOR)
registerBumpTask("bumpMajor", VersionBump.MAJOR)

/**
 * Creates the only artifact intended for players. Loom 1.17 on Minecraft 26.1.2
 * exposes the production artifact as the standard jar task (there is no
 * remapJar task in this project's task graph). This task copies that jar into
 * an otherwise empty release directory and sanity-checks it.
 */
tasks.register<Sync>("release") {
	group = "build"
	description = "Builds and verifies the single user-installable Skyveil mod JAR."
	dependsOn(tasks.named("build"))
	from(layout.buildDirectory.dir("libs")) {
		include(releaseFileName)
	}
	into(layout.buildDirectory.dir("release"))

	doFirst {
		check(semanticVersionPattern.matches(releaseVersion)) {
			"mod_version must use MAJOR.MINOR.PATCH (for example 1.4.2), found '$releaseVersion'"
		}
	}

	doLast {
		val releaseDirectory = layout.buildDirectory.dir("release").get().asFile
		val jars = releaseDirectory.listFiles { file -> file.isFile && file.extension == "jar" }?.toList().orEmpty()
		check(jars.size == 1 && jars.single().name == releaseFileName) {
			"Release output must contain exactly $releaseFileName, found: ${jars.map { it.name }}"
		}

		JarFile(jars.single()).use { jar ->
			val names = jar.entries().asSequence().map { it.name }.toSet()
			check("fabric.mod.json" in names) { "Release JAR is missing fabric.mod.json" }
			val metadata = jar.getInputStream(jar.getJarEntry("fabric.mod.json")).bufferedReader().use { it.readText() }
			check(Regex("\\\"version\\\"\\s*:\\s*\\\"${Regex.escape(releaseVersion)}\\\"").containsMatchIn(metadata)) {
				"fabric.mod.json version does not match release filename/project version $releaseVersion"
			}
			check("assets/skyveil/icon.png.png" in names) { "Release JAR is missing the icon declared by fabric.mod.json" }
			check("assets/skyveil/data/attribute_shard_market.properties" in names) { "Release JAR is missing the Hunting Box market identity bridge" }
			check("assets/skyveil/data/attribute_ability_names.properties" in names) { "Release JAR is missing Attribute Menu display-name identities" }
			check("assets/skyveil/data/attribute_shard_heads.properties" in names) { "Release JAR is missing unique Attribute Shard source heads" }
			check("assets/skyveil/data/item_search_catalog.json" in names) { "Release JAR is missing the generated SkyBlock Item Search catalog" }
			check("assets/skyveil/release_notes.txt" in names) { "Release JAR is missing the in-game release notes" }
			check("name/skyveil/client/update/GitHubUpdateManager.class" in names) { "Release JAR is missing the GitHub updater" }
			check("THIRD_PARTY_NOTICES.md" in names) { "Release JAR is missing third-party data attribution" }
			check(names.any { it.startsWith("name/skyveil/") && it.endsWith(".class") }) {
				"Release JAR is missing Skyveil classes"
			}
			check(names.any { it.startsWith("assets/skyveil/") }) { "Release JAR is missing Skyveil assets" }
			check(names.any { it.endsWith(".mixins.json") }) { "Release JAR is missing its mixin configuration" }
			val removedPrefixes = listOf(
				"name/skyveil/client/fishing/",
				"name/skyveil/client/price/",
				"name/skyveil/client/marketplace/"
			)
			check(names.none { entry -> removedPrefixes.any(entry::startsWith) }) {
				"Release JAR contains removed Fishing or pricing implementation classes"
			}
			val removedEntries = setOf(
				"name/skyveil/client/mixin/FishingHookMixin.class",
				"name/skyveil/client/mixin/ClientLevelSoundMixin.class",
				"assets/skyveil/data/sea_creatures.json",
				"assets/skyveil/data/sea_creature_catch_messages.tsv",
				"assets/skyveil/data/attribute_shard_bazaar.properties",
				"assets/skyveil/data/attribute_shard_display_bazaar.properties"
			)
			check(names.intersect(removedEntries).isEmpty()) {
				"Release JAR contains removed feature resources or mixins: ${names.intersect(removedEntries)}"
			}
			check(names.none { it.startsWith("name/skyveil/client/map/") || it.startsWith("assets/skyveil/textures/maps/") || it.startsWith("assets/skyveil/data/maps/") }) {
				"Release JAR contains removed Map feature classes or resources"
			}
			check(names.none { it.endsWith(".mca") || it.endsWith(".zip") || it.endsWith("/level.dat") }) {
				"Release JAR must not contain raw Minecraft world data"
			}
		}
	}
}

fun registerReleaseBumpTask(name: String, kind: VersionBump) = tasks.register<GradleBuild>(name) {
	group = "versioning"
	description = "Increments the ${kind.name.lowercase()} version, then creates the verified release JAR."
	tasks = listOf("clean", "release")
	doFirst { bumpSkyveilVersion(kind) }
}

registerReleaseBumpTask("releasePatch", VersionBump.PATCH)
registerReleaseBumpTask("releaseMinor", VersionBump.MINOR)
registerReleaseBumpTask("releaseMajor", VersionBump.MAJOR)
