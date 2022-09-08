package org.jetbrains.tinygoplugin.services

import com.goide.completion.GoImportsFilter
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.sdk.unknownVersion
import org.yaml.snakeyaml.Yaml
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Service
class UnsupportedPackageProvider(private val project: Project) {
    private fun loadUnsupportedLibraries(version: TinyGoSdkVersion): Set<String> {
        val stream = this.javaClass.classLoader.getResourceAsStream("libraries/$version.yaml")
        if (stream == null) {
            thisLogger().warn("Cannot load a list of supported libraries for $version")
            return emptySet()
        }
        val data: Map<String, Any> = Yaml().load(stream)
        return data.mapValues { entry ->
            if (entry.value is Boolean) entry.value as Boolean else true
        }.filterValues { !it }.keys
    }

    private val unsupportedLibrariesCache: ConcurrentMap<TinyGoSdkVersion, Set<String>> = ConcurrentHashMap()
    fun unsupportedLibraries(): Set<String> {
        val settings = project.tinyGoConfiguration()
        val version = settings.sdk.sdkVersion
        if (version == unknownVersion) {
            return emptySet()
        }
        return unsupportedLibrariesCache.computeIfAbsent(version, this::loadUnsupportedLibraries)
    }
}

class TinyGoImportsFilter : GoImportsFilter {
    override fun isExcluded(project: Project, import: String): Boolean {
        val unsupportedPackages = project.service<UnsupportedPackageProvider>()
        return unsupportedPackages.unsupportedLibraries().contains(import)
    }
}
