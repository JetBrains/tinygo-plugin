package org.jetbrains.tinygoplugin.services

import com.goide.project.GoRootsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.nio.file.Paths

class TinyGoGopathManager : GoRootsProvider {

    private fun getTinyGoRoot(project: Project, subfolder: String? = null): VirtualFile? {
        val settings = TinyGoConfiguration.getInstance(project)
        val tinyGoSDKPath = settings.tinyGoSDKPath
        if (tinyGoSDKPath.isEmpty() || !settings.enabled) {
            return null
        }
        val result = if (subfolder == null) Paths.get(tinyGoSDKPath) else Paths.get(tinyGoSDKPath, subfolder)

        return if (result.exists()) VfsUtil.findFileByIoFile(result.toFile(), true) else null
    }

    override fun getGoPathRoots(project: Project?, p1: Module?): MutableCollection<VirtualFile> {
        if (project == null) {
            return mutableSetOf()
        }
        val tinyGoRoot = getTinyGoRoot(project, null)
        return if (tinyGoRoot == null) {
            mutableSetOf()
        } else {
            mutableSetOf(tinyGoRoot)
        }
    }

    override fun getGoPathSourcesRoots(project: Project?, module: Module?): Collection<VirtualFile> {
        if (project == null) {
            return setOf()
        }
        val tinyGoRoot = getTinyGoRoot(project, "src")
        return if (tinyGoRoot == null) {
            setOf()
        } else {
            setOf(tinyGoRoot)
        }
    }

    override fun getGoPathBinRoots(project: Project?, module: Module?): Collection<VirtualFile> {
        return setOf()
    }

    override fun isExternal(): Boolean = true
}
