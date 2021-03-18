package org.jetbrains.tinygoplugin.services

import com.goide.project.GoSyntheticLibrary
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons.TinyGoLibraryIcon
import java.nio.file.Paths
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
        val tinygoRoots = getRootsToWatch(project)
        return if (tinygoRoots.isEmpty()) emptyList() else listOf(TinyGoRootLibrary("tinygo", tinygoRoots))
    }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) {
            return emptyList()
        }
        val tinyGoSources = getTinyGoSources(project) ?: return emptyList()
        return listOf(tinyGoSources)
    }
}

internal fun getTinyGoSources(project: Project): VirtualFile? {
    val settings = TinyGoConfiguration.getInstance(project)
    val tinyGoSDKPath = settings.tinyGoSDKPath
    if (tinyGoSDKPath.isEmpty() || !settings.enabled) {
        return null
    }
    val result = Paths.get(tinyGoSDKPath, "src")

    return if (result.exists()) VfsUtil.findFileByIoFile(result.toFile(), true) else null
}
