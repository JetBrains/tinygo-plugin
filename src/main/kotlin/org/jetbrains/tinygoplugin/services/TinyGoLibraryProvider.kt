package org.jetbrains.tinygoplugin.services

import com.goide.project.GoSyntheticLibrary
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons.TinyGoLibraryIcon
import javax.swing.Icon

class TinyGoRootLibrary(
    private val moduleName: String,
    private val sourceRoots: Collection<VirtualFile>,
    private val excludedRoots: Set<VirtualFile>,
) : GoSyntheticLibrary("TinyGoCachedGoRoot", null), ItemPresentation {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TinyGoRootLibrary) {
            return false
        }
        return other.moduleName == moduleName && other.sourceRoots == sourceRoots &&
            other.excludedRoots == excludedRoots
    }

    override fun hashCode(): Int {
        var result: Int = moduleName.hashCode()
        result = 31 * result + sourceRoots.hashCode()
        result = 31 * result + excludedRoots.hashCode()
        return result
    }

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots

    override fun getExcludedRoots(): Set<VirtualFile> = excludedRoots

    override fun getPresentableText(): String = moduleName

    override fun getIcon(unused: Boolean): Icon = TinyGoLibraryIcon
}

class TinyGoLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        thisLogger().debug("TinyGo SDK additional library (cached GOROOT) requested")
        val tinyGoRoots = getRootsToWatch(project)
        if (tinyGoRoots.isEmpty()) return emptyList()

        val settings = project.tinyGoConfiguration()
        val cachedGoRoot = settings.cachedGoRoot.sdkRoot ?: return emptyList()
        val layoutService = project.service<TinyGoLibraryLayoutService>()
        layoutService.requestRefresh(settings, cachedGoRoot)
        val excludedRoots = layoutService.getExcludedRoots(cachedGoRoot, settings)
        if (excludedRoots.isNotEmpty()) {
            thisLogger().debug("TinyGo cached GOROOT has ${excludedRoots.size} excluded roots")
        }
        return listOf(
            TinyGoRootLibrary(
                "TinyGo ${settings.sdk.sdkVersion} (Go ${settings.cachedGoRoot.version})",
                tinyGoRoots,
                excludedRoots,
            )
        )
    }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        val settings = project.tinyGoConfiguration()
        if (!settings.enabled) {
            thisLogger().debug("cached GOROOT not presented because TinyGo is disabled")
            return emptyList()
        }
        val tinyGoCachedGoRoot = settings.cachedGoRoot
        val tinyGoCachedGoRootSources = tinyGoCachedGoRoot.srcDir ?: return emptyList()
        thisLogger().debug("cached GOROOT presented at ${project.tinyGoConfiguration().cachedGoRoot}")
        return listOf(tinyGoCachedGoRootSources)
    }
}
