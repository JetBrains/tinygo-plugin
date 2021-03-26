package org.jetbrains.tinygoplugin.services

import com.goide.project.GoRootsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.nullSdk

class TinyGopathManager : GoRootsProvider {

    private fun getTinyGoRoot(project: Project, subfolder: String? = null): VirtualFile? {
        val settings = TinyGoConfiguration.getInstance(project)
        val tinyGoSDKPath = settings.tinyGoSDKPath
        if (tinyGoSDKPath == nullSdk) {
            return null
        }
        val root = tinyGoSDKPath.sdkRoot
        return if (subfolder == null) root else root?.findChild(subfolder)
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
