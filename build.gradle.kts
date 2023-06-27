import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.14.2"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

sourceSets["main"].java.srcDirs("src/main/gen")

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
}
dependencies {
    testImplementation("com.jetbrains.intellij.go:go-test-framework:GOLAND-232-EAP-SNAPSHOT") {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
        exclude("com.jetbrains.rd", "rd-core")
        exclude("com.jetbrains.rd", "rd-swing")
        exclude("com.jetbrains.rd", "rd-framework")
        exclude("org.jetbrains.teamcity", "serviceMessages")
        exclude("io.ktor", "ktor-network-jvm")
        exclude("com.jetbrains.infra", "download-pgp-verifier")
        exclude("ai.grazie.utils", "utils-common-jvm")
        exclude("ai.grazie.model", "model-common-jvm")
        exclude("ai.grazie.model", "model-gec-jvm")
        exclude("ai.grazie.model", "model-text-jvm")
        exclude("ai.grazie.nlp", "nlp-common-jvm")
        exclude("ai.grazie.nlp", "nlp-detect-jvm")
        exclude("ai.grazie.nlp", "nlp-langs-jvm")
        exclude("ai.grazie.nlp", "nlp-patterns-jvm")
        exclude("ai.grazie.nlp", "nlp-phonetics-jvm")
        exclude("ai.grazie.nlp", "nlp-similarity-jvm")
        exclude("ai.grazie.nlp", "nlp-stemmer-jvm")
        exclude("ai.grazie.nlp", "nlp-tokenizer-jvm")
        exclude("ai.grazie.spell", "hunspell-en-jvm")
        exclude("ai.grazie.spell", "gec-spell-engine-local-jvm")
        exclude("ai.grazie.utils", "utils-lucene-lt-compatibility-jvm")
    }

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
    implementation("org.codehaus.plexus:plexus-utils:3.5.1")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(pluginName_)
    version.set(platformVersion)
    type.set(platformType)
    downloadSources.set(platformDownloadSources.toBoolean())
    updateSinceUntilBuild.set(true)

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true
}

tasks {
    // Set the compatibility versions to 17
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }

    withType<Detekt> {
        jvmTarget = "17"
        reports {
            html.required.set(false)
            xml.required.set(false)
            txt.required.set(false)
        }
    }

    withType<Test> {
        systemProperty("idea.home.path", "~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.goland/goland/232.7295-EAP-CANDIDATE-SNAPSHOT/40acfac3037f866cd2f2d1fddaf15f1f90f24c38/goland-232.7295-EAP-CANDIDATE-SNAPSHOT.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.goland/goland/232.7295-EAP-CANDIDATE-SNAPSHOT/40acfac3037f866cd2f2d1fddaf15f1f90f24c38/goland-232.7295-EAP-CANDIDATE-SNAPSHOT/")
    }

    patchPluginXml {
        version.set(pluginVersion)
        sinceBuild.set(pluginSinceBuild)
        untilBuild.set(pluginUntilBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }
        )

        // Get the latest available change notes from the changelog file
        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        )
    }

    runPluginVerifier {
        ideVersions.set(pluginVerifierIdeVersions.split(',').map { it.trim() })
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
