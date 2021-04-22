package org.jetbrains.tinygoplugin.services

import com.goide.project.GoRootsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.util.Collections

class TinyGoRootsProvider : GoRootsProvider {

    override fun getGoPathRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> = Collections.emptyList()

    override fun getGoPathSourcesRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> =
        Collections.emptyList()

    override fun getGoPathBinRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> = Collections.emptyList()

    override fun isExternal(): Boolean = false
    override fun getVendorDirectoriesInResolveScope(
        project: Project,
        module: Module?,
        file: VirtualFile?,
    ): MutableCollection<VirtualFile>? {
        val projectSettings = TinyGoConfiguration.getInstance(project)
        if (file == null || module == null || !projectSettings.enabled) {
            return null
        }
        val sdk = projectSettings.sdk
        val sdkRoot = sdk.sdkRoot ?: return null
        if (VfsUtil.isAncestor(sdkRoot, file, false)) {
            return mutableListOf(file.parent)
        }
        return null
    }
}