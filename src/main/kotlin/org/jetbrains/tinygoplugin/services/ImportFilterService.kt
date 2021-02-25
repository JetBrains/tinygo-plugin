package org.jetbrains.tinygoplugin.services

import com.goide.codeInsight.imports.GoImportsSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.yaml.snakeyaml.Yaml

interface SupportedLibrariesFilter {
    fun check(name: String): Boolean
}

class DummyLibraryFilter : SupportedLibrariesFilter {
    override fun check(name: String): Boolean = true
}

class LibraryFilter(private val unsupportedPackages: Set<String>) : SupportedLibrariesFilter {
    private val defaultPolicy = true
    override fun check(name: String): Boolean = !unsupportedPackages.contains(name)
}

@Service
class TinyGoSupportedPackages(private val project: Project) {
    private fun excludePackages(excludedPackages: Set<String>) {
        val importsSettings = GoImportsSettings.getInstance(project)
        excludedPackages.forEach(importsSettings::excludePath)
    }

    private val supportedPackagesData: SupportedLibrariesFilter by lazy {
        val stream = this.javaClass.classLoader.getResourceAsStream("libraries/latest.yaml")
        if (stream == null) {
            thisLogger().warn("Could not load list of supported libraries")
            DummyLibraryFilter()
        } else {
            val data: Map<String, Any> = Yaml().load(stream)
            val supportedLibraries = data.mapValues { entry ->
                if (entry.value is Boolean) entry.value as Boolean else true
            }.filterValues { !it }.keys
            excludePackages(supportedLibraries)
            LibraryFilter(supportedLibraries)
        }
    }

    private val tinyGoEnabled by lazy {
        TinyGoConfiguration.getInstance(project).enabled
    }
    val supportedPackages: SupportedLibrariesFilter
        get() {
            return if (tinyGoEnabled) {
                supportedPackagesData
            } else {
                DummyLibraryFilter()
            }
        }
}
