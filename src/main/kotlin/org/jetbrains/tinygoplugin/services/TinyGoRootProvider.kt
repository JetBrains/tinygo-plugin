package org.jetbrains.tinygoplugin.services

import com.goide.project.GoSyntheticLibrary
import com.goide.sdk.GoSdkUtil
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
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

class TinyGoRootProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) {
            return emptyList()
        }
        val tinyGoGoPathManager = TinyGoGopathManager()
        return GoSdkUtil.getGoModules(project).map { tinyGoGoPathManager.getGoPathSourcesRoots(project, it) }
            .map { TinyGoRootLibrary("tinygo", it) }.toSet()
    }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) {
            return emptyList()
        }
        val tinyGoGoPathManager = TinyGoGopathManager()
        return GoSdkUtil.getGoModules(project).map { tinyGoGoPathManager.getGoPathSourcesRoots(project, it) }
            .flatten().toSet()
    }
}
