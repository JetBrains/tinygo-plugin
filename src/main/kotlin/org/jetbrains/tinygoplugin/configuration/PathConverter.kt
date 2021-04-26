package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

fun absoluteOrRelativePath(path: String, project: Project?): VirtualFile? {
    val pathUrl = VfsUtil.convertToURL(path) ?: return null
    var result = VfsUtil.findFileByURL(pathUrl)
    if (result == null && project != null) {
        val projectPath = project.basePath
        val absolutePath = FileUtil.join(projectPath, path)
        val absoluteUrl = VfsUtil.convertToURL(absolutePath) ?: return null
        result = VfsUtil.findFileByURL(absoluteUrl)
    }
    return result
}

fun toRelativePath(path: String, project: Project): String {
    val projectPath = project.basePath ?: return ""
    if (!path.startsWith(projectPath)) {
        return ""
    }
    return path.substring(projectPath.length)
}
// Default fall through implementation
interface PathConverter {
    fun toRelative(absolutePath: String): String = absolutePath
    fun toAbsolute(relativePath: String): String = relativePath
}

class ProjectPathConverter(private val project: Project) : PathConverter {
    override fun toRelative(absolutePath: String): String = toRelativePath(absolutePath, project)
    override fun toAbsolute(relativePath: String): String = absoluteOrRelativePath(relativePath, project)?.path ?: ""
}
