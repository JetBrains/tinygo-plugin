pluginManagement {
    repositories {
        // Use local repository to use patched gradle-intellij-plugin
        mavenLocal()
        // Default plugin repository
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        // JetBrains plugin repository
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org")
        // TODO [AK] remove two repositories below when switching to the 0.7.0 release
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}
rootProject.name = "tinygo-plugin"
