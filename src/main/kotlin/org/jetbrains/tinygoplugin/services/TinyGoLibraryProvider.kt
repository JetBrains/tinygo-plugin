package org.jetbrains.tinygoplugin.services

import com.goide.project.GoSyntheticLibrary
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons.TinyGoLibraryIcon
import javax.swing.Icon

class TinyGoRootLibrary(private val moduleName: String, private val sourceRoots: Collection<VirtualFile>) :
    GoSyntheticLibrary(), ItemPresentation {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TinyGoRootLibrary) {
            return false
        }
        return other.moduleName == moduleName && other.sourceRoots == sourceRoots
    }

    override fun hashCode(): Int {
        var result: Int = moduleName.hashCode()
        result = 31 * result + sourceRoots.hashCode()
        return result
    }

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots

    override fun getPresentableText(): String = moduleName

    override fun getIcon(unused: Boolean): Icon = TinyGoLibraryIcon
}

class TinyGoLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        thisLogger().debug("TinyGo SDK additional library (cached GOROOT) requested")
        val tinyGoRoots = getRootsToWatch(project)
        return if (tinyGoRoots.isEmpty()) emptyList() else listOf(
            TinyGoRootLibrary(
                "TinyGo ${project.tinyGoConfiguration().sdk.sdkVersion} " +
                    "(Go ${project.tinyGoConfiguration().cachedGoRoot.version})",
                tinyGoRoots
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
