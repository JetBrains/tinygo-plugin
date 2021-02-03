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
    }
}
rootProject.name = "tinygo-plugin"
